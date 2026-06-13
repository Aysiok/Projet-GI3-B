package moldsim.model;

/**
 * Computes and stores statistical indicators for a simulation step.
 * <p>
 * This class analyzes a wall grid and derives aggregated metrics such as
 * cell states distribution, mold intensity, and trend evolution compared
 * to the previous simulation step.
 */
public class Statistics {

    /** Total number of cells in the grid. */
    private final int totalCells;
    /** Number of healthy cells. */
    private final int healthyCells;
    /** Number of infected or active mold cells. */
    private final int infectedCells;
    /** Number of dead cells. */
    private final int deadCells;
    /** Average age of all cells. */
    private final double averageAge;
    /** Average mold level across all cells. */
    private final double averageMoldLevel;
    /** Maximum mold level observed. */
    private final double maxMoldLevel;
    /** Minimum mold level among infected cells. */
    private final double minMoldLevel;

    /** Number of infected cells at previous simulation step (used for trend calculation). */
    private final int previousInfectedCells;

    /**
     * Computes statistics from a wall grid.
     *
     * @param wall wall to analyze
     * @param previousInfectedCells infected cell count at previous step
     */
    public Statistics(Wall wall, int previousInfectedCells) {
        this.previousInfectedCells = previousInfectedCells;

        int healthy = 0, infected = 0, dead = 0;
        double totalAge = 0, totalMold = 0;
        double maxMold = 0, minMold = 100;

        for (Cell[] row : wall.getGrid()) {
            for (Cell c : row) {
                switch (c.getState()) {
                    case HEALTHY -> healthy++;
                    case DEPOSITED_SPORE -> healthy++; // spore deposited = not yet infected
                    case INFECTED -> infected++;
                    case SPORULATING -> infected++; // sporulating = active = counted as infected
                    case DEAD -> dead++;
                }
                totalAge += c.getAge();
                totalMold += c.getMoldLevel();
                if (c.getMoldLevel() > maxMold) {
                    maxMold = c.getMoldLevel();
                }
                if (c.isInfected() && c.getMoldLevel() < minMold) {
                    minMold = c.getMoldLevel();
                }
            }
        }

        this.totalCells = wall.getWidth() * wall.getHeight();
        this.healthyCells = healthy;
        this.infectedCells = infected;
        this.deadCells = dead;
        this.averageAge = totalAge  / totalCells;
        this.averageMoldLevel = totalMold / totalCells;
        this.maxMoldLevel = maxMold;
        this.minMoldLevel = infected > 0 ? minMold : 0;
    }

    /**
     * Computes infection trend compared to previous step.
     *
     * @return formatted trend indicator
     */
    public String getTrend() {
        int diff = infectedCells - previousInfectedCells;
        if (diff > 0) return "↑ +" + diff + " cells";
        if (diff < 0) return "↓ " + diff + " cells";
        return "→ stable";
    }

    @Override
    public String toString() {
        return "--- Statistics ---\n" +
            String.format("Healthy  : %d (%.1f%%)%n", healthyCells,  100.0 * healthyCells  / totalCells) +
            String.format("Infected : %d (%.1f%%)%n", infectedCells, 100.0 * infectedCells / totalCells) +
            String.format("Dead     : %d (%.1f%%)%n", deadCells,     100.0 * deadCells     / totalCells) +
            "Trend    : " + getTrend() + "\n" +
            String.format("Avg age  : %.1f steps%n",  averageAge) +
            String.format("Mold lvl : avg=%.1f  min=%.1f  max=%.1f%n", averageMoldLevel, minMoldLevel, maxMoldLevel) +
            "-------------------------------";
    }

    /**
     * Returns total number of cells.
     *
     * @return total cells
     */
    public int getTotalCells() {
        return totalCells; 
    }
    /**
     * Returns number of healthy cells.
     *
     * @return healthy cells
     */
    public int getHealthyCells() {
        return healthyCells; 
    }
    /**
     * Returns number of infected cells.
     *
     * @return infected cells
     */
    public int getInfectedCells() {
        return infectedCells; 
    }
    /**
     * Returns number of dead cells.
     *
     * @return dead cells
     */
    public int getDeadCells() {
        return deadCells; 
    }
    /**
     * Returns average cell age.
     *
     * @return average age
     */
    public double getAverageAge() {
        return averageAge; 
    }
    /**
     * Returns average mold level.
     *
     * @return average mold level
     */
    public double getAverageMoldLevel() { 
        return averageMoldLevel; 
    }
    /**
     * Returns maximum mold level observed.
     *
     * @return max mold level
     */
    public double getMaxMoldLevel() {
        return maxMoldLevel; 
    }
    /**
     * Returns minimum mold level among infected cells.
     *
     * @return min mold level
     */
    public double getMinMoldLevel() {
        return minMoldLevel; 
    }
}