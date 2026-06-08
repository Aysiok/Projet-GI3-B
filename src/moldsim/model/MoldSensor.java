package moldsim.model;

public class MoldSensor {

    private static final double THRESHOLD_MEDIUM   = 0.05;
    private static final double THRESHOLD_HIGH     = 0.15;
    private static final double THRESHOLD_CRITICAL = 0.30;

    private final Wall wall;

    public MoldSensor(Wall wall) {
        if (wall == null) throw new IllegalArgumentException("Wall cannot be null");
        this.wall = wall;
    }

    /** Taux de cellules infectées sur le mur (entre 0.0 et 1.0) */
    public double measure() {
        int total = 0;
        int infected = 0;
        for (Cell[] row : wall.getCells()) {
            for (Cell cell : row) {
                total++;
                if (cell.isInfected()) infected++;
            }
        }
        return total == 0 ? 0.0 : (double) infected / total;
    }

    /** Niveau d'alerte correspondant au taux actuel */
    public AlertLevel getAlertLevel() {
        double rate = measure();
        if (rate >= THRESHOLD_CRITICAL) return AlertLevel.CRITICAL;
        if (rate >= THRESHOLD_HIGH)     return AlertLevel.HIGH;
        if (rate >= THRESHOLD_MEDIUM)   return AlertLevel.MEDIUM;
        return AlertLevel.LOW;
    }

    /** Génère un SensorEvent si le niveau dépasse LOW */
    public SensorEvent poll(int currentWeek) {
        AlertLevel level = getAlertLevel();
        if (level == AlertLevel.LOW) return null;
        return new SensorEvent(currentWeek, level, measure(), wall);
    }

    public Wall getWall() { return wall; }
}