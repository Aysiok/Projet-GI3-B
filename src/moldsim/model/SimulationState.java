package moldsim.model;

import java.io.Serializable;
import java.util.List;

/**
 * Complete simulation state for binary save/load.
 * Contains everything needed to fully restore a simulation.
 */
public class SimulationState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<WallContext> wallContexts;
    private final double humidity;
    private final double temperature;
    private final double ventilation;
    private final int step;
    private final List<SimulationSnapshot> history;

    public SimulationState(List<WallContext> wallContexts, Environment env,
                           int step, List<SimulationSnapshot> history) {
        this.wallContexts  = wallContexts;
        this.humidity      = env.getHumidity();
        this.temperature   = env.getTemperature();
        this.ventilation   = env.getVentilation();
        this.step          = step;
        this.history       = history;
    }

    public List<WallContext> getWallContexts() { return wallContexts; }
    public double getHumidity()                { return humidity; }
    public double getTemperature()             { return temperature; }
    public double getVentilation()             { return ventilation; }
    public int getStep()                       { return step; }
    public List<SimulationSnapshot> getHistory(){ return history; }
}