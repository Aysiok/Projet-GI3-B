package moldsim.model;

/**
 * Computes and stores statistics about the grid at a given step.
 */
public class Statistics {

    private final int totalCells;
    private final int healthyCells;
    private final int infectedCells;
    private final int deadCells;
    private final double averageAge;
    private final double averageMoldLevel;
    private final double maxMoldLevel;
    private final double minMoldLevel;

    // Pour la tendance : on compare avec le step précédent
    private final int previousInfectedCells;

    public Statistics(Grid grid, int previousInfectedCells) {
        this.previousInfectedCells = previousInfectedCells;

        int healthy = 0, infected = 0, dead = 0;
        double totalAge = 0, totalMold = 0;
        double maxMold = 0, minMold = 100;

        for (Cell[] row : grid.getCells()) {
            for (Cell c : row) {
                switch (c.getState()) {
                    case HEALTHY  -> healthy++;
                    case INFECTED -> infected++;
                    case DEAD     -> dead++;
                }
                totalAge  += c.getAge();
                totalMold += c.getMoldLevel();
                if (c.getMoldLevel() > maxMold) maxMold = c.getMoldLevel();
                if (c.isInfected() && c.getMoldLevel() < minMold) minMold = c.getMoldLevel();
            }
        }

        this.totalCells    = grid.getWidth() * grid.getHeight();
        this.healthyCells  = healthy;
        this.infectedCells = infected;
        this.deadCells     = dead;
        this.averageAge    = totalAge  / totalCells;
        this.averageMoldLevel = totalMold / totalCells;
        this.maxMoldLevel  = maxMold;
        this.minMoldLevel  = infected > 0 ? minMold : 0;
    }

    /** Tendance : hausse, baisse ou stable */
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

    // Getters pour le PDF et JavaFX plus tard
    public int getTotalCells()       { return totalCells; }
    public int getHealthyCells()     { return healthyCells; }
    public int getInfectedCells()    { return infectedCells; }
    public int getDeadCells()        { return deadCells; }
    public double getAverageAge()    { return averageAge; }
    public double getAverageMoldLevel() { return averageMoldLevel; }
    public double getMaxMoldLevel()  { return maxMoldLevel; }
    public double getMinMoldLevel()  { return minMoldLevel; }
}