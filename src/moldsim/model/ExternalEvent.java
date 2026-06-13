package moldsim.model;

/**
 * Represents external events that can affect the simulation environment or walls.
 * <p>
 * These events simulate environmental disturbances or interventions such as
 * water leaks, ventilation changes, and anti-mold treatments.
 */
public enum ExternalEvent {
    /** Water leak event increasing local humidity. */
    WATER_LEAK,

    /** HVAC system failure disabling ventilation. */
    HVAC_FAILURE,

    /** Window opening temporarily increasing ventilation. */
    WINDOW_OPENED,

    /** Anti-mold treatment applied to a wall zone. */
    ANTI_MOLD_TREATMENT_WALL,

    /** Anti-mold treatment applied to a shelf. */
    ANTI_MOLD_TREATMENT_SHELF
}
