package moldsim.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sensor responsible for detecting mold levels and generating alert events
 * for walls and shelves in the simulation.
 * <p>
 * It evaluates infection rates, computes alert levels, and emits events
 * when thresholds are crossed or when sensitive shelves are affected.
 */
public class MoldSensor {

    /** Medium alert threshold (infection rate). */
    private static final double THRESHOLD_MEDIUM   = 0.05;
    /** High alert threshold (infection rate). */
    private static final double THRESHOLD_HIGH     = 0.15;
    /** Critical alert threshold (infection rate). */
    private static final double THRESHOLD_CRITICAL = 0.30;

    /** Wall monitored by this sensor. */
    private final Wall wall;
    /** Shelves monitored by this sensor. */
    private final List<Shelf> shelves;
    /** Last emitted global alert level. */
    private AlertLevel lastAlertLevel;
    /** Last emitted alert level per shelf. */
    private final Map<String, AlertLevel> shelfAlertLevels;

    /**
     * Creates a mold sensor for a wall with optional shelves.
     *
     * @param wall monitored wall
     * @param shelves monitored shelves (may be null)
     */
    public MoldSensor(Wall wall, List<Shelf> shelves) {
        if (wall == null) throw new IllegalArgumentException("Wall cannot be null");
        this.wall = wall;
        this.shelves = shelves != null ? shelves : List.of();
        this.lastAlertLevel = AlertLevel.LOW;
        this.shelfAlertLevels  = new HashMap<>();
    }

    /**
     * Creates a mold sensor for a wall without shelves.
     *
     * @param wall monitored wall
     */
    public MoldSensor(Wall wall) {
        this(wall, List.of());
    }

    /**
     * Measures infection rate on the wall.
     *
     * @return infection rate between 0 and 1
     */
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

    /**
     * Converts an infection rate into an alert level.
     *
     * @param rate infection rate
     * @return corresponding alert level
     */
    public AlertLevel toAlertLevel(double rate) {
        if (rate >= THRESHOLD_CRITICAL) {
            return AlertLevel.CRITICAL;
        }
        if (rate >= THRESHOLD_HIGH) {
            return AlertLevel.HIGH;
        }
        if (rate >= THRESHOLD_MEDIUM) {
            return AlertLevel.MEDIUM;
        }
        return AlertLevel.LOW;
    }

    /**
     * Generates sensor events for the current simulation tick.
     *
     * @param currentWeek current simulation week
     * @return list of generated events
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

    /**
     * Checks shelves for infection or risk and generates events if needed.
     *
     * @param currentWeek simulation week
     * @param rate infection rate
     * @return generated shelf-related events
     */
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

    /**
     * Emits a shelf event if the alert level has increased.
     */
    private void emitIfEscalated(Shelf shelf, AlertLevel level, int currentWeek, List<SensorEvent> events, double rate) {
        AlertLevel last = shelfAlertLevels.getOrDefault(shelf.getId(), AlertLevel.LOW);
        if (level.ordinal() > last.ordinal()) {
            shelfAlertLevels.put(shelf.getId(), level);
            events.add(new SensorEvent(currentWeek, level, rate, wall, EventType.SHELF, shelf));
        }
    }

    /**
     * Checks if a shelf is considered sensitive.
     *
     * @param shelf shelf to evaluate
     * @return true if sensitive
     */
    private boolean isSensitive(Shelf shelf) {
        return shelf.getValue() == ShelfValue.HIGH || shelf.getValue() == ShelfValue.CRITICAL;
    }

    /**
     * Resets the sensor state.
     */
    public void reset() {
        this.lastAlertLevel = AlertLevel.LOW;
        this.shelfAlertLevels.clear();
    }

    /**
     * Returns the shelf located at the given coordinates if any.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return matching shelf or null
     */
    private Shelf getShelfAt(int x, int y) {
        for (Shelf shelf : shelves) {
            if (x >= shelf.getX() && x < shelf.getX() + shelf.getWidth() && y >= shelf.getY() && y < shelf.getY() + shelf.getHeight()) {
                return shelf;
            }
        }
        return null;
    }

    /**
     * Returns the monitored wall.
     *
     * @return wall
     */
    public Wall getWall() {
        return wall;
    }

    /**
     * Returns the last global alert level.
     *
     * @return alert level
     */
    public AlertLevel getLastAlertLevel() {
        return lastAlertLevel;
    }

}
