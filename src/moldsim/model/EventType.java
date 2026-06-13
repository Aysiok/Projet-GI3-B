package moldsim.model;

/**
 * Represents the type of sensor or system event occurring in the simulation.
 * <p>
 * Events can either affect an entire wall globally or target a specific shelf area.
 */
public enum EventType {
    /** Event affecting the entire wall globally. */
    GLOBAL,

    /** Event affecting a specific shelf or localized area. */
    SHELF;
}
