package moldsim.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import moldsim.model.*;

/**
 * Controller responsible for executing the mold simulation.
 * <p>
 * This class manages mold growth, spore deposition, contamination
 * propagation, sensor polling, event handling, and environmental effects.
 */
public class SimulationController {

    private final ArchiveRoom room;
    private final List<MoldSensor> sensors;
    private final AlertController alertController;
    private final Environment environment;
    private final Random random;
    private final Map<Wall, List<Shelf>> shelvesByWall;
    private final EventManager eventManager;
    private int currentWeek;
    private String displayName;

    private static final double ROOM_EXTERNAL_SPORE_DEPOSITION = 0.00002;
    private static final double ROOM_INTERNAL_SPORE_DEPOSITION = 0.004;
    private static final double BASE_SPORE_GERMINATION = 0.05; //probabilité qu’une spore déposée germe
    private static final double SPORULATION_THRESHOLD = 10.0; //niveau de moisissure à partir duquel une cellule devient sporulante
    private static final int MIN_ACTIVE_MOLD_AGE_BEFORE_DEATH = 104; // 104 semaines : seuil de mort naturelle liée à l'âge.
    private static final double BASE_MOLD_DEATH_PROBABILITY = 0.04; //4% : probabilité de mort par semaine après ce seuil.
    private static final double CRITICAL_MOLD_LEVEL = 90.0; //90.0 : si le niveau de moisissure est très haut, la mort devient plus probable.

    /**
     * Creates a simulation controller for an archive room.
     *
     * @param room archive room being simulated
     * @param shelvesByWall shelves grouped by wall
     * @param environment simulation environment
     */
    public SimulationController(ArchiveRoom room, Map<Wall, List<Shelf>> shelvesByWall, Environment environment) {
        this.room = room;
        this.environment = environment;
        this.sensors = new ArrayList<>();
        this.alertController = new AlertController();
        this.currentWeek = 0;
        this.random = new Random();
        this.eventManager = new EventManager(environment);
        this.shelvesByWall = new HashMap<>(shelvesByWall);
        initSensors(shelvesByWall);
        this.displayName = "Unknown";
        alertController.setRecommendationEngine(new RecommendationEngine(displayName));
    }

    /**
     * Creates a simulation controller for a single wall.
     *
     * @param wall wall to simulate
     * @param environment simulation environment
     */
    public SimulationController(Wall wall, Environment environment) {
        this.room = new ArchiveRoom("Archive", environment);
        this.room.setNorthWall(wall);
        this.eventManager = new EventManager(environment);
        this.environment = environment;
        this.sensors = new ArrayList<>();
        this.alertController = new AlertController();
        this.currentWeek = 0;
        this.random = new Random();

        this.shelvesByWall = new HashMap<>();
        this.shelvesByWall.put(wall, new ArrayList<>());

        initSensors(this.shelvesByWall);
        this.displayName = "Unknown";
        alertController.setRecommendationEngine(new RecommendationEngine(displayName));
    }

    /**
     * Creates and initializes mold sensors for all available walls.
     *
     * @param shelvesByWall shelves associated with each wall
     */
    private void initSensors(Map<Wall, List<Shelf>> shelvesByWall) {
        if (room.getNorthWall() != null) {
            sensors.add(new MoldSensor(room.getNorthWall(), shelvesByWall.getOrDefault(room.getNorthWall(), List.of())));
        }
        if (room.getSouthWall() != null) {
            sensors.add(new MoldSensor(room.getSouthWall(), shelvesByWall.getOrDefault(room.getSouthWall(), List.of())));
        }
        if (room.getEastWall()  != null) {
            sensors.add(new MoldSensor(room.getEastWall(), shelvesByWall.getOrDefault(room.getEastWall(),  List.of())));
        }
        if (room.getWestWall()  != null) {
            sensors.add(new MoldSensor(room.getWestWall(), shelvesByWall.getOrDefault(room.getWestWall(),  List.of())));
        }
    }

    // Appelé par GridView via stepSimulation()
    /**
     * Advances the simulation by one week.
     */
    public void step() {
        currentWeek++;

        for (MoldSensor sensor : sensors) {
            Wall wall = sensor.getWall();
            updateDepositedSpores(wall);  
            propagateOnWall(wall);       
            matureInfectedCells(wall);         
            killOldMoldCells(wall);
        }
        pollSensors();
    }

    /**
     * Propagates mold contamination across a wall.
     *
     * @param wall wall to update
     */
    private void propagateOnWall(Wall wall) {
        List<Cell> cellsToInfect = new ArrayList<>();
        for (Cell[] row : wall.getGrid()) {
            for (Cell cell : row) {
                if (!isActiveMold(cell) || cell.getSpecies() == null) {
                    continue;
                }
                for (Cell neighbor : wall.getNeighbors(cell)) {
                    if (neighbor.getState() == CellState.HEALTHY && neighbor.getWallMaterial() != WallMaterial.WOOD) {
                        double p = computeInfectionProbability(neighbor, cell.getSpecies(), wall.getMaterial());
                        if (random.nextDouble() < p){
                            cellsToInfect.add(neighbor);
                        }
                    }
                }
                cell.incrementAge();
                double growth = cell.getSpecies().getMoldGrowthPerStep();
                if (environment.getHumidity() > 80){ 
                    growth *= 1.5;
                }
                cell.setMoldLevel(cell.getMoldLevel() + growth);
            }
        }
        cellsToInfect.forEach(c -> {
            if (c.getSpecies() == null){
                c.infect(MoldSpecies.CLADOSPORIUM);
            }
        });
    }

