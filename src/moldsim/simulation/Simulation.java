package moldsim.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import moldsim.model.Cell;
import moldsim.model.CellState;
import moldsim.model.Environment;
import moldsim.model.Grid;
import moldsim.model.MoldSpecies;

public class Simulation {
    private final Grid grid;
    private final Environment environment;
    private final Random random;

    public Simulation(Grid grid, Environment environment) {
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
                        if (neighbor.getState() == CellState.HEALTHY) {
                            if (random.nextDouble() < current.getSpecies().getInfectionProbability()) {
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
}
