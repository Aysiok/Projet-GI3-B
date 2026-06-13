package moldsim.model;

/**
 * Represents all possible states of a cell in the simulation.
 * <p>
 * These states describe whether a cell is healthy, contains spores,
 * is infected, is sporulating, or is dead.
 */
public enum CellState {
    /** Healthy cell with no mold present. */
    HEALTHY,

    /** Cell containing deposited spores. */
    DEPOSITED_SPORE,

    /** Cell currently infected by mold. */
    INFECTED,

    /** Cell producing spores for propagation. */
    SPORULATING,

    /** Dead cell with no biological activity. */
    DEAD
}