    /**
     * Computes the probability that a cell becomes infected.
     *
     * @param neighbor target cell
     * @param species mold species
     * @param wallMaterial wall material
     * @return infection probability
     */
    public double computeInfectionProbability(Cell neighbor, MoldSpecies species, WallMaterial wallMaterial) {
        double humidity = environment.getHumidity();
        double temperature = environment.getTemperature();
        double ventilation = environment.getVentilation();
        if (humidity < species.getMinHumidity()) return 0.0;
        if (temperature < species.getMinTemperature() || temperature > species.getMaxTemperature()) return 0.0;
        double fHumidity = (humidity - species.getMinHumidity()) / (100.0 - species.getMinHumidity());
        fHumidity = Math.max(0.0, Math.min(1.0, fHumidity));
        double tempMid = (species.getMinTemperature() + species.getMaxTemperature()) / 2.0;
        double tempRange = tempMid - species.getMinTemperature();
        double fTemperature = Math.max(0.0, 1.0 - Math.abs(temperature - tempMid) / tempRange);
        double fMaterial = getMaterialFactor(neighbor.getWallMaterial(), wallMaterial);
        double fVentilation = 1.0 - (ventilation / 200.0);
        return 0.10 * fHumidity * fTemperature * fMaterial * species.getInfectionProbability() * fVentilation;
    }

    /**
     * Returns the growth factor associated with a material.
     *
     * @param cellMaterial material assigned to the cell
     * @param wallMaterial default wall material
     * @return material factor
     */
    private double getMaterialFactor(WallMaterial cellMaterial, WallMaterial wallMaterial) {
        // La cellule peut surcharger le matériau du mur (planches, documents)
        WallMaterial effective = cellMaterial != null ? cellMaterial : wallMaterial;
        if (effective == null) {
            return 1.0;
        }
        return switch (effective) {
            case PLASTER -> 1.0;
            case CONCRETE -> 0.5;
            case BRICK -> 0.7;
            case WOOD -> 1.2;
            case DOCUMENT -> 1.5;
            default -> 1.0;
        };
    }

    /**
     * Polls all sensors and processes generated alerts.
     */
    private void pollSensors() {
        for (MoldSensor sensor : sensors) {
            sensor.poll(currentWeek).forEach(alertController::handle);
        }
    }

    /**
     * Gets the current simulation week.
     *
     * @return the current week number
     */
    public int getCurrentWeek(){
        return currentWeek;
    }

    /**
     * Gets the alert controller for the simulation.
     *
     * @return the alert controller
     */
    public AlertController getAlertController() {
        return alertController;
    }

    /**
     * Gets the history of sensor events.
     *
     * @return list of sensor events
     */
    public List<SensorEvent> getHistory() {
        return alertController.getHistory();
    }

    /**
     * Gets the event manager for the simulation.
     *
     * @return the event manager
     */
    public EventManager getEventManager() {
        return eventManager;
    }

    /**
     * Updates shelf assignments and recreates sensors.
     *
     * @param shelves updated shelf list
     */
    public void updateShelves(List<Shelf> shelves) {
        shelvesByWall.put(room.getNorthWall(), shelves);
        sensors.clear();
        initSensors(shelvesByWall);
    }

    /**
     * Resets all simulation sensors.
     */
    public void resetSensors() {
        for (MoldSensor sensor : sensors) {
            sensor.reset();
        }
}
    /**
     * Sets the display name for the simulation context.
     *
     * @param displayName the display name to set
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        alertController.setContextName(displayName);
        alertController.getRecommendationEngine().setWallName(displayName);
    }

    /**
     * Determines whether a cell contains active mold.
     *
     * @param cell cell to evaluate
     * @return true if the cell contains active mold
     */
    private boolean isActiveMold(Cell cell) {
        return cell.getState() == CellState.INFECTED || cell.getState() == CellState.SPORULATING;
    }

    /**
     * Converts mature infected cells into sporulating cells.
     *
     * @param wall wall to update
     */
    private void matureInfectedCells(Wall wall) {
        for (Cell[] row : wall.getGrid()) {
            for (Cell cell : row) {
                if (cell.getState() == CellState.INFECTED && cell.getMoldLevel() >= SPORULATION_THRESHOLD) {
                    cell.setState(CellState.SPORULATING);
                }
            }
        }
    }

    //Environment factor calculus methods

