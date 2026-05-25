package moldsim.model;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * partie asma
 * la grille en 2d qui servira au javafx
 */
public class Grid implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int width;
    private final int height;
    private final boolean toric;          // si true, les bords se rebouclent
    private NeighborhoodMode neighborhoodMode;
    private final Cell[][] cells;

    /** grille avec des cellules same */
    public Grid(int width,
                int height,
                boolean toric,
                NeighborhoodMode neighborhoodMode) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Grid size must be positive");
        }
        if (neighborhoodMode == null) {
            throw new IllegalArgumentException("Neighborhood mode cannot be null");
        }
        this.width = width;
        this.height = height;
        this.toric = toric;
        this.neighborhoodMode = neighborhoodMode;
        this.cells = new Cell[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                cells[y][x] = new Cell(x, y);
            }
        }
    }

    public Cell getCell(int x, int y) {
        if (toric) {
            x = wrap(x, width);
            y = wrap(y, height);
        } else if (!inBounds(x, y)) {
            return null;
        }
        return cells[y][x];
    }

    /** voisins d'une cellule */
    public List<Cell> getNeighbors(int x, int y) {
        List<Cell> neighbors = new ArrayList<>(8);
        int[][] offsets = (neighborhoodMode == NeighborhoodMode.FOUR)
                ? new int[][] { {0,-1}, {0,1}, {-1,0}, {1,0} }
                : new int[][] {
                        {-1,-1}, {0,-1}, {1,-1},
                        {-1, 0},          {1, 0},
                        {-1, 1}, {0, 1}, {1, 1}
                };
        for (int[] offset : offsets) {
            Cell neighbor = getCell(x + offset[0], y + offset[1]);
            if (neighbor != null) {
                neighbors.add(neighbor);
            }
        }
        return neighbors;
    }

    public List<Cell> getNeighbors(Cell cell) {
        return getNeighbors(cell.getX(), cell.getY());
    }


    /** une cellule s'infecte */
    public void randomlyInfect(int count, MoldSpecies species, Random random) {
        if (species == null || random == null) {
            throw new IllegalArgumentException("Species and random cannot be null");
        }
        int total = width * height;
        int toInfect = Math.min(count, total);
        int infected = 0;
        int safety = total * 4; 
        while (infected < toInfect && safety-- > 0) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            Cell c = cells[y][x];
            if (!c.isInfected() && c.isAlive()) {
                c.infect(species);
                infected++;
            }
        }
    }

    /** retour à que des cellules saines */
    public void reset(double humidity, double temperature) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Cell c = cells[y][x];
                c.cure();
                c.setAge(0);
            }
        }
    }


    /** dans la grille ? */
    public boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /** quand c'est des valeurs négatives */
    private static int wrap(int value, int size) {
        int r = value % size;
        return (r < 0) ? r + size : r;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isToric() { return toric; }
    public NeighborhoodMode getNeighborhoodMode() { return neighborhoodMode; }

    public void setNeighborhoodMode(NeighborhoodMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("Mode cannot be null");
        }
        this.neighborhoodMode = mode;
    }

    /** tableau en 2d */
    public Cell[][] getCells() {
        return cells;
    }
}
