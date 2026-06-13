package moldsim.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable snapshot of the entire simulation state at a given simulation week.
 * <p>
 * A snapshot captures environmental conditions, wall cell states, and alert logs
 * in order to allow restoration or export of a previous simulation state.
 */
public class SimulationSnapshot implements Serializable{

    /** Serialization version identifier. */
    private static final long serialVersionUID = 1L;

    /** Simulation week represented by this snapshot. */
    private final int week;
    /** Deep-copied grid states of all walls. */
    private final List<int[][]> wallCellStates;

    /** Global humidity level at snapshot time. */
    private final double humidity;
    /** Global temperature at snapshot time. */
    private final double temperature;
    /** Global ventilation level at snapshot time. */
    private final double ventilation;
    /** Wall material configuration used during simulation. */
    private final WallMaterial material;
    /** Recorded alert messages at snapshot time. */
    private final List<String> alertLogs;

    /**
     * Creates a simulation snapshot without alert logs.
     *
     * @param week simulation week
     * @param wallCellStates state of all walls
     * @param humidity humidity level
     * @param temperature temperature level
     * @param ventilation ventilation level
     * @param material wall material configuration
     */
    public SimulationSnapshot(int week, List<int[][]> wallCellStates, double humidity, double temperature, double ventilation,WallMaterial material) {
        this(week, wallCellStates, humidity, temperature, ventilation, material, new ArrayList<>());
    }

    /**
     * Creates a simulation snapshot including alert logs.
     *
     * @param week simulation week
     * @param wallCellStates state of all walls
     * @param humidity humidity level
     * @param temperature temperature level
     * @param ventilation ventilation level
     * @param material wall material configuration
     * @param alertLogs recorded alert logs
     */
    public SimulationSnapshot(int week, List<int[][]> wallCellStates, double humidity, double temperature, double ventilation, WallMaterial material, List<String> alertLogs) {
        this.week = week;
        this.wallCellStates = deepCopyWallStates(wallCellStates);
        this.humidity = humidity;
        this.temperature = temperature;
        this.ventilation = ventilation;
        this.material = material;
        this.alertLogs = new ArrayList<>(alertLogs);
    }

    /**
     * Returns the simulation week of this snapshot.
     *
     * @return simulation week
     */
    public int getWeek() {
        return week;
    }

    /**
     * Returns a deep copy of all wall cell states.
     *
     * @return list of wall grids
     */
    public List<int[][]> getWallCellStates() {
        return deepCopyWallStates(wallCellStates);
    }

    /**
     * Returns humidity level at snapshot time.
     *
     * @return humidity
     */
    public double getHumidity() {
        return humidity;
    }

    /**
     * Returns temperature at snapshot time.
     *
     * @return temperature
     */
    public double getTemperature() {
        return temperature;
    }

    /**
     * Returns ventilation level at snapshot time.
     *
     * @return ventilation
     */
    public double getVentilation() {
        return ventilation;
    }

    /**
     * Returns wall material configuration.
     *
     * @return material
     */
    public WallMaterial getMaterial() {
        return material;
    }

    /**
     * Returns a copy of alert logs.
     *
     * @return list of alert messages
     */
    public List<String> getAlertLogs() {
        return new ArrayList<>(alertLogs);
    }

    /**
     * Creates a deep copy of all wall state grids.
     *
     * @param source original wall states
     * @return deep-copied list of grids
     */
    private static List<int[][]> deepCopyWallStates(List<int[][]> source) {
        List<int[][]> copy = new ArrayList<>();
        for (int[][] wallState : source) {
            copy.add(deepCopy(wallState));
        }
        return copy;
    }

    /**
     * Creates a deep copy of a 2D grid.
     *
     * @param source original grid
     * @return copied grid
     */
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