package moldsim.model;

import java.util.ArrayList;
import java.util.List;

public class WallManager {

    private final List<WallContext> walls;
    private int currentWallIndex;

    public WallManager() {
        this.walls = new ArrayList<>();
        this.currentWallIndex = 0;
    }

    public void addWall(WallContext wallContext) {
        walls.add(wallContext);
    }

    public WallContext getCurrentWallContext() {
        return walls.get(currentWallIndex);
    }

    public WallContext getPreviousWallContext() {
        int index = (currentWallIndex - 1 + walls.size()) % walls.size();
        return walls.get(index);
    }

    public WallContext getNextWallContext() {
        int index = (currentWallIndex + 1) % walls.size();
        return walls.get(index);
    }

    public void moveToPreviousWall() {
        currentWallIndex = (currentWallIndex - 1 + walls.size()) % walls.size();
    }

    public void moveToNextWall() {
        currentWallIndex = (currentWallIndex + 1) % walls.size();
    }

    public List<WallContext> getWalls() {
        return walls;
    }

    public int getCurrentWallIndex() {
        return currentWallIndex;
    }
}