package moldsim.model;
import java.io.Serializable;

/**
 * Represents a single cell in the wall grid.
 * <p>
 * A cell stores biological state information such as mold level,
 * infection state, species, and age.
 */
public class Cell implements Serializable {
    private static final long serialVersionUID = 1L;

    /** X coordinate of the cell. */
    private final int x;
    /** Y coordinate of the cell. */
    private final int y;

    /** Mold intensity level (0..100). */
    private double moldLevel;
    /** Age of the cell in simulation steps. */
    private int age;
    /** Current biological state of the cell. */
    private CellState state;
    /** Mold species currently present in the cell (null if none). */
    private MoldSpecies species;
    /** Material of the wall containing this cell. */
    private WallMaterial wallMaterial;

    /**
     * Creates a healthy cell at given coordinates.
     *
     * @param x x coordinate
     * @param y y coordinate
     */
    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
        this.moldLevel = 0.0;
        this.age = 0;
        this.state = CellState.HEALTHY;
        this.species = null;
    }

    /**
     * Returns the wall material of the cell.
     *
     * @return wall material
     */
    public WallMaterial getWallMaterial() {
        return wallMaterial;
    }

    /**
     * Sets the wall material of the cell.
     *
     * @param wallMaterial material to assign
     */
    public void setWallMaterial(WallMaterial wallMaterial){
        this.wallMaterial = wallMaterial; 
    }


    /**
     * Checks whether the cell is infected or sporulating.
     *
     * @return true if infected or sporulating
     */
    public boolean isInfected() {
        return state == CellState.INFECTED || state == CellState.SPORULATING;
    }

    /**
     * Checks whether the cell is not dead.
     *
     * @return true if the cell is alive
     */
    public boolean isAlive() {
        return state != CellState.DEAD;
    }

    /**
     * Infects the cell with a mold species.
     * If null is provided, a default species is used.
     *
     * @param species mold species
     */
    public void infect(MoldSpecies species) {
        if (species == null) {
            species = MoldSpecies.CLADOSPORIUM;
        }
        if (state == CellState.DEAD) return;
        this.state = CellState.INFECTED;
        this.species = species;
        if (this.moldLevel < 1.0) {
            this.moldLevel = 1.0;
        }
    }

    /**
     * Marks the cell as dead.
     */
    public void kill() {
        this.state = CellState.DEAD;
        this.species = null;
    }

    /**
     * Restores the cell to a healthy state.
     */
    public void cure() {
        if (state == CellState.DEAD) return;
        this.state = CellState.HEALTHY;
        this.species = null;
        this.moldLevel = 0.0;
    }

    /**
     * Increases the age of the cell by one simulation step.
     */
    public void incrementAge() {
        this.age++;
    }

    /**
     * Returns the x coordinate.
     *
     * @return x coordinate
     */
    public int getX() {
        return x;
    }

    /**
     * Returns the y coordinate.
     *
     * @return y coordinate
     */
    public int getY() {
        return y;
    }

    /**
     * Returns the mold level.
     *
     * @return mold level
     */
    public double getMoldLevel() {
        return moldLevel;
    }

    /**
     * Sets the mold level (clamped between 0 and 100).
     *
     * @param moldLevel new mold level
     */
    public void setMoldLevel(double moldLevel) {
        this.moldLevel = clamp(moldLevel);
    }

    /**
     * Returns the age of the cell.
     *
     * @return age
     */
    public int getAge() {
        return age;
    }

    /**
     * Sets the age of the cell.
     *
     * @param age new age value
     */
    public void setAge(int age) { 
        this.age = age;
    }

    /**
     * Returns the current state of the cell.
     *
     * @return cell state
     */
    public CellState getState() { 
        return state;
    }

    /**
     * Sets the state of the cell.
     *
     * @param state new state
     */
    public void setState(CellState state) {
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null");
        }
        this.state = state;
    }

    /**
     * Returns the mold species in the cell.
     *
     * @return mold species
     */
    public MoldSpecies getSpecies() {
        return species;
    }

    /**
     * Sets the mold species of the cell.
     *
     * @param species mold species
     */
    public void setSpecies(MoldSpecies species) { 
        this.species = species;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }

    @Override
    public String toString() {
        return "Cell(" + x + "," + y + ", " + state + (species != null ? " [" + species.name() + "]" : "") + ", mold=" + String.format("%.1f", moldLevel) + ", age=" + age + ")";
    }
}