    /**
     * Computes the suitability of the current humidity for given mold species.
     *
     * @param species mold species
     * @return humidity suitability in [0,1]
     */
    private double computeHumiditySuitability(MoldSpecies species) {
        double humidity = environment.getHumidity();
        if (humidity < species.getMinHumidity()) {
            return 0.0;
        }
        double value = (humidity - species.getMinHumidity()) / (100.0 - species.getMinHumidity());
        return Math.max(0.0, Math.min(1.0, value));
    }

    /**
     * Computes the suitability of the current temperature for given mold species.
     *
     * @param species mold species
     * @return temperature suitability in [0,1]
     */
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

    /**
     * Computes the environmental blocking factor due to ventilation.
     *
     * @return ventilation blocking factor in [0,1]
     */
    private double computeVentilationBlockingFactor() {
        double ventilation = environment.getVentilation();
        double factor = 1.0 - ventilation / 100.0;
        return Math.max(0.0, Math.min(1.0, factor));
    }

    /**
     * Updates deposited spores by applying germination and removal rules.
     *
     * @param wall wall to update
     */
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
                double materialFactor = getMaterialFactor(cell.getWallMaterial(), wall.getMaterial());
                double germinationProbability = BASE_SPORE_GERMINATION * humidityFactor * temperatureFactor * ventilationFactor * materialFactor;

                if (random.nextDouble() < germinationProbability) {
                    cell.infect(species);
                    continue;
                }
                double ventilation = environment.getVentilation();
                double removalProbability = 0.02 + 0.15 * (ventilation / 100.0) + 0.10 * (1.0 - humidityFactor);
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

    /**
     * Deposits spores across all walls in the simulation.
     *
     * @param walls wall contexts participating in the simulation
     */
    public void depositSporesAcrossRoom(List<WallContext> walls) {
        MoldSpecies species = MoldSpecies.CLADOSPORIUM;
        int totalCells = 0;
        int sporulatingCount = 0;

        for (WallContext wallContext : walls) {
            Wall wall = wallContext.getWall();
            totalCells += wall.getWidth() * wall.getHeight();
            sporulatingCount += countCellsByState(wall, CellState.SPORULATING);
        }
        if (totalCells <= 0) return;
        double sporulatingRatio = (double) sporulatingCount / totalCells;
        double sporePressure = 1.0 - Math.exp(-8.0 * sporulatingRatio);
        double environmentalSuitability = computeHumiditySuitability(species) * computeTemperatureSuitability(species) * computeVentilationBlockingFactor();
        double probability = environmentalSuitability * (ROOM_EXTERNAL_SPORE_DEPOSITION + ROOM_INTERNAL_SPORE_DEPOSITION * sporePressure);

        for (WallContext wallContext : walls) {
            depositSporesOnWall(wallContext.getWall(), species, probability);
        }
    }

    /**
     * Deposits spores on healthy cells of a wall.
     *
     * @param wall target wall
     * @param species mold species to deposit
     * @param probability deposition probability
     */
    private void depositSporesOnWall(Wall wall, MoldSpecies species, double probability) {
        for (Cell[] row : wall.getGrid()) {
            for (Cell cell : row) {
                if (cell.getState() == CellState.HEALTHY) {
                    if (Math.random() < probability) {
                        cell.setState(CellState.DEPOSITED_SPORE);
                        cell.setSpecies(species);
                        cell.setMoldLevel(0.0);
                        cell.setAge(0);
                    }
                }
            }
        }
    }

    /**
     * Counts cells matching a given state.
     *
     * @param wall wall to inspect
     * @param state state to count
     * @return number of matching cells
     */
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

    /**
     * Removes old mold colonies according to age and environment conditions.
     *
     * @param wall wall to update
     */
    private void killOldMoldCells(Wall wall) {
        for (Cell[] row : wall.getGrid()) {
            for (Cell cell : row) {
                if (!isActiveMold(cell)) {
                    continue;
                }
                if (cell.getAge() < MIN_ACTIVE_MOLD_AGE_BEFORE_DEATH) {
                    continue;
                }
                double ageFactor = (cell.getAge() - MIN_ACTIVE_MOLD_AGE_BEFORE_DEATH) / 20.0;
                double moldLevelFactor = 0.0;
                if (cell.getMoldLevel() >= CRITICAL_MOLD_LEVEL) {
                    moldLevelFactor = (cell.getMoldLevel() - CRITICAL_MOLD_LEVEL)
                            / (100.0 - CRITICAL_MOLD_LEVEL);
                }
                double dryEnvironmentFactor = 0.0;
                if (environment.getHumidity() < 50.0) {
                    dryEnvironmentFactor = 0.08;
                }
                double deathProbability = BASE_MOLD_DEATH_PROBABILITY + 0.08 * ageFactor + 0.10 * moldLevelFactor + dryEnvironmentFactor;
                deathProbability = Math.max(0.0, Math.min(1.0, deathProbability));
                if (random.nextDouble() < deathProbability) {
                    cell.kill();
                }
            }
        }
    }

    /**
     * Sets the current simulation week.
     *
     * @param week the week number to set
     */
    public void setCurrentWeek(int week) {
        this.currentWeek = week;
    }

}