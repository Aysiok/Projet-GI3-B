package moldsim.model;

/**
 * Utility class providing conversions between real-world measurements
 * and simulation grid units.
 * <p>
 * Defines the relationship between meters, centimeters, and grid cells
 * used by the simulation engine.
 */
public final class GridScale {

    /** Number of grid cells representing one meter. */
    public static final int CELLS_PER_METER = 20;
    /** Size of one cell in meters. */
    public static final double METERS_PER_CELL = 1.0 / CELLS_PER_METER;
    /** Size of one cell in centimeters. */
    public static final int CENTIMETERS_PER_CELL = 5;

    /**
     * Private constructor to prevent instantiation.
     */
    private GridScale() {
    }

    /**
     * Converts a length in meters to grid cells.
     *
     * @param meters length in meters
     * @return equivalent number of cells (minimum 1)
     */
    public static int metersToCells(double meters) {
        if (meters <= 0) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil(meters * CELLS_PER_METER));
    }

    /**
     * Converts a number of grid cells to meters.
     *
     * @param cells number of cells
     * @return equivalent length in meters
     */
    public static double cellsToMeters(int cells) {
        return cells * METERS_PER_CELL;
    }

    /**
     * Parses a string representing a length in meters.
     * Supports comma or dot decimal separators.
     *
     * @param text input string
     * @return parsed value in meters
     * @throws NumberFormatException if input is invalid
     */
    public static double parseMeters(String text) {
        if (text == null) {
            throw new NumberFormatException("Empty value");
        }
        return Double.parseDouble(text.trim().replace(",", "."));
    }

    /**
     * Returns a human-readable description of the grid scale.
     *
     * @return scale description string
     */
    public static String getScaleDescription() {
        return "1 cell = " + CENTIMETERS_PER_CELL + " cm × " + CENTIMETERS_PER_CELL + " cm";
    }
}