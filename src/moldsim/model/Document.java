package moldsim.model;

import java.io.Serializable;

/**
 * Represents a document stored on a shelf in the archive.
 * A document can be healthy, contaminated, damaged or destroyed.
 */
public class Document implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final DocumentValue value;
    private CellState state;
    private MoldSpecies species;
    private double moldLevel;  // 0..100

    /**
     * Creates a healthy document.
     * @param id    unique identifier of the document
     * @param value patrimonial value of the document
     */
    public Document(String id, DocumentValue value) {
        if (id == null || value == null) {
            throw new IllegalArgumentException("Id and value cannot be null");
        }
        this.id        = id;
        this.value     = value;
        this.state     = CellState.HEALTHY;
        this.species   = null;
        this.moldLevel = 0.0;
    }

    /** Contaminates the document with a mold species. */
    public void contaminate(MoldSpecies species) {
        if (species == null) {
            throw new IllegalArgumentException("Species cannot be null");
        }
        if (this.state == CellState.DEAD) return;
        this.state   = CellState.INFECTED;
        this.species = species;
        if (this.moldLevel < 1.0) this.moldLevel = 1.0;
    }

    /** Treats the document — removes contamination. */
    public void treat() {
        if (this.state == CellState.DEAD) return;
        this.state     = CellState.HEALTHY;
        this.species   = null;
        this.moldLevel = 0.0;
    }

    /** Destroys the document permanently. */
    public void destroy() {
        this.state     = CellState.DEAD;
        this.species   = null;
    }

    public boolean isContaminated() { return state == CellState.INFECTED; }
    public boolean isAlive()        { return state != CellState.DEAD; }

    public String getId()            { return id; }
    public DocumentValue getValue()  { return value; }
    public CellState getState()      { return state; }
    public MoldSpecies getSpecies()  { return species; }
    public double getMoldLevel()     { return moldLevel; }

    public void setMoldLevel(double level) {
        this.moldLevel = Math.max(0.0, Math.min(100.0, level));
    }

    @Override
    public String toString() {
        return "Document[" + id + ", " + value + ", " + state
            + (species != null ? ", " + species.getDisplayName() : "")
            + ", mold=" + String.format("%.1f", moldLevel) + "]";
    }
}