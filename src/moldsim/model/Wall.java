package moldsim.model;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * le mur en 2d qui servira au javafx
 */
public class Wall implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int width;
    private final int height;
    private final Cell[][] grid;
    private final WallMaterial material;

    /** mur avec des cellules same */
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

    public Wall(int width, int height){
        this(width, height, WallMaterial.CONCRETE);
    }


    public Cell getCell(int x, int y) {
        return grid[y][x];
    }

    /** voisins d'une cellule */
    public List<Cell> getNeighbors(int x, int y) {
        List<Cell> neighbors = new ArrayList<>(8);
        int[][] offsets = new int[][] {
                        {-1,-1}, {0,-1}, {1,-1},
                        {-1, 0},         {1, 0},
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
            Cell c = grid[y][x];
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
                Cell c = grid[y][x];
                c.cure();
                c.setAge(0);
            }
        }
    }

    /** dans la grille ? */
    public boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    /** tableau en 2d */
    public Cell[][] getCells() {
        return grid;
    }

    public WallMaterial getMaterial(){
        return material;
    }
    
}
