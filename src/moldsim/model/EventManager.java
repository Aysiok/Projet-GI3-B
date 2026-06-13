package moldsim.model;

public class EventManager {

    private final Environment environment;

    public EventManager(Environment environment) {
        this.environment = environment;
    }

    public void apply(ExternalEvent event, Wall wall, int x, int y, int radius) {
        switch (event) {
            case WATER_LEAK -> applyWaterLeak(wall, x, y, radius);
            case HVAC_FAILURE -> environment.setVentilation(0);
            case WINDOW_OPENED -> environment.setVentilation(Math.min(100, environment.getVentilation() + 40));
            case ANTI_MOLD_TREATMENT_WALL -> treatWallZone(wall, x, y, radius);
            case ANTI_MOLD_TREATMENT_SHELF -> {} // géré séparément avec la shelf
        }
    }

    private void applyWaterLeak(Wall wall, int x, int y, int radius) {
        environment.setHumidity(Math.min(100, environment.getHumidity() + 30));
        for (Cell[] row : wall.getGrid()) {
            for (Cell cell : row) {
                int dx = cell.getX() - x, dy = cell.getY() - y;
                if (dx*dx + dy*dy <= radius*radius && cell.isAlive()) {
                    cell.infect(MoldSpecies.STACHYBOTRYS); // moisissure noire = fuite d'eau
                }
            }
        }
    }

    private void treatWallZone(Wall wall, int x, int y, int radius) {
        for (Cell[] row : wall.getGrid()) {
            for (Cell cell : row) {
                int dx = cell.getX() - x, dy = cell.getY() - y;
                if (dx*dx + dy*dy <= radius*radius) cell.cure();
            }
        }
    }

    public void treatShelf(Wall wall, Shelf shelf) {
        for (int cx = shelf.getX(); cx < shelf.getX() + shelf.getWidth(); cx++) {
            for (int cy = shelf.getY(); cy < shelf.getY() + shelf.getHeight(); cy++) {
                Cell cell = wall.getCell(cx, cy);
                if (cell != null) cell.cure();
            }
        }
    }
}
