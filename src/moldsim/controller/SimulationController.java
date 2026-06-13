package moldsim.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import moldsim.model.*;

public class SimulationController {

    private final ArchiveRoom room;
    private final List<MoldSensor> sensors;
    private final AlertController alertController;
    private final Environment environment;
    private final Random random;
    private final Map<Wall, List<Shelf>> shelvesByWall;
    private int currentWeek;

    private static final double BASE_EXTERNAL_SPORE_DEPOSITION = 0.00002; //apparition très rare de spores venant de l’environnement extérieur
    private static final double BASE_INTERNAL_SPORE_DEPOSITION = 0.004; //dépôt de spores dû aux moisissures sporulantes déjà présentes
    private static final double BASE_SPORE_GERMINATION = 0.05; //probabilité qu’une spore déposée germe
    private static final double SPORULATION_THRESHOLD = 10.0; //niveau de moisissure à partir duquel une cellule devient sporulante
    private static final int MIN_ACTIVE_MOLD_AGE_BEFORE_DEATH = 104; //104 semaines (2 ans) : avant ça, une cellule infectée ne meurt normalement pas.
    private static final double BASE_MOLD_DEATH_PROBABILITY = 0.04; //4% : probabilité de mort par semaine après ce seuil.
    private static final double CRITICAL_MOLD_LEVEL = 90.0; //90.0 : si le niveau de moisissure est très haut, la mort devient plus probable.

    public SimulationController(ArchiveRoom room, Map<Wall, List<Shelf>> shelvesByWall, Environment environment) {
        this.room            = room;
        this.environment     = environment;
        this.sensors         = new ArrayList<>();
        this.alertController = new AlertController();
        this.currentWeek     = 0;
        this.random          = new Random();
        this.shelvesByWall = new HashMap<>(shelvesByWall);
        initSensors(shelvesByWall);
        alertController.setRecommendationEngine(new RecommendationEngine(room));
    }

    public SimulationController(Wall wall, Environment environment) {
        this.room = new ArchiveRoom("Archive", environment);
        this.room.setNorthWall(wall);

        this.environment = environment;
        this.sensors = new ArrayList<>();
        this.alertController = new AlertController();
        this.currentWeek = 0;
        this.random = new Random();

        this.shelvesByWall = new HashMap<>();
        this.shelvesByWall.put(wall, new ArrayList<>());

        initSensors(this.shelvesByWall);
        alertController.setRecommendationEngine(new RecommendationEngine(room));
    }

    private void initSensors(Map<Wall, List<Shelf>> shelvesByWall) {
        if (room.getNorthWall() != null) sensors.add(new MoldSensor(room.getNorthWall(),
            shelvesByWall.getOrDefault(room.getNorthWall(), List.of())));
        if (room.getSouthWall() != null) sensors.add(new MoldSensor(room.getSouthWall(),
            shelvesByWall.getOrDefault(room.getSouthWall(), List.of())));
        if (room.getEastWall()  != null) sensors.add(new MoldSensor(room.getEastWall(),
            shelvesByWall.getOrDefault(room.getEastWall(),  List.of())));
        if (room.getWestWall()  != null) sensors.add(new MoldSensor(room.getWestWall(),
            shelvesByWall.getOrDefault(room.getWestWall(),  List.of())));
    }

    // Appelé par GridView via stepSimulation()
    public void step() {
        currentWeek++;

        for (MoldSensor sensor : sensors) {
            Wall wall = sensor.getWall();

            updateDepositedSpores(wall);          // DEPOSITED_SPORE → INFECTED ou HEALTHY
            propagateOnWall(wall);                // INFECTED / SPORULATING → voisins
            matureInfectedCells(wall);            // INFECTED → SPORULATING
            killOldMoldCells(wall);
        }

        pollSensors();
    }

    private void propagateOnWall(Wall wall) {
        List<Cell> cellsToInfect = new ArrayList<>();
        for (Cell[] row : wall.getGrid()) {
            for (Cell cell : row) {
                if (!isActiveMold(cell) || cell.getSpecies() == null) {
                    continue;
                }
                for (Cell neighbor : wall.getNeighbors(cell)) {
                    if (neighbor.getState() == CellState.HEALTHY
                            && neighbor.getWallMaterial() != WallMaterial.WOOD) {
                        double p = computeInfectionProbability(
                            neighbor, cell.getSpecies(), wall.getMaterial());
                        if (random.nextDouble() < p)
                            cellsToInfect.add(neighbor);
                    }
                }
                cell.incrementAge();
                double growth = cell.getSpecies().getMoldGrowthPerStep();
                if (environment.getHumidity() > 80) growth *= 1.5;
                cell.setMoldLevel(cell.getMoldLevel() + growth);
            }
        }
        cellsToInfect.forEach(c -> {
            if (c.getSpecies() == null) c.infect(MoldSpecies.CLADOSPORIUM);
        });
    }

