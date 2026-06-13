package moldsim.view;

/**
 * Defines interaction modes available in the simulation UI.
 * <p>
 * Each mode determines how user actions are interpreted when interacting
 * with the simulation grid (editing cells, applying treatments, or placing events).
 */
public enum InteractionMode {
    /** No interaction mode active. */
    NONE,

    /** Mode for adding mold to cells. */
    ADD_MOLD,

    /** Mode for applying treatment to wall cells. */
    TREAT_WALL,

    /** Mode for applying treatment to shelf structures. */
    TREAT_SHELF,

    /** Mode for placing simulation events. */
    PLACE_EVENT
}