package moldsim.model;

import java.util.ArrayList;
import java.util.List;

public class MoldSensor {

    private static final double THRESHOLD_MEDIUM   = 0.05;
    private static final double THRESHOLD_HIGH     = 0.15;
    private static final double THRESHOLD_CRITICAL = 0.30;

    private final Wall wall;
    private final List<Shelf> shelves;
    private AlertLevel lastAlertLevel;

    public MoldSensor(Wall wall, List<Shelf> shelves) {
        if (wall == null) throw new IllegalArgumentException("Wall cannot be null");
        this.wall           = wall;
        this.shelves        = shelves != null ? shelves : List.of();
        this.lastAlertLevel = AlertLevel.LOW;
    }

    public MoldSensor(Wall wall) {
        this(wall, List.of());
    }

    /** Taux de cellules infectées sur le mur (entre 0.0 et 1.0) */
    public double measure() {
        int total = 0, infected = 0;
        for (Cell[] row : wall.getCells()) {
            for (Cell cell : row) {
                total++;
                if (cell.isInfected()) infected++;
            }
        }
        return total == 0 ? 0.0 : (double) infected / total;
    }

    /** Niveau d'alerte correspondant au taux actuel */
    public AlertLevel getAlertLevel() {
        double rate = measure();
        if (rate >= THRESHOLD_CRITICAL) return AlertLevel.CRITICAL;
        if (rate >= THRESHOLD_HIGH)     return AlertLevel.HIGH;
        if (rate >= THRESHOLD_MEDIUM)   return AlertLevel.MEDIUM;
        return AlertLevel.LOW;
    }

    /**
     * Génère les SensorEvents du tick courant :
     * - un event global si le niveau a augmenté
     * - un event étagère si une shelf sensible est touchée ou menacée
     */
    public List<SensorEvent> poll(int currentWeek) {
        List<SensorEvent> events = new ArrayList<>();

        AlertLevel level = getAlertLevel();
        if (level.ordinal() > lastAlertLevel.ordinal()) {
            lastAlertLevel = level;
            events.add(new SensorEvent(currentWeek, level, measure(), wall));
        }

        SensorEvent shelfEvent = checkShelves(currentWeek);
        if (shelfEvent != null) events.add(shelfEvent);

        return events;
    }

    /** Vérifie si une shelf sensible est infectée ou menacée */
    private SensorEvent checkShelves(int currentWeek) {
        for (Cell[] row : wall.getCells()) {
            for (Cell cell : row) {
                if (!cell.isInfected()) continue;

                // cellule infectée directement sur une shelf sensible
                Shelf shelf = getShelfAt(cell.getX(), cell.getY());
                if (shelf != null && isSensitive(shelf)) {
                    return new SensorEvent(currentWeek, AlertLevel.CRITICAL,
                                           measure(), wall);
                }

                // cellule infectée voisine d'une shelf sensible saine
                for (Cell neighbor : wall.getNeighbors(cell)) {
                    if (neighbor.isInfected()) continue;
                    Shelf neighborShelf = getShelfAt(neighbor.getX(), neighbor.getY());
                    if (neighborShelf != null && isSensitive(neighborShelf)) {
                        return new SensorEvent(currentWeek, AlertLevel.HIGH,
                                               measure(), wall);
                    }
                }
            }
        }
        return null;
    }

    private boolean isSensitive(Shelf shelf) {
        return shelf.getValue() == ShelfValue.HIGH
            || shelf.getValue() == ShelfValue.CRITICAL;
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

    public Wall getWall(){
        return wall;
    }

    public AlertLevel getLastAlertLevel() {
        return lastAlertLevel;
    }
}