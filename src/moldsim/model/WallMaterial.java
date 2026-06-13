package moldsim.model;

/**
 * Represents the material composition of a wall in the simulation.
 * <p>
 * Each material influences mold growth behavior, humidity absorption,
 * and overall contamination resistance.
 */
public enum WallMaterial {
    /** Concrete wall material with high resistance to mold. */
    CONCRETE,

    /** Wood material highly susceptible to mold growth. */
    WOOD,

    /** Plaster material with متوسط susceptibility to humidity. */
    PLASTER,

    /** Brick material with moderate resistance to contamination. */
    BRICK,

    /** Paper/document material extremely sensitive to mold. */
    DOCUMENT;
}