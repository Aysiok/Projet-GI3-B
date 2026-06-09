package moldsim.model;

import java.io.Serializable;

/**
 * Represents a shelf unit in the archive room.
 */
public class Shelf implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int plankCount;
    private DocumentValue value;

    public Shelf(String id, int x, int y, int width, int height, int plankCount, DocumentValue value) {
        this.id         = id;
        this.x          = x;
        this.y          = y;
        this.width      = width;
        this.height     = height;
        this.plankCount = plankCount;
        this.value      = value;
    }

    public String getId()           { return id; }
    public int getX()               { return x; }
    public int getY()               { return y; }
    public int getWidth()           { return width; }
    public int getHeight()          { return height; }
    public int getPlankCount()      { return plankCount; }
    public DocumentValue getValue() { return value; }
    public void setValue(DocumentValue value) { this.value = value; }

    @Override
    public String toString() {
        return "Shelf[" + id + ", pos=(" + x + "," + y + ")"
            + ", " + width + "x" + height
            + ", planks=" + plankCount
            + ", value=" + value + "]";
    }
}