package moldsim.view;

/**
 * Defines the drawing modes available in the UI for modifying the simulation grid.
 * <p>
 * Each mode determines how user input is interpreted when interacting with cells.
 */
public enum DrawMode {
    /** Single-cell editing mode (click to modify one cell). */
    POINT,

    /** Freehand brush mode for continuous drawing over cells. */
    BRUSH,

    /** Rectangle selection mode for area-based modifications. */
    RECTANGLE
}