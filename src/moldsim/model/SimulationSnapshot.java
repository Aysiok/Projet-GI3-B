package moldsim.model;

/**
 * Represents a saved state of the simulation at a given step.
 */
public class SimulationSnapshot {

    private final int step;
    private final int week;
    private final int[][] cellStates;

    public SimulationSnapshot(int step, int week, int[][] cellStates) {
        this.step = step;
        this.week = week;
        this.cellStates = cellStates;
    }

    public int getStep() {
        return step;
    }

    public int getWeek() {
        return week;
    }

    public int[][] getCellStates() {
        return cellStates;
    }
}