package moldsim.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import moldsim.model.Cell;
import moldsim.model.CellState;
import moldsim.model.Wall;

public class WallPreviewView extends Canvas {

    private final int previewColumns;
    private final double cellSize;

    public WallPreviewView(int rows, int previewColumns, double cellSize) {
        this.previewColumns = previewColumns;
        this.cellSize = cellSize;

        setWidth(previewColumns * cellSize);
        setHeight(rows * cellSize);
    }

    public void drawPreview(Wall wall, boolean showRightEdge) {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        if (wall == null) {
            return;
        }

        int height = wall.getHeight();
        int width = wall.getWidth();
        int columnsToDraw = Math.min(previewColumns, width);

        for (int row = 0; row < height; row++) {
            for (int localCol = 0; localCol < columnsToDraw; localCol++) {

                int realCol;

                if (showRightEdge) {
                    realCol = width - columnsToDraw + localCol;
                } else {
                    realCol = localCol;
                }

                Cell cell = wall.getCell(realCol, row);

                if (cell != null && cell.getState() == CellState.INFECTED) {
                    gc.setFill(Color.rgb(40, 130, 60));
                } else if (cell != null && cell.getState() == CellState.DEAD) {
                    gc.setFill(Color.rgb(70, 70, 70));
                } else {
                    gc.setFill(Color.rgb(190, 220, 225));
                }

                double x = localCol * cellSize;
                double y = row * cellSize;

                gc.fillRect(x, y, cellSize, cellSize);
                gc.setStroke(Color.rgb(50, 50, 50));
                gc.strokeRect(x, y, cellSize, cellSize);
            }
        }
    }
}