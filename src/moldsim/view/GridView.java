package moldsim.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import moldsim.model.Document;
import moldsim.model.Shelf;

/**
 * Graphical 2D grid drawn with JavaFX Canvas.
 */
public class GridView extends Canvas {

    private static final int HEALTHY  = 0;
    private static final int INFECTED = 1;
    private static final int DEAD     = 2;

    private final int rows;
    private final int columns;
    private final double cellSize;
    private final int[][] cells;
    private final int[][] cellType; // 0=wall, 1=shelf, 2=document
    private final Random random;
    private List<Shelf> shelves = new ArrayList<>();
    private CellClickListener cellClickListener;
    public static final int TYPE_WALL     = 0;
    public static final int TYPE_SHELF    = 1;
    public static final int TYPE_DOCUMENT = 2;

    public GridView(int rows, int columns, double cellSize) {
        this.rows      = rows;
        this.columns   = columns;
        this.cellSize  = cellSize;
        this.cells     = new int[rows][columns];
        this.cellType = new int[rows][columns]; // tout à 0 (wall) par défaut
        this.random    = new Random();

        setWidth(columns * cellSize);
        setHeight(rows * cellSize);

        setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && cellClickListener != null) {
                int column = (int) (event.getX() / cellSize);
                int row    = (int) (event.getY() / cellSize);
                if (isInside(row, column)) {
                    cellClickListener.onCellClicked(row, column);
                }
            }
        });

        draw();
    }

    public void setShelves(List<Shelf> shelves) {
        this.shelves = shelves;
    }

    public void setCellType(int row, int column, int type) {
    if (isInside(row, column)) {
        cellType[row][column] = type;
    }
}


    public void setCellClickListener(CellClickListener listener) {
        this.cellClickListener = listener;
    }

    public void toggleInfection(int row, int column) {
        if (!isInside(row, column)) return;
        if (cells[row][column] == HEALTHY) {
            cells[row][column] = INFECTED;
        } else if (cells[row][column] == INFECTED) {
            cells[row][column] = HEALTHY;
        }
        draw();
    }

    public void stepSimulation() {
        int[][] nextCells = copyCells();
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                if (cells[row][column] == INFECTED) {
                    spreadToNeighbors(nextCells, row, column);
                }
            }
        }
        copyIntoCells(nextCells);
        draw();
    }

    private void spreadToNeighbors(int[][] nextCells, int row, int column) {
        for (int deltaRow = -1; deltaRow <= 1; deltaRow++) {
            for (int deltaColumn = -1; deltaColumn <= 1; deltaColumn++) {
                if (deltaRow == 0 && deltaColumn == 0) continue;

                int neighborRow    = row + deltaRow;
                int neighborColumn = column + deltaColumn;

                if (isInside(neighborRow, neighborColumn)
                        && cells[neighborRow][neighborColumn] == HEALTHY
                        && cellType[neighborRow][neighborColumn] != TYPE_SHELF) {

                    // Probabilité différente selon le type
                    double probability;
                    if (cellType[neighborRow][neighborColumn] == TYPE_DOCUMENT) {
                        probability = 0.30; // documents plus vulnérables
                    } else {
                        probability = 0.15; // mur normal
                    }

                    if (random.nextDouble() < probability) {
                        nextCells[neighborRow][neighborColumn] = INFECTED;
                    }
                }
            }
        }
    }
    public void reset() {
        for (int row = 0; row < rows; row++)
            for (int column = 0; column < columns; column++)
                cells[row][column] = HEALTHY;
        // on ne remet PAS cellType à zéro — la structure reste
        draw();
    }

    public int countInfectedCells() {
        int count = 0;
        for (int row = 0; row < rows; row++)
            for (int column = 0; column < columns; column++)
                if (cells[row][column] == INFECTED) count++;
        return count;
    }

    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        // Fond de la salle
        gc.setFill(Color.rgb(240, 230, 210));
        gc.fillRect(0, 0, getWidth(), getHeight());

        // Cellules
        for (int row = 0; row < rows; row++)
            for (int column = 0; column < columns; column++)
                drawCell(gc, row, column);
    }

    private void drawCell(GraphicsContext gc, int row, int column) {
        double x = column * cellSize;
        double y = row * cellSize;
        int type  = cellType[row][column];
        int state = cells[row][column];

        if (state == INFECTED) {
            if (type == TYPE_DOCUMENT) {
                gc.setFill(Color.rgb(120, 206, 140));   // vert vif sur document
            } else if (type == TYPE_SHELF) {
                gc.setFill(Color.rgb(20, 100, 30));   // vert foncé sur bois
            } else {
                gc.setFill(Color.rgb(40, 130, 60));   // vert normal sur mur
            }
        } else if (state == DEAD) {
            gc.setFill(Color.rgb(70, 70, 70));
        } else {
            // Sain — couleur selon le type
            if (type == TYPE_DOCUMENT) {
                gc.setFill(Color.rgb(255, 255, 255)); // jaune parchemin
            } else if (type == TYPE_SHELF) {
                gc.setFill(Color.rgb(101, 67, 33));   // marron bois
            } else {
                gc.setFill(Color.rgb(105, 240, 255)); // beige mur
            }
        }
    

        gc.fillRect(x, y, cellSize, cellSize);
        gc.setStroke(Color.rgb(50, 50, 50));
        gc.setLineWidth(0.5);
        gc.strokeRect(x, y, cellSize, cellSize);
    }


    private boolean isInside(int row, int column) {
        return row >= 0 && row < rows && column >= 0 && column < columns;
    }

    private int[][] copyCells() {
        int[][] copy = new int[rows][columns];
        for (int row = 0; row < rows; row++)
            for (int column = 0; column < columns; column++)
                copy[row][column] = cells[row][column];
        return copy;
    }

    private void copyIntoCells(int[][] source) {
        for (int row = 0; row < rows; row++)
            for (int column = 0; column < columns; column++)
                cells[row][column] = source[row][column];
    }

    public int getRows()    { return rows; }
    public int getColumns() { return columns; }

    public interface CellClickListener {
        void onCellClicked(int row, int column);
    }
}