    public double computeInfectionProbability(Cell neighbor, MoldSpecies species, WallMaterial wallMaterial) {
        double humidity = environment.getHumidity();
        double temperature = environment.getTemperature();
        double ventilation = environment.getVentilation();
        if (humidity < species.getMinHumidity()) return 0.0;
        if (temperature < species.getMinTemperature()
         || temperature > species.getMaxTemperature()) return 0.0;
        double fHumidity = (humidity - species.getMinHumidity()) / (100.0 - species.getMinHumidity());
        fHumidity = Math.max(0.0, Math.min(1.0, fHumidity));
        double tempMid = (species.getMinTemperature() + species.getMaxTemperature()) / 2.0;
        double tempRange = tempMid - species.getMinTemperature();
        double fTemperature = Math.max(0.0, 1.0 - Math.abs(temperature - tempMid) / tempRange);
        double fMaterial = getMaterialFactor(neighbor.getWallMaterial(), wallMaterial);
        double fVentilation = 1.0 - (ventilation / 200.0);
        return 0.10 * fHumidity * fTemperature * fMaterial * species.getInfectionProbability() * fVentilation;
    }

    private double getMaterialFactor(WallMaterial cellMaterial, WallMaterial wallMaterial) {
        // La cellule peut surcharger le matériau du mur (planches, documents)
        WallMaterial effective = cellMaterial != null ? cellMaterial : wallMaterial;
        if (effective == null) return 1.0;
        return switch (effective) {
            case PLASTER  -> 1.0;
            case CONCRETE -> 0.5;
            case BRICK    -> 0.7;
            case WOOD     -> 1.2;
            case DOCUMENT -> 1.5;
            default       -> 1.0;
        };
    }

    private void pollSensors() {
        for (MoldSensor sensor : sensors) {
            sensor.poll(currentWeek).forEach(alertController::handle);
        }
    }

    public int getCurrentWeek(){
        return currentWeek;
    }

    public AlertController getAlertController() {
        return alertController;
    }

    public List<SensorEvent> getHistory() {
        return alertController.getHistory();
    }

    public void updateShelves(List<Shelf> shelves) {
        shelvesByWall.put(room.getNorthWall(), shelves);
        sensors.clear();
        initSensors(shelvesByWall);
    }

    public void resetSensors() {
        sensors.clear();
        initSensors(shelvesByWall);
    }

    public void setDisplayName(String displayName) {
        alertController.setContextName(displayName);
    }

    private boolean isActiveMold(Cell cell) {
        return cell.getState() == CellState.INFECTED
            || cell.getState() == CellState.SPORULATING;
    }

    private void matureInfectedCells(Wall wall) {
        for (Cell[] row : wall.getGrid()) {
            for (Cell cell : row) {
                if (cell.getState() == CellState.INFECTED
                        && cell.getMoldLevel() >= SPORULATION_THRESHOLD) {
                    cell.setState(CellState.SPORULATING);
                }
            }
        }
    }

    private int countCellsByState(Wall wall, CellState state) {
        int count = 0;

        for (Cell[] row : wall.getGrid()) {
            for (Cell cell : row) {
                if (cell.getState() == state) {
                    count++;
                }
            }
        }

        return count;
    }

    //Environment factor calculus methods

