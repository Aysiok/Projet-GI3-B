package moldsim.view;

import javafx.scene.canvas.Canvas;

/**
 * Renders the 2D grid on a JavaFX Canvas.
 */
public class GridView extends Canvas {

    public GridView(int rows, int cols, double cellSize) {
        super(cols * cellSize, rows * cellSize);
    }
}