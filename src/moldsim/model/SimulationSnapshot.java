package moldsim.model;

import java.util.List;

/**
 * Represents a saved state of the simulation at a given step.
 */
public class SimulationSnapshot {

    private final int week;
    private final int[][] cellStates;

    private final double humidity;
    private final double temperature;
    private final double ventilation;
    private final WallMaterial material;
    private final List<String> alertLogs;

    public SimulationSnapshot(int week, int[][] cellStates, double humidity, double temperature, double ventilation, WallMaterial material, List<String> alertLogs) {
        this.week = week;
        this.cellStates = cellStates;
        this.humidity = humidity;
        this.temperature = temperature;
        this.ventilation = ventilation;
        this.material = material;
        this.alertLogs = alertLogs;
    }

    public int getWeek() {
        return week;
    }

    public int[][] getCellStates() {
        return cellStates;
    }

    public double getHumidity() {
        return humidity;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getVentilation() {
        return ventilation;
    }

    public WallMaterial getMaterial() {
        return material;
    }

    public List<String> getAlertLogs() {
        return alertLogs;
    }
}