    private double computeHumiditySuitability(MoldSpecies species) {
        double humidity = environment.getHumidity();

        if (humidity < species.getMinHumidity()) {
            return 0.0;
        }

        double value = (humidity - species.getMinHumidity()) / (100.0 - species.getMinHumidity());
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double computeTemperatureSuitability(MoldSpecies species) {
        double temperature = environment.getTemperature();

        if (temperature < species.getMinTemperature()
                || temperature > species.getMaxTemperature()) {
            return 0.0;
        }

        double tempMid = (species.getMinTemperature() + species.getMaxTemperature()) / 2.0;
        double tempRange = tempMid - species.getMinTemperature();

        if (tempRange <= 0.0) {
            return 0.0;
        }

        double value = 1.0 - Math.abs(temperature - tempMid) / tempRange;
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double computeVentilationBlockingFactor() {
        double ventilation = environment.getVentilation();

        double factor = 1.0 - ventilation / 100.0;

        return Math.max(0.0, Math.min(1.0, factor));
    }

    //Spore methods
    private void depositSporesFromSporulating(Wall wall) {
        MoldSpecies species = MoldSpecies.CLADOSPORIUM;

        int totalCells = wall.getWidth() * wall.getHeight();

        if (totalCells <= 0) {
            return;
        }

        int sporulatingCount = countCellsByState(wall, CellState.SPORULATING);

        double sporulatingRatio = (double) sporulatingCount / totalCells;

        double sporePressure = 1.0 - Math.exp(-8.0 * sporulatingRatio);

        double humidityFactor = computeHumiditySuitability(species);
        double temperatureFactor = computeTemperatureSuitability(species);
        double ventilationFactor = computeVentilationBlockingFactor();

        double environmentalSuitability =
                humidityFactor
                * temperatureFactor
                * ventilationFactor;

        double probability =
                environmentalSuitability
                * (
                    BASE_EXTERNAL_SPORE_DEPOSITION
                    + BASE_INTERNAL_SPORE_DEPOSITION * sporePressure
                );

        for (Cell[] row : wall.getGrid()) {
            for (Cell cell : row) {
                if (cell.getState() == CellState.HEALTHY) {
                    if (random.nextDouble() < probability) {
                        cell.setState(CellState.DEPOSITED_SPORE);
                        cell.setSpecies(species);
                        cell.setMoldLevel(0.0);
                        cell.setAge(0);
                    }
                }
            }
        }
    }

    private void updateDepositedSpores(Wall wall) {
        MoldSpecies species = MoldSpecies.CLADOSPORIUM;

        double humidityFactor = computeHumiditySuitability(species);
        double temperatureFactor = computeTemperatureSuitability(species);
        double ventilationFactor = computeVentilationBlockingFactor();

        for (Cell[] row : wall.getGrid()) {
            for (Cell cell : row) {
                if (cell.getState() != CellState.DEPOSITED_SPORE) {
                    continue;
                }

                double materialFactor = getMaterialFactor(
                    cell.getWallMaterial(),
                    wall.getMaterial()
                );

                double germinationProbability =
                        BASE_SPORE_GERMINATION
                        * humidityFactor
                        * temperatureFactor
                        * ventilationFactor
                        * materialFactor;

                if (random.nextDouble() < germinationProbability) {
                    cell.infect(species);
                    continue;
                }

                double ventilation = environment.getVentilation();

                double removalProbability =
                        0.02
                        + 0.15 * (ventilation / 100.0)
                        + 0.10 * (1.0 - humidityFactor);

                removalProbability = Math.max(0.0, Math.min(1.0, removalProbability));

                if (random.nextDouble() < removalProbability) {
                    cell.setState(CellState.HEALTHY);
                    cell.setSpecies(null);
                    cell.setMoldLevel(0.0);
                    cell.setAge(0);
                }
            }
        }
    }

    private void killOldMoldCells(Wall wall) {
        for (Cell[] row : wall.getGrid()) {
            for (Cell cell : row) {
                if (!isActiveMold(cell)) {
                    continue;
                }

                if (cell.getAge() < MIN_ACTIVE_MOLD_AGE_BEFORE_DEATH
                        && cell.getMoldLevel() < CRITICAL_MOLD_LEVEL) {
                    continue;
                }

                double ageFactor = 0.0;

                if (cell.getAge() >= MIN_ACTIVE_MOLD_AGE_BEFORE_DEATH) {
                    ageFactor = (cell.getAge() - MIN_ACTIVE_MOLD_AGE_BEFORE_DEATH) / 20.0;
                }

                double moldLevelFactor = 0.0;

                if (cell.getMoldLevel() >= CRITICAL_MOLD_LEVEL) {
                    moldLevelFactor = (cell.getMoldLevel() - CRITICAL_MOLD_LEVEL)
                            / (100.0 - CRITICAL_MOLD_LEVEL);
                }

                double dryEnvironmentFactor = 0.0;

                if (environment.getHumidity() < 50.0) {
                    dryEnvironmentFactor = 0.08;
                }

                double deathProbability =
                        BASE_MOLD_DEATH_PROBABILITY
                        + 0.08 * ageFactor
                        + 0.10 * moldLevelFactor
                        + dryEnvironmentFactor;

                deathProbability = Math.max(0.0, Math.min(1.0, deathProbability));

                if (random.nextDouble() < deathProbability) {
                    cell.kill();
                }
            }
        }
    }

}