package moldsim.controller;

import moldsim.model.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SimulationController {

    private final ArchiveRoom room;
    private final List<MoldSensor> sensors;
    private final AlertController alertController;
    private final Environment environment;
    private final Random random;
    private final Map<Wall, List<Shelf>> shelvesByWall;
    private int currentWeek;

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
            propagateOnWall(sensor.getWall());
        }
        pollSensors();
    }

    private void propagateOnWall(Wall wall) {
        List<Cell> cellsToInfect = new ArrayList<>();
        for (Cell[] row : wall.getGrid()) {
            for (Cell cell : row) {
                if (!cell.isInfected() || cell.getSpecies() == null) continue;
                for (Cell neighbor : wall.getNeighbors(cell)) {
                    if (neighbor.getState() == CellState.HEALTHY
                            && neighbor.getWallMaterial() != WallMaterial.WOOD) {
                        double p = computeInfectionProbability(
                            neighbor, cell.getSpecies(), wall.getMaterial());
                        if (random.nextDouble() < p)
                            cellsToInfect.add(neighbor);
                    }
                }
                double growth = cell.getSpecies().getMoldGrowthPerStep();
                if (environment.getHumidity() > 80) growth *= 1.5;
                cell.setMoldLevel(cell.getMoldLevel() + growth);
            }
        }
        cellsToInfect.forEach(c -> {
            if (c.getSpecies() == null) c.infect(MoldSpecies.CLADOSPORIUM);
        });
    }

    private double computeInfectionProbability(Cell neighbor, MoldSpecies species, WallMaterial wallMaterial) {
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
    

    public void setCurrentWeek(int week) {
        this.currentWeek = week;
    }
}