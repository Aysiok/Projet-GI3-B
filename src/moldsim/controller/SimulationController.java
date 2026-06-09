package moldsim.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import moldsim.model.Cell;
import moldsim.model.CellState;
import moldsim.model.Environment;
import moldsim.model.Grid;
import moldsim.model.MoldSpecies;

public class SimulationController {
    private final Grid grid;
    private final Environment environment;
    private final Random random;

    public SimulationController(Grid grid, Environment environment) {
        this.grid = grid;
        this.environment = environment;
        this.random = new Random();
    }

    public void step() {
        List<Cell> cellsToInfect = new ArrayList<>();

        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                Cell current = grid.getCell(x, y);
                if (current.isInfected() && current.getSpecies() != null) {
                    List<Cell> neighbors = grid.getNeighbors(x, y);
                    for (Cell neighbor : neighbors) {
                        if (neighbor.getState() == CellState.HEALTHY
                                && neighbor.getWallMaterial() != moldsim.model.WallMaterial.WOOD) {
                            double probability = computeInfectionProbability(neighbor, current.getSpecies());
                            if (random.nextDouble() < probability) {
                                cellsToInfect.add(neighbor);
                            }
                        }
                    }
                    double growth = current.getSpecies().getMoldGrowthPerStep();
                    if (environment.getHumidity() > 80) growth *= 1.5;
                    current.setMoldLevel(current.getMoldLevel() + growth);
                }
            }
        }

        for (Cell c : cellsToInfect) {
            if (c.getSpecies() == null) c.infect(MoldSpecies.CLADOSPORIUM);
        }
    }

    private double computeInfectionProbability(Cell neighbor, MoldSpecies species) {
        double humidity    = environment.getHumidity();
        double temperature = environment.getTemperature();
        double ventilation = environment.getVentilation();

        if (humidity < species.getMinHumidity()) return 0.0;
        if (temperature < species.getMinTemperature() || temperature > species.getMaxTemperature()) return 0.0;

        double fHumidity = (humidity - species.getMinHumidity())
                           / (100.0 - species.getMinHumidity());
        fHumidity = Math.max(0.0, Math.min(1.0, fHumidity));

        double tempMid      = (species.getMinTemperature() + species.getMaxTemperature()) / 2.0;
        double tempRange    = tempMid - species.getMinTemperature();
        double fTemperature = Math.max(0.0, 1.0 - Math.abs(temperature - tempMid) / tempRange);

        double fMaterial    = getMaterialFactor(neighbor);
        double fSpecies     = species.getInfectionProbability();
        double fVentilation = 1.0 - (ventilation / 200.0);

        return 0.10 * fHumidity * fTemperature * fMaterial * fSpecies * fVentilation;
    }

    private double getMaterialFactor(Cell cell) {
        if (cell.getWallMaterial() == null) return 1.0;

        switch (cell.getWallMaterial()) {
            case PLASTER:   return 1.0;
            case CONCRETE:  return 0.5;
            case BRICK:     return 0.7;
            case WOOD:      return 1.2;
            case DOCUMENT: return 1.5;
            default:        return 1.0;
        }
    }
}