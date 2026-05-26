package moldsim;
import moldsim.model.*;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Test Simulation Console ===");
        Environment env = new Environment();
        env.setHumidity(85.0);
        
        Grid grid = new Grid(10, 10, false, NeighborhoodMode.FOUR);
        
        Scanner scanner = new Scanner(System.in);
        System.out.print("Entrez la coordonnée X du foyer (0-9) : ");
        int x = scanner.nextInt();
        System.out.print("Entrez la coordonnée Y du foyer (0-9) : ");
        int y = scanner.nextInt();

        if (grid.inBounds(x, y)) {
            grid.getCell(x, y).infect(MoldSpecies.CLADOSPORIUM);
            System.out.println("Foyer placé en (" + x + "," + y + ")");
        } else {
            System.out.println("Coordonnées invalides, foyer placé au centre par défaut.");
            grid.getCell(5, 5).infect(MoldSpecies.CLADOSPORIUM);
        }
        scanner.close();
        
        Simulation sim = new Simulation(grid, env);
        System.out.println("Etat initial :");
        printGrid(grid);
        int previousInfected = 0;
        for (int i = 0; i < 10; i++) {
            sim.step();
            System.out.println("Tour " + (i + 1) + " :");
            printGrid(grid);
            Statistics stats = new Statistics(grid, previousInfected);
            System.out.println("Tour " + (i + 1) + " :");
            System.out.println(stats);
            previousInfected = stats.getInfectedCells();
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