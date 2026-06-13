package moldsim.model;

import java.io.Serializable;
import java.util.List;

import moldsim.controller.WallContext;

/**
 * Complete serialized state of the simulation used for save and load operations.
 * <p>
 * This object contains all wall contexts, environment values, simulation step,
 * and historical snapshots required to fully restore a simulation session.
 */
public class SimulationState implements Serializable {
    /** Serialization version identifier. */
    private static final long serialVersionUID = 1L;

    /** List of all wall contexts in the simulation. */
    private final List<WallContext> wallContexts;
    /** Global humidity value at save time. */
    private final double humidity;
    /** Global temperature value at save time. */
    private final double temperature;
    /** Global ventilation value at save time. */
    private final double ventilation;
    /** Current simulation step index. */
    private final int step;
    /** List of simulation snapshots representing the simulation history. */
    private final List<SimulationSnapshot> history;

    /**
     * Creates a full simulation state from runtime data.
     *
     * @param wallContexts list of wall contexts
     * @param env environment at save time
     * @param step current simulation step
     * @param history simulation snapshot history
     */
    public SimulationState(List<WallContext> wallContexts, Environment env,
                           int step, List<SimulationSnapshot> history) {
        this.wallContexts  = wallContexts;
        this.humidity      = env.getHumidity();
        this.temperature   = env.getTemperature();
        this.ventilation   = env.getVentilation();
        this.step          = step;
        this.history       = history;
    }

    /**
     * Returns all wall contexts.
     *
     * @return list of wall contexts
     */
    public List<WallContext> getWallContexts() { 
        return wallContexts; 
    }

    /**
     * Returns saved humidity value.
     *
     * @return humidity
     */
    public double getHumidity() {
        return humidity; 
    }

    /**
     * Returns saved temperature value.
     *
     * @return temperature
     */
    public double getTemperature() {
        return temperature; 
    }

    /**
     * Returns saved ventilation value.
     *
     * @return ventilation
     */
    public double getVentilation() {
        return ventilation; 
    }

    /**
     * Returns simulation step index.
     *
     * @return step index
     */
    public int getStep() {
        return step; 
    }

    /**
     * Returns simulation history snapshots.
     *
     * @return list of snapshots
     */
    public List<SimulationSnapshot> getHistory() {
        return history; 
    }
}