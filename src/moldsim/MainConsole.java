package moldsim;

import moldsim.model.*;

public class MainConsole {
    public static void main(String[] args) {
        System.out.println("=== Test Simulation Console ===");

        Environment env = new Environment();
        env.setHumidity(85.0);
        
        Grid grid = new Grid(10, 10, false, NeighborhoodMode.FOUR);
        grid.getCell(5, 5).infect(MoldSpecies.CLADOSPORIUM);
        
        Simulation sim = new Simulation(grid, env);

        System.out.println("Etat initial :");
        printGrid(grid);

        for (int i = 0; i < 3; i++) {
            sim.step();
            System.out.println("Tour " + (i+1) + " :");
            printGrid(grid);
        }
    }

    private static void printGrid(Grid g) {
        for (int y = 0; y < g.getHeight(); y++) {
            for (int x = 0; x < g.getWidth(); x++) {
                Cell c = g.getCell(x, y);
                System.out.print(c.isInfected() ? " X " : " . ");
            }
            System.out.println();
        }
        System.out.println();
    }
}
