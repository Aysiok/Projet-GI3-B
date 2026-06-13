package moldsim.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import moldsim.controller.SimulationController;
import moldsim.model.*;

/**
 * Graphical 2D grid drawn with JavaFX Canvas.
 */
public class GridView extends Canvas {

    private static final int HEALTHY  = 0;
    private static final int INFECTED = 1;
    private static final int DEAD     = 2;
    private static final int TREATED = 3;

    private final int rows;
    private final int columns;
    private final double cellSize;
    private final int[][] cells;
    private final int[][] cellType; // 0=wall, 1=shelf, 2=document
    private SimulationController simulation;
    private Wall modelGrid;
    private CellClickListener cellClickListener;
    private boolean placementMode = false;
    private int ghostRow = -1;
    private int ghostCol = -1;
    private int ghostWidth  = 4;
    private int ghostHeight = 20;
    private ShelfPlacementListener shelfPlacementListener;
    private ShelfValue nextShelfValue = ShelfValue.MEDIUM;
    private final ShelfValue[][] cellValue;
    private InteractionMode interactionMode = InteractionMode.NONE;

    public static final int TYPE_WALL     = 0;
    public static final int TYPE_SHELF    = 1;
    public static final int TYPE_DOCUMENT = 2;

    public GridView(int rows, int columns, double cellSize) {
        this.rows      = rows;
        this.columns   = columns;
        this.cellSize  = cellSize;
        this.cells     = new int[rows][columns];
        this.cellType = new int[rows][columns]; // tout à 0 (wall) par défaut
        this.cellValue = new ShelfValue[rows][columns];

        setWidth(columns * cellSize);
        setHeight(rows * cellSize);

        setOnMouseClicked(event -> {
            int column = (int) (event.getX() / cellSize);
            int row    = (int) (event.getY() / cellSize);

            if (placementMode) {
                if (event.getButton() == MouseButton.PRIMARY && shelfPlacementListener != null) {
                    shelfPlacementListener.onShelfPlaced(row, column, ghostWidth, ghostHeight);
                    disablePlacementMode();
                }
                return;
            }
            if (event.getButton() == MouseButton.SECONDARY) {
                if (cellClickListener != null && isInside(row, column)) {
                    cellClickListener.onCellClicked(row, column, MouseButton.SECONDARY);
                }
                return;
            }
            if (event.getButton() == MouseButton.PRIMARY && cellClickListener != null && isInside(row, column)) {
                cellClickListener.onCellClicked(row, column, MouseButton.PRIMARY);
            }
        });

        setOnMouseMoved(event -> {
            if (placementMode) {
            ghostCol = (int) (event.getX() / cellSize);
            ghostRow = (int) (event.getY() / cellSize);
            draw();
            }
        });
         draw();
    }


    public void enablePlacementMode(int width, int height) {
        this.placementMode = true;
        this.ghostWidth    = width;
        this.ghostHeight   = height;
    }
    public void setNextShelfValue(ShelfValue value) {
    this.nextShelfValue = value;
}

    public ShelfValue getNextShelfValue() {
        return nextShelfValue;
    }

    public void disablePlacementMode() {
        this.placementMode = false;
        this.ghostRow = -1;
        this.ghostCol = -1;
    }

    public void setCellValue(int row, int col, ShelfValue value) {
        if (isInside(row, col)) cellValue[row][col] = value;
    }

    public void setShelfPlacementListener(ShelfPlacementListener listener) {
        this.shelfPlacementListener = listener;
    }
        public void setSimulation(SimulationController simulation, Wall modelGrid) {
        this.simulation = simulation;
        this.modelGrid  = modelGrid;
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
        if (!isInside(row, column)) {
            return;
        }

        if (cells[row][column] == HEALTHY) {
            cells[row][column] = INFECTED;
        } else if (cells[row][column] == INFECTED) {
            cells[row][column] = HEALTHY;
        }

        syncModelCellFromView(row, column);
        draw();
    }

