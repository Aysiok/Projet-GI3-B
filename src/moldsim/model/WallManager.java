
package moldsim.model;

import java.util.ArrayList;
import java.util.List;

import moldsim.controller.WallContext;

/**
 * Manages a collection of wall contexts and provides navigation between them.
 * <p>
 * This class allows switching between multiple walls in the simulation,
 * maintaining a current active wall index and providing access to adjacent walls.
 */
public class WallManager {

    /** List of all registered wall contexts. */
    private final List<WallContext> walls;
    /** Index of the currently active wall. */
    private int currentWallIndex;

    /**
     * Creates an empty wall manager with no registered walls.
     */
    public WallManager() {
        this.walls = new ArrayList<>();
        this.currentWallIndex = 0;
    }

    /**
     * Adds a new wall context to the manager.
     *
     * @param wallContext wall context to add
     */
    public void addWall(WallContext wallContext) {
        walls.add(wallContext);
    }

    /**
     * Returns the currently active wall context.
     *
     * @return current wall context
     */
    public WallContext getCurrentWallContext() {
        return walls.get(currentWallIndex);
    }

    /**
     * Returns the previous wall context in circular order.
     *
     * @return previous wall context
     */
    public WallContext getPreviousWallContext() {
        int index = (currentWallIndex - 1 + walls.size()) % walls.size();
        return walls.get(index);
    }

    /**
     * Returns the next wall context in circular order.
     *
     * @return next wall context
     */
    public WallContext getNextWallContext() {
        int index = (currentWallIndex + 1) % walls.size();
        return walls.get(index);
    }

    /**
     * Moves the current index to the previous wall.
     */
    public void moveToPreviousWall() {
        currentWallIndex = (currentWallIndex - 1 + walls.size()) % walls.size();
    }

    /**
     * Moves the current index to the next wall.
     */
    public void moveToNextWall() {
        currentWallIndex = (currentWallIndex + 1) % walls.size();
    }

    /**
     * Returns the list of all wall contexts.
     *
     * @return list of wall contexts
     */
    public List<WallContext> getWalls() {
        return walls;
    }

    /**
     * Returns the index of the currently active wall.
     *
     * @return current wall index
     */
    public int getCurrentWallIndex() {
        return currentWallIndex;
    }
}