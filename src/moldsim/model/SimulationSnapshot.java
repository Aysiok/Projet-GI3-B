package moldsim.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot of the whole simulation at a given week.
 * It stores the state of all walls, not only the displayed wall.
 */
public class SimulationSnapshot {

    private final int week;
    private final List<int[][]> wallCellStates;

    private final double humidity;
    private final double temperature;
    private final double ventilation;
    private final WallMaterial material;
    private final List<String> alertLogs;

    public SimulationSnapshot(int week, int[][] cellStates, double humidity, double temperature, double ventilation, WallMaterial material, List<String> alertLogs) {
    public SimulationSnapshot(
            int week,
            List<int[][]> wallCellStates,
            double humidity,
            double temperature,
            double ventilation,
            WallMaterial material
    ) {
        this.week = week;
        this.wallCellStates = deepCopyWallStates(wallCellStates);
        this.humidity = humidity;
        this.temperature = temperature;
        this.ventilation = ventilation;
        this.material = material;
        this.alertLogs = alertLogs;
    }

    public int getWeek() {
        return week;
    }

    public List<int[][]> getWallCellStates() {
        return deepCopyWallStates(wallCellStates);
    }

    // Ancienne méthode gardée au cas où du vieux code l'utilise encore.
    public int[][] getCellStates() {
        if (wallCellStates.isEmpty()) {
            return null;
        }
        return deepCopy(wallCellStates.get(0));
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
    private static List<int[][]> deepCopyWallStates(List<int[][]> source) {
        List<int[][]> copy = new ArrayList<>();

        for (int[][] wallState : source) {
            copy.add(deepCopy(wallState));
        }

        return copy;
    }

    private static int[][] deepCopy(int[][] source) {
        if (source == null) {
            return null;
        }

        int[][] copy = new int[source.length][];

        for (int row = 0; row < source.length; row++) {
            copy[row] = new int[source[row].length];
            for (int col = 0; col < source[row].length; col++) {
                copy[row][col] = source[row][col];
            }
        }

        return copy;
    }
}