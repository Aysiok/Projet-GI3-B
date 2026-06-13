package moldsim.model;

/**
 * Handles external events that modify the simulation environment and walls.
 * <p>
 * This class applies environmental effects such as humidity changes,
 * ventilation failures, water leaks, and anti-mold treatments.
 */
public class EventManager {

    /**
     * Global simulation environment affected by external events.
     */
    private final Environment environment;

    /**
     * Creates an event manager bound to a simulation environment.
     *
     * @param environment simulation environment
     */
    public EventManager(Environment environment) {
        this.environment = environment;
    }

    /**
     * Applies an external event to the simulation.
     *
     * @param event event type to apply
     * @param wall target wall
     * @param x center x coordinate
     * @param y center y coordinate
     * @param radius effect radius
     */
    public void apply(ExternalEvent event, Wall wall, int x, int y, int radius) {
        switch (event) {
            case WATER_LEAK -> applyWaterLeak(wall, x, y, radius);
            case HVAC_FAILURE -> environment.setVentilation(0);
            case WINDOW_OPENED -> environment.setVentilation(Math.min(100, environment.getVentilation() + 40));
            case ANTI_MOLD_TREATMENT_WALL -> treatWallZone(wall, x, y, radius);
            case ANTI_MOLD_TREATMENT_SHELF -> {} // handled separately with the shelf
        }
    }

    /**
     * Simulates a water leak by increasing humidity and infecting nearby cells.
     *
     * @param wall target wall
     * @param x leak center x coordinate
     * @param y leak center y coordinate
     * @param radius effect radius
     */
    private void applyWaterLeak(Wall wall, int x, int y, int radius) {
        environment.setHumidity(Math.min(100, environment.getHumidity() + 30));
        for (Cell[] row : wall.getGrid()) {
            for (Cell cell : row) {
                int dx = cell.getX() - x, dy = cell.getY() - y;
                if (dx*dx + dy*dy <= radius*radius && cell.isAlive()) {
                    cell.infect(MoldSpecies.STACHYBOTRYS);
                }
            }
        }
    }

    /**
     * Applies anti-mold treatment to a circular area of a wall.
     *
     * @param wall target wall
     * @param x center x coordinate
     * @param y center y coordinate
     * @param radius treatment radius
     */
    private void treatWallZone(Wall wall, int x, int y, int radius) {
        for (Cell[] row : wall.getGrid()) {
            for (Cell cell : row) {
                int dx = cell.getX() - x, dy = cell.getY() - y;
                if (dx*dx + dy*dy <= radius*radius) cell.cure();
            }
        }
    }

    /**
     * Applies anti-mold treatment to a shelf area.
     *
     * @param wall target wall
     * @param shelf shelf to treat
     */
    public void treatShelf(Wall wall, Shelf shelf) {
        for (int cx = shelf.getX(); cx < shelf.getX() + shelf.getWidth(); cx++) {
            for (int cy = shelf.getY(); cy < shelf.getY() + shelf.getHeight(); cy++) {
                Cell cell = wall.getCell(cx, cy);
                if (cell != null) cell.cure();
            }
        }
    }
}
