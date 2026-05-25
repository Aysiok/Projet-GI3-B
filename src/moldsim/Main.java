package moldsim;

import moldsim.model.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Démarrage de la simulation de moisissure ===");

        // 1. Création de l'environnement
        Environment env = new Environment();
        env.setHumidity(80.0); // Humidité élevée pour favoriser la croissance
        System.out.println("Environnement créé : " + env);

        // 2. Création de la grille (10x10)
        Grid grid = new Grid(10, 10, false, NeighborhoodMode.FOUR);
        System.out.println("Grille créée : " + grid.getWidth() + "x" + grid.getHeight());

        // 3. Infecter une cellule au centre (position 5,5)
        Cell centerCell = grid.getCell(5, 5);
        centerCell.infect(MoldSpecies.CLADOSPORIUM);
        System.out.println("Cellule centrale infectée : " + centerCell);

        // 4. Test de propagation manuelle (Simulation d'un tour)
        // On regarde les voisins de la cellule infectée
        var neighbors = grid.getNeighbors(centerCell);
        System.out.println("Tentative de propagation aux voisins...");

        for (Cell neighbor : neighbors) {
            // Pour le test, on infecte automatiquement les voisins sains
            if (neighbor.getState() == CellState.HEALTHY) {
                neighbor.infect(MoldSpecies.CLADOSPORIUM);
                System.out.println("  -> Voisin infecté en " + neighbor.getX() + "," + neighbor.getY());
            }
        }

        // 5. Affichage visuel simplifié (Console)
        System.out.println("\nRésultat visuel (X = Infecté, . = Sain) :");
        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                Cell c = grid.getCell(x, y);
                if (c.isInfected()) {
                    System.out.print(" X ");
                } else {
                    System.out.print(" . ");
                }
            }
            System.out.println(); // Retour à la ligne
        }
        
        System.out.println("\n=== Test terminé avec succès ===");
    }
}