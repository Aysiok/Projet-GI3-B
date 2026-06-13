package moldsim.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoldSensor {

    private static final double THRESHOLD_MEDIUM   = 0.05;
    private static final double THRESHOLD_HIGH     = 0.15;
    private static final double THRESHOLD_CRITICAL = 0.30;

    private final Wall wall;
    private final List<Shelf> shelves;
    private AlertLevel lastAlertLevel;
    private final Map<String, AlertLevel> shelfAlertLevels;

    public MoldSensor(Wall wall, List<Shelf> shelves) {
        if (wall == null) throw new IllegalArgumentException("Wall cannot be null");
        this.wall = wall;
        this.shelves = shelves != null ? shelves : List.of();
        this.lastAlertLevel = AlertLevel.LOW;
        this.shelfAlertLevels  = new HashMap<>();
    }

    public MoldSensor(Wall wall) {
        this(wall, List.of());
    }

    /** Taux de cellules infectées sur le mur (entre 0.0 et 1.0) */
    public double measure() {
        int total = 0, infected = 0;
        for (Cell[] row : wall.getGrid()) {
            for (Cell cell : row) {
                total++;
                if (cell.isInfected()) infected++;
            }
        }
        return total == 0 ? 0.0 : (double) infected / total;
    }

    /** Niveau d'alerte correspondant au taux actuel */
    public AlertLevel toAlertLevel(double rate) {
        if (rate >= THRESHOLD_CRITICAL) return AlertLevel.CRITICAL;
        if (rate >= THRESHOLD_HIGH)     return AlertLevel.HIGH;
        if (rate >= THRESHOLD_MEDIUM)   return AlertLevel.MEDIUM;
        return AlertLevel.LOW;
    }

    /**
     * Génère les SensorEvents du tick courant :
     * - un event GLOBAL si le niveau a augmenté
     * - un event SHELF si une shelf sensible est touchée ou menacée
     */
    public List<SensorEvent> poll(int currentWeek) {
        List<SensorEvent> events = new ArrayList<>();
        double rate = measure();
        AlertLevel level = toAlertLevel(rate);
        if (level.ordinal() > lastAlertLevel.ordinal()) {
            lastAlertLevel = level;
            events.add(new SensorEvent(currentWeek, level, rate, wall, EventType.GLOBAL, null));
        }
        events.addAll(checkShelves(currentWeek, rate));
        return events;
    }

    /** Vérifie si une shelf sensible est infectée ou menacée */
    private List<SensorEvent> checkShelves(int currentWeek, double rate) {
        List<SensorEvent> events = new ArrayList<>();
        for (Cell[] row : wall.getGrid()) {
            for (Cell cell : row) {
                if (!cell.isInfected()) continue;
                Shelf shelf = getShelfAt(cell.getX(), cell.getY());
                if (shelf != null && isSensitive(shelf)) {
                    emitIfEscalated(shelf, AlertLevel.CRITICAL, currentWeek, events, rate);
                }

                for (Cell neighbor : wall.getNeighbors(cell)) {
                    if (neighbor.isInfected()) continue;
                    Shelf neighborShelf = getShelfAt(neighbor.getX(), neighbor.getY());
                    if (neighborShelf != null && isSensitive(neighborShelf)) {
                        emitIfEscalated(neighborShelf, AlertLevel.HIGH, currentWeek, events, rate);
                    }
                }
            }
        }
        return events;
    }

    private void emitIfEscalated(Shelf shelf, AlertLevel level, int currentWeek, List<SensorEvent> events, double rate) {
        AlertLevel last = shelfAlertLevels.getOrDefault(shelf.getId(), AlertLevel.LOW);
        if (level.ordinal() > last.ordinal()) {
            shelfAlertLevels.put(shelf.getId(), level);
            events.add(new SensorEvent(currentWeek, level, rate, wall, EventType.SHELF, shelf));
        }
    }

    private boolean isSensitive(Shelf shelf) {
        return shelf.getValue() == ShelfValue.HIGH
            || shelf.getValue() == ShelfValue.CRITICAL;
    }

    public void reset() {
        this.lastAlertLevel = AlertLevel.LOW;
        this.shelfAlertLevels.clear();
    }

    private Shelf getShelfAt(int x, int y) {
        for (Shelf shelf : shelves) {
            if (x >= shelf.getX() && x < shelf.getX() + shelf.getWidth() && y >= shelf.getY() && y < shelf.getY() + shelf.getHeight()) {
                return shelf;
            }
        }
        return null;
    }

    public Wall getWall() {
        return wall;
    }

    public AlertLevel getLastAlertLevel() {
        return lastAlertLevel;
    }

}
