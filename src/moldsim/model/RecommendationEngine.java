package moldsim.model;

import java.util.ArrayList;
import java.util.List;

public class RecommendationEngine {

    private final Wall wall;
    private final List<Shelf> shelves;

    public RecommendationEngine(Wall wall, List<Shelf> shelves) {
        this.wall    = wall;
        this.shelves = shelves;
    }

    public List<String> analyze() {
        List<String> recommendations = new ArrayList<>();

        for (Cell[] row : wall.getCells()) {
            for (Cell cell : row) {
                if (!cell.isInfected()) continue;

                Shelf shelf = getShelfAt(cell.getX(), cell.getY());

                // cellule infectée directement sur une étagère
                if (shelf != null) {
                    if (shelf.getValue() == ShelfValue.CRITICAL
                     || shelf.getValue() == ShelfValue.HIGH) {
                        recommendations.add("URGENT : étagère " + shelf.getId()
                            + " infectée [valeur : " + shelf.getValue()
                            + "] — évacuation immédiate");
                    } else {
                        recommendations.add("ATTENTION : étagère " + shelf.getId()
                            + " infectée [valeur : " + shelf.getValue()
                            + "] — traitement recommandé");
                    }
                }

                // cellule infectée voisine d'une étagère saine
                for (Cell neighbor : wall.getNeighbors(cell)) {
                    if (neighbor.isInfected()) continue;
                    Shelf neighborShelf = getShelfAt(neighbor.getX(), neighbor.getY());
                    if (neighborShelf != null
                     && (neighborShelf.getValue() == ShelfValue.CRITICAL
                      || neighborShelf.getValue() == ShelfValue.HIGH)) {
                        recommendations.add("PRÉVENTIF : étagère " + neighborShelf.getId()
                            + " menacée [valeur : " + neighborShelf.getValue()
                            + "] — zone voisine infectée");
                    }
                }
            }
        }

        // recommandation globale selon le taux
        double rate = getMoldRate();
        if (rate >= 0.30) {
            recommendations.add("CRITIQUE : " + String.format("%.0f%%", rate * 100)
                + " du mur infecté — augmenter ventilation et déshumidifier");
        } else if (rate >= 0.15) {
            recommendations.add("ÉLEVÉ : " + String.format("%.0f%%", rate * 100)
                + " du mur infecté — surveiller et traiter les zones touchées");
        }

        return recommendations;
    }

    private Shelf getShelfAt(int x, int y) {
        for (Shelf shelf : shelves) {
            if (x >= shelf.getX() && x < shelf.getX() + shelf.getWidth()
             && y >= shelf.getY() && y < shelf.getY() + shelf.getHeight()) {
                return shelf;
            }
        }
        return null;
    }

    private double getMoldRate() {
        int total = 0, infected = 0;
        for (Cell[] row : wall.getCells()) {
            for (Cell cell : row) {
                total++;
                if (cell.isInfected()) infected++;
            }
        }
        return total == 0 ? 0.0 : (double) infected / total;
    }
}