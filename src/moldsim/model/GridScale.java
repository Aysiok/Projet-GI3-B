package moldsim.model;

public final class GridScale {

    public static final int CELLS_PER_METER = 20;
    public static final double METERS_PER_CELL = 1.0 / CELLS_PER_METER;
    public static final int CENTIMETERS_PER_CELL = 5;

    private GridScale() {
    }

    public static int metersToCells(double meters) {
        if (meters <= 0) {
            return 1;
        }

        return Math.max(1, (int) Math.ceil(meters * CELLS_PER_METER));
    }

    public static double cellsToMeters(int cells) {
        return cells * METERS_PER_CELL;
    }

    public static double parseMeters(String text) {
        if (text == null) {
            throw new NumberFormatException("Empty value");
        }

        return Double.parseDouble(text.trim().replace(",", "."));
    }

    public static String getScaleDescription() {
        return "1 cell = " + CENTIMETERS_PER_CELL + " cm × " + CENTIMETERS_PER_CELL + " cm";
    }
}