    public void stepSimulation() {
        for (int row = 0; row < rows; row++){
            for (int col = 0; col < columns; col++){
                if (cells[row][col] == TREATED){
                    cells[row][col] = HEALTHY;
                }
            }
        }
        if (simulation != null) {
            simulation.step();
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < columns; col++) {
                    Cell cell = modelGrid.getCell(col, row);
                    if (cell == null) continue;
                    switch (cell.getState()) {
                        case INFECTED: cells[row][col] = INFECTED; break;
                        case DEAD:     cells[row][col] = DEAD;     break;
                        default:       cells[row][col] = HEALTHY;  break;
                    }
                }
            }
        }
        draw();
    }

    public void reset() {
        for (int row = 0; row < rows; row++)
            for (int column = 0; column < columns; column++)
                cells[row][column] = HEALTHY;
        syncModelFromView();
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

        gc.setFill(Color.rgb(240, 230, 210));
        gc.fillRect(0, 0, getWidth(), getHeight());

        for (int row = 0; row < rows; row++){
            for (int column = 0; column < columns; column++){
                drawCell(gc, row, column);
            }
        }
        if (placementMode && ghostRow >= 0 && ghostCol >= 0) {
            double gx = ghostCol * cellSize;
            double gy = ghostRow * cellSize;
            double gw = ghostWidth  * cellSize;
            double gh = ghostHeight * cellSize;
            gc.setFill(Color.rgb(122, 98, 72, 0.4));
            gc.fillRect(gx, gy, gw, gh);
            gc.setStroke(Color.rgb(122, 98, 72, 0.9));
            gc.setLineWidth(2);
            gc.strokeRect(gx, gy, gw, gh);
        }
    }

    private void drawCell(GraphicsContext gc, int row, int column) {
        double x = column * cellSize;
        double y = row * cellSize;
        int type  = cellType[row][column];
        int state = cells[row][column];

        if (state == INFECTED) {
            if (type == TYPE_DOCUMENT) {
                gc.setFill(Color.rgb(120, 206, 140));
            } else if (type == TYPE_SHELF) {
                gc.setFill(Color.rgb(20, 100, 30)); 
            } else {
                gc.setFill(Color.rgb(40, 130, 60)); 
            }
        } else if (state == DEAD) {
            gc.setFill(Color.rgb(70, 70, 70));
        } else if (state == TREATED) {
            gc.setFill(Color.rgb(180, 100, 220));
        } else {
            if (type == TYPE_DOCUMENT) {
                ShelfValue val = cellValue[row][column];
                if (val == null) {
                    gc.setFill(Color.rgb(255, 248, 220));
                } else {
                    gc.setFill(switch (val) {
                        case LOW      -> Color.rgb(200, 200, 180);
                        case MEDIUM   -> Color.rgb(220, 200, 140);
                        case HIGH     -> Color.rgb(240, 180, 80);
                        case CRITICAL -> Color.rgb(220, 80, 80);
                    });
                }
            } else if (type == TYPE_SHELF) {
                gc.setFill(Color.rgb(101, 67, 33)); 
            } else {
                gc.setFill(Color.rgb(105, 240, 255));
            }
        }
        gc.fillRect(x, y, cellSize, cellSize);
        gc.setStroke(Color.rgb(50, 50, 50));
        gc.setLineWidth(0.5);
        gc.strokeRect(x, y, cellSize, cellSize);
    }

    public void paintMold(int row, int col) {
        if (!isInside(row, col)) return;
        cells[row][col] = INFECTED;
        syncModelCellFromView(row, col);
        draw();
    }

    public void paintTreatment(int row, int col) {
        if (!isInside(row, col)) return;
        if (cells[row][col] == INFECTED) {
            cells[row][col] = TREATED;
            Cell cell = modelGrid.getCell(col, row);
            if (cell != null) cell.cure();
        }
        draw();
    }

    public void eraseMold(int row, int col) {
        if (!isInside(row, col)) return;
        if (cells[row][col] == INFECTED || cells[row][col] == TREATED) {
            cells[row][col] = HEALTHY;
            Cell cell = modelGrid.getCell(col, row);
            if (cell != null) cell.cure();
        }
        draw();
    }

    public InteractionMode getInteractionMode() { return interactionMode; }
    public void setInteractionMode(InteractionMode mode) { this.interactionMode = mode; }


    private boolean isInside(int row, int column) {
        return row >= 0 && row < rows && column >= 0 && column < columns;
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public interface CellClickListener {
        void onCellClicked(int row, int column, MouseButton button);
    }

    public int[][] copyGridState() {
        int[][] copy = new int[rows][columns];

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                copy[row][column] = cells[row][column];
            }
        }
        return copy;
    }

    public void restoreGridState(int[][] savedState) {
        if (savedState == null) {
            return;
        }
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                cells[row][col] = savedState[row][col];
            }
        }
        syncModelFromView();
        draw();
    }

    public void syncModelFromView() {
        if (modelGrid == null) {
            return;
        }
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                syncModelCellFromView(row, col);
            }
        }
    }

    private void syncModelCellFromView(int row, int col) {
        if (modelGrid == null) {
            return;
        }
        Cell cell = modelGrid.getCell(col, row);
        if (cell == null) {
            return;
        }
        int state = cells[row][col];
        int type = cellType[row][col];

        if (state == INFECTED) {
            cell.infect(MoldSpecies.CLADOSPORIUM);
        } else if (state == DEAD) {
            cell.kill();
        } else {
            cell.setState(CellState.HEALTHY);
            cell.setSpecies(null);
            cell.setMoldLevel(0.0);
            cell.setAge(0);
        }
        if (type == TYPE_SHELF) {
            cell.setWallMaterial(WallMaterial.WOOD);
        } else if (type == TYPE_DOCUMENT) {
            cell.setWallMaterial(WallMaterial.DOCUMENT);
        } else {
            cell.setWallMaterial(WallMaterial.PLASTER);
        }
    }

    public void syncViewFromModel() {
        if (modelGrid == null) return;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                Cell cell = modelGrid.getCell(col, row);
                if (cell == null) continue;
                switch (cell.getState()) {
                    case INFECTED -> cells[row][col] = INFECTED;
                    case DEAD -> cells[row][col] = DEAD;
                    default -> cells[row][col] = HEALTHY;
                }
            }
        }
        draw();
    }

    public interface ShelfPlacementListener {
        void onShelfPlaced(int row, int col, int width, int height);
        void onShelfRemoved(int row, int col);
}

public enum InteractionMode {
    NONE, ADD_MOLD, TREAT_WALL, TREAT_SHELF, PLACE_EVENT
}
}