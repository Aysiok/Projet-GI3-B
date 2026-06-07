package moldsim.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a shelf unit in the archive room.
 * A shelf contains multiple planks, each holding documents.
 */
public class Shelf implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final int x;           // position on the grid (top-left)
    private final int y;
    private final int width;       // in cells
    private final int height;      // in cells
    private final int plankCount;  // number of horizontal planks
    private final List<List<Document>> planks; // one list per plank

    /**
     * Creates a shelf at a given position on the grid.
     * @param id         unique identifier
     * @param x          column position (top-left)
     * @param y          row position (top-left)
     * @param width      width in cells
     * @param height     height in cells
     * @param plankCount number of planks inside the shelf
     */
    public Shelf(String id, int x, int y, int width, int height, int plankCount) {
        if (id == null) throw new IllegalArgumentException("Id cannot be null");
        if (width <= 0 || height <= 0 || plankCount <= 0) {
            throw new IllegalArgumentException("Width, height and plankCount must be positive");
        }
        this.id         = id;
        this.x          = x;
        this.y          = y;
        this.width      = width;
        this.height     = height;
        this.plankCount = plankCount;
        this.planks     = new ArrayList<>();
        for (int i = 0; i < plankCount; i++) {
            planks.add(new ArrayList<>());
        }
    }

    /**
     * Adds a document to a specific plank.
     * @param plankIndex index of the plank (0-based)
     * @param document   the document to add
     */
    public void addDocument(int plankIndex, Document document) {
        if (plankIndex < 0 || plankIndex >= plankCount) {
            throw new IllegalArgumentException("Invalid plank index: " + plankIndex);
        }
        if (document == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }
        planks.get(plankIndex).add(document);
    }

    /**
     * Returns all documents on a specific plank.
     * @param plankIndex index of the plank (0-based)
     */
    public List<Document> getDocumentsOnPlank(int plankIndex) {
        if (plankIndex < 0 || plankIndex >= plankCount) {
            throw new IllegalArgumentException("Invalid plank index: " + plankIndex);
        }
        return planks.get(plankIndex);
    }

    /** Returns all documents in the shelf across all planks. */
    public List<Document> getAllDocuments() {
        List<Document> all = new ArrayList<>();
        for (List<Document> plank : planks) {
            all.addAll(plank);
        }
        return all;
    }

    /** Returns the number of contaminated documents in this shelf. */
    public int countContaminated() {
        int count = 0;
        for (Document doc : getAllDocuments()) {
            if (doc.isContaminated()) count++;
        }
        return count;
    }

    /** Returns true if at least one document is contaminated. */
    public boolean isContaminated() {
        return countContaminated() > 0;
    }

    public String getId()       { return id; }
    public int getX()           { return x; }
    public int getY()           { return y; }
    public int getWidth()       { return width; }
    public int getHeight()      { return height; }
    public int getPlankCount()  { return plankCount; }
    public List<List<Document>> getPlanks() { return planks; }

    @Override
    public String toString() {
        return "Shelf[" + id + ", pos=(" + x + "," + y + ")"
            + ", " + width + "x" + height
            + ", planks=" + plankCount
            + ", contaminated=" + countContaminated() + "/" + getAllDocuments().size()
            + "]";
    }
}