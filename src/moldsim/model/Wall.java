package moldsim.model;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Represents a 2D grid wall used in the simulation.
 * <p>
 * A wall is composed of cells arranged in a rectangular grid. It supports
 * infection simulation, neighbor retrieval, and material-dependent behavior.
 */
public class Wall implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Width of the wall grid. */
    private final int width;
    /** Height of the wall grid. */
    private final int height;
    /** 2D grid of cells composing the wall. */
    private final Cell[][] grid;
    /** Material composing the wall. */
    private WallMaterial material;

    /**
     * Creates a wall with specified dimensions and material.
     *
     * @param width wall width
     * @param height wall height
     * @param material wall material
     */
    public Wall(int width, int height, WallMaterial material) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Grid size must be positive");
        }
        this.width = width;
        this.height = height;
        this.material = material;
        this.grid = new Cell[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                grid[y][x] = new Cell(x, y);
            }
        }
    }

    /**
     * Creates a wall with default material (CONCRETE).
     *
     * @param width wall width
     * @param height wall height
     */
    public Wall(int width, int height){
        this(width, height, WallMaterial.CONCRETE);
    }


    /**
     * Returns the cell at given coordinates if inside bounds.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return cell or null if out of bounds
     */
    public Cell getCell(int x, int y) {
        if (inBounds(x, y)){
            return grid[y][x];
        }
        else return null;
    }

    /**
     * Returns all neighboring cells (8-directional).
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return list of neighbor cells
     */
    public List<Cell> getNeighbors(int x, int y) {
        List<Cell> neighbors = new ArrayList<>(8);
        int[][] offsets = {
            {-1,-1}, {0,-1}, {1,-1},
            {-1, 0},         {1, 0},
            {-1, 1}, {0, 1}, {1, 1}
        };
        for (int[] o : offsets) {
            int nx = x + o[0], ny = y + o[1];
            if (inBounds(nx, ny)) neighbors.add(grid[ny][nx]);
        }
        return neighbors;
    }

    /**
     * Returns neighbors of a given cell.
     *
     * @param cell reference cell
     * @return list of neighbor cells
     */
    public List<Cell> getNeighbors(Cell cell) {
        return getNeighbors(cell.getX(), cell.getY());
    }


    /**
     * Randomly infects a number of cells on the wall.
     *
     * @param count number of cells to infect
     * @param species mold species used for infection
     * @param random random generator
     */
    public void randomlyInfect(int count, MoldSpecies species, Random random) {
        if (random == null) {
            throw new IllegalArgumentException("random cannot be null");
        }
        int total = width * height;
        int toInfect = Math.min(count, total);
        int infected = 0;
        int safety = total * 4; 
        while (infected < toInfect && safety-- > 0) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            Cell c = grid[y][x];
            if (!c.isInfected() && c.isAlive()) {
                c.infect(species);
                infected++;
            }
        }
    }

    /**
     * Resets all cells to a healthy state and clears ages.
     */
    public void reset() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Cell c = grid[y][x];
                c.cure();
                c.setAge(0);
            }
        }
    }

    /**
     * Checks whether coordinates are inside the grid bounds.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return true if inside bounds
     */
    public boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /**
     * Returns wall width.
     *
     * @return width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns wall height.
     *
     * @return height
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the full grid of cells.
     *
     * @return 2D cell array
     */
    public Cell[][] getGrid() {
        return grid;
    }

    /**
     * Returns the wall material.
     *
     * @return material
     */
    public WallMaterial getMaterial(){
        return material;
    }

    /**
     * Updates the wall material.
     *
     * @param material new material
     */
    public void setMaterial(WallMaterial material) {
        this.material = material;
    }
    
    /**
     * Computes the contamination rate of the wall.
     *
     * @return ratio of infected cells
     */
    public double getContaminationRate() {
        int infected = 0;
        for (Cell[] row : grid) {
            for (Cell cell : row) {
                if (cell.isInfected()) infected++;
            }
        }
        return (double) infected / (width * height);
    }

}
