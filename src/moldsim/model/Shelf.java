package moldsim.model;

import java.io.Serializable;

/**
 * Represents a shelf unit in the archive room.
 * <p>
 * A shelf has a position, dimensions, number of planks, and a value
 * indicating its importance or sensitivity in the simulation.
 */
public class Shelf implements Serializable {

    /**
     * Serialization version identifier.
     */
    private static final long serialVersionUID = 1L;

    /** Unique identifier of the shelf. */
    private final String id;
    /** X coordinate of the shelf position. */
    private final int x;
    /** Y coordinate of the shelf position. */
    private final int y;
    /** Width of the shelf. */
    private final int width;
    /** Height of the shelf. */
    private final int height;
    /** Number of planks in the shelf. */
    private final int plankCount;
    /** Value or sensitivity level of the shelf. */
    private ShelfValue value;

    /**
     * Creates a shelf with specified properties.
     *
     * @param id shelf identifier
     * @param x x position
     * @param y y position
     * @param width shelf width
     * @param height shelf height
     * @param plankCount number of planks
     * @param value shelf value
     */
    public Shelf(String id, int x, int y, int width, int height, int plankCount, ShelfValue value) {
        this.id         = id;
        this.x          = x;
        this.y          = y;
        this.width      = width;
        this.height     = height;
        this.plankCount = plankCount;
        this.value      = value;
    }

    /**
     * Returns the shelf identifier.
     *
     * @return shelf id
     */
    public String getId() {
        return id; 
    }

    /**
     * Returns the x coordinate.
     *
     * @return x position
     */
    public int getX() {
        return x; 
    }

    /**
     * Returns the y coordinate.
     *
     * @return y position
     */
    public int getY() {
        return y; 
    }

    /**
     * Returns the shelf width.
     *
     * @return width
     */
    public int getWidth() { 
        return width; 
    }

    /**
     * Returns the shelf height.
     *
     * @return height
     */
    public int getHeight() {
        return height; 
    }

    /**
     * Returns the number of planks.
     *
     * @return plank count
     */
    public int getPlankCount() {
        return plankCount; 
    }

    /**
     * Returns the shelf value.
     *
     * @return shelf value
     */
    public ShelfValue getValue() { 
        return value; 
    }

    /**
     * Sets the shelf value.
     *
     * @param value new shelf value
     */
    public void setValue(ShelfValue value) {
        this.value = value; 
    }

    /**
     * Returns a string representation of the shelf.
     *
     * @return formatted shelf description
     */
    @Override
    public String toString() {
        return "Shelf[" + id + ", pos=(" + x + "," + y + ")" + ", " + width + "x" + height + ", planks=" + plankCount + ", value=" + value + "]";
    }
}