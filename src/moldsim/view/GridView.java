package moldsim.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import moldsim.controller.SimulationController;
import moldsim.model.*;
/**
 * Graphical 2D grid drawn with JavaFX Canvas.
 *
 * Responsibilities (view only):
 *  - render cells according to type/state
 *  - forward user interactions via listeners
 *  - expose save/restore helpers for the controller
 *
 * The controller owns the simulation loop and model synchronisation.
 */
public class GridView extends Canvas {

    // ── cell states ──────────────────────────────────────────────────────────
    private static final int HEALTHY = 0;
    private static final int INFECTED = 1;
    private static final int DEAD = 2;
    private static final int TREATED = 3;
    private static final int DEPOSITED_SPORE = 4;
    private static final int SPORULATING = 5;

    // ── cell types (public so the controller can reference them) ─────────────
    public static final int TYPE_WALL = 0;
    public static final int TYPE_SHELF = 1;
    public static final int TYPE_DOCUMENT = 2;
    

    // ── grid data ─────────────────────────────────────────────────────────────

    private int rows;
    private int columns;
    private final double cellSize;
    private int[][] cells;
    private int[][] cellType;// 0=wall, 1=shelf, 2=document
    private ShelfValue[][] cellValue;
    
    // ── model references ──────────────────────────────────────────────────────
    private SimulationController simulation;
    private Wall modelGrid;

    // ── listeners ─────────────────────────────────────────────────────────────
    private CellClickListener cellClickListener;
    private ShelfPlacementListener shelfPlacementListener;

    // ── placement ghost ───────────────────────────────────────────────────────
    private boolean placementMode = false;
    private int ghostRow = -1;
    private int ghostCol = -1;
    private int ghostWidth = 4;
    private int ghostHeight = 20;

    // ── misc ──────────────────────────────────────────────────────────────────
    private ShelfValue nextShelfValue = ShelfValue.MEDIUM;
    public enum DrawMode { POINT, BRUSH, RECTANGLE }
    private DrawMode drawMode = DrawMode.POINT;
    private boolean justFinishedRectangle = false;
    private InteractionMode interactionMode = InteractionMode.NONE;
    private InteractionCompleteListener interactionCompleteListener;


    // ═════════════════════════════════════════════════════════════════════════
    //  Construction
    // ═════════════════════════════════════════════════════════════════════════

    private int dragStartRow = -1;
    private int dragStartCol = -1;
    private int dragCurrentRow = -1;
    private int dragCurrentCol = -1;
    private boolean isDraggingRectangle = false;

    public GridView(int rows, int columns, double cellSize) {
        this.rows = rows;
        this.columns = columns;
        this.cellSize = cellSize;
        this.cells = new int[rows][columns];
        this.cellType = new int[rows][columns];
        this.cellValue = new ShelfValue[rows][columns];

        setWidth(columns * cellSize);
        setHeight(rows * cellSize);

        setOnMouseClicked(event -> {
            int col = (int) (event.getX() / cellSize);
            int row    = (int) (event.getY() / cellSize);
            if (justFinishedRectangle) {
                justFinishedRectangle = false;
                return;
            }
            if (placementMode) {
                if (event.getButton() == MouseButton.PRIMARY && shelfPlacementListener != null) {
                    shelfPlacementListener.onShelfPlaced(row, col, ghostWidth, ghostHeight);
                    disablePlacementMode();
                } else if (drawMode == DrawMode.BRUSH) {
                    setCellStateAndSync(row, col, INFECTED);
                    draw();
                } else {
                    if (cellClickListener != null && isInside(row, col)) {
                        cellClickListener.onCellClicked(row, col, event.getButton());
                    }
                }
            } else if (event.getButton() == MouseButton.SECONDARY) {
                if (isInside(row, col) && cellClickListener != null && interactionMode != InteractionMode.NONE) {
                    cellClickListener.onCellClicked(row, col, MouseButton.SECONDARY);
                } else if (shelfPlacementListener != null && isInside(row, col) && cellType[row][col] != TYPE_WALL) {
                    shelfPlacementListener.onShelfRemoved(row, col);
                }
            } else if (isInside(row, col) && cellClickListener != null) {
                cellClickListener.onCellClicked(row, col, event.getButton());
            }
        });

        setOnMouseDragged(event -> {
            int column = (int) (event.getX() / cellSize);
            int row    = (int) (event.getY() / cellSize);

            if (!placementMode) {
                if (drawMode == DrawMode.BRUSH && isInside(row, column)) {
                    if (cellClickListener != null) {
                        cellClickListener.onCellClicked(row, column, event.getButton());
                }
                } else if (drawMode == DrawMode.RECTANGLE && isDraggingRectangle) {
                    dragCurrentRow = Math.max(0, Math.min(rows - 1, row));
                    dragCurrentCol = Math.max(0, Math.min(columns - 1, column));
                    draw();
                }
            }
        });

        setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.PRIMARY && !placementMode) {
                if (drawMode == DrawMode.BRUSH) {
                    if (cellClickListener != null)
                        cellClickListener.onCellClicked(dragCurrentRow, dragCurrentCol, MouseButton.PRIMARY);
                } else if (drawMode == DrawMode.RECTANGLE && isDraggingRectangle) {
                    int rMin = Math.min(dragStartRow, dragCurrentRow);
                    int rMax = Math.max(dragStartRow, dragCurrentRow);
                    int cMin = Math.min(dragStartCol, dragCurrentCol);
                    int cMax = Math.max(dragStartCol, dragCurrentCol);
                    for (int r = rMin; r <= rMax; r++) {
                        for (int c = cMin; c <= cMax; c++) {
                            applyInteractionToCell(r, c);
                        }
                    }
                    isDraggingRectangle = false;
                    justFinishedRectangle = true;
                    if (interactionCompleteListener != null)
                        interactionCompleteListener.onComplete();
                    draw();
                }
            }
        });

        setOnMouseMoved(event -> {
            if (placementMode) {
                ghostCol = (int) (event.getX() / cellSize);
                ghostRow = (int) (event.getY() / cellSize);
                draw();
            }
        });

        setOnMousePressed(event -> {
            if (!placementMode && drawMode == DrawMode.RECTANGLE && event.getButton() == MouseButton.PRIMARY) {
                int col = (int) (event.getX() / cellSize);
                int row = (int) (event.getY() / cellSize);
                dragStartRow = row;
                dragStartCol = col;
                dragCurrentRow = row;
                dragCurrentCol = col;
                isDraggingRectangle = true;
            }
        });
        
        draw();
    }

    public void setDrawMode(DrawMode mode) {
        this.drawMode = mode;
        this.isDraggingRectangle = false;
    }

    private void setCellStateAndSync(int row, int col, int state) {
        if (isInside(row, col) && cells[row][col] != state) {
            cells[row][col] = state;
            syncModelCellFromView(row, col);
        }
    }

    public void enablePlacementMode(int width, int height) {
        this.placementMode = true;
        this.ghostWidth    = width;
        this.ghostHeight   = height;
    }

    public void disablePlacementMode() {
        this.placementMode = false;
        this.ghostRow = -1;
        this.ghostCol = -1;
        draw();
    }

    public void applyInteractionToCell(int row, int col) {
        switch (interactionMode) {
            case ADD_MOLD  -> paintMold(row, col);
            case TREAT_WALL -> paintTreatment(row, col);
            default -> {}
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Setters
    // ═════════════════════════════════════════════════════════════════════════

    public void setCellType(int row, int col, int type) {
        if (isInside(row, col)) {
            cellType[row][col] = type;
            draw();
        }
    }

    public void setCellValue(int row, int col, ShelfValue value) {
        if (isInside(row, col)) {
            cellValue[row][col] = value;
            draw();
        }
    }

    public void setNextShelfValue(ShelfValue value) {
        this.nextShelfValue = value;
    }

    public ShelfValue getNextShelfValue() {
        return nextShelfValue;
    }

    public void setInteractionCompleteListener(InteractionCompleteListener l) {
        this.interactionCompleteListener = l;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Listeners
    // ═════════════════════════════════════════════════════════════════════════

    public void setCellClickListener(CellClickListener listener) {
        this.cellClickListener = listener;
    }

    public void setShelfPlacementListener(ShelfPlacementListener listener) {
        this.shelfPlacementListener = listener;
    }

    public void setSimulation(SimulationController simulation, Wall modelGrid) {
        this.simulation = simulation;
        this.modelGrid  = modelGrid;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Simulation
    // ═════════════════════════════════════════════════════════════════════════

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
            updateViewFromModel();
        }
    }

    public void reset() {
        reset(false);
    }

    public void reset(boolean resetValues) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                cells[row][col] = HEALTHY;
                if (resetValues) cellValue[row][col] = null;
            }
        }
        syncModelFromView();
        draw();
    }

    public int countInfectedCells() {
        int count = 0;

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                if (cells[row][column] == INFECTED
                        || cells[row][column] == SPORULATING) {
                    count++;
                }
            }
        }

        return count;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Render
    // ═════════════════════════════════════════════════════════════════════════

    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        gc.setFill(Color.rgb(240, 230, 210));
        gc.fillRect(0, 0, getWidth(), getHeight());

        for (int row = 0; row < rows; row++){
            for (int col = 0; col < columns; col++){
                drawCell(gc, row, col);
            }
        }

        if (placementMode && ghostRow >= 0 && ghostCol >= 0) {
            int clampedCol = Math.max(0, Math.min(ghostCol, columns - ghostWidth));
            int clampedRow = Math.max(0, Math.min(ghostRow, rows - ghostHeight));
            double gx = clampedCol * cellSize;
            double gy = clampedRow * cellSize;
            double gw = ghostWidth * cellSize;
            double gh = ghostHeight * cellSize;
            gc.setFill(Color.rgb(122, 98, 72, 0.4));
            gc.fillRect(gx, gy, gw, gh);
            gc.setStroke(Color.rgb(122, 98, 72, 0.9));
            gc.setLineWidth(2);
            gc.strokeRect(gx, gy, gw, gh);
        }

        // Prévisualisation Rectangle de dessin
        if (drawMode == DrawMode.RECTANGLE && isDraggingRectangle) {
            int rMin = Math.min(dragStartRow, dragCurrentRow);
            int rMax = Math.max(dragStartRow, dragCurrentRow);
            int cMin = Math.min(dragStartCol, dragCurrentCol);
            int cMax = Math.max(dragStartCol, dragCurrentCol);

            double gx = cMin * cellSize;
            double gy = rMin * cellSize;
            double gw = (cMax - cMin + 1) * cellSize;
            double gh = (rMax - rMin + 1) * cellSize;

            gc.setFill(Color.rgb(120, 206, 140, 0.4));
            gc.fillRect(gx, gy, gw, gh);
            gc.setStroke(Color.rgb(40, 130, 60, 0.9));
            gc.setLineWidth(2);
            gc.strokeRect(gx, gy, gw, gh);
        }
    }

    private void drawCell(GraphicsContext gc, int row, int col) {
        double x = col * cellSize;
        double y = row * cellSize;
        int type = cellType[row][col];
        int state = cells[row][col];

        if (state == DEPOSITED_SPORE) {
            gc.setFill(Color.rgb(180, 160, 60));
        } else if (state == SPORULATING) {
            if (type == TYPE_DOCUMENT) {
                gc.setFill(Color.rgb(160, 230, 120));
            } else if (type == TYPE_SHELF) {
                gc.setFill(Color.rgb(60, 150, 50));
            } else {
                gc.setFill(Color.rgb(90, 180, 90));
            }
        } else if (state == INFECTED) {
            if (type == TYPE_DOCUMENT) {
                gc.setFill(Color.rgb(120, 206, 140));
            } else if (type == TYPE_SHELF) {
                gc.setFill(Color.rgb(20, 100, 30));
            } else {
                gc.setFill(Color.rgb(40, 130, 60));
            }
        } else if (state == TREATED) {
            gc.setFill(Color.rgb(133, 133, 133));
        } else if (state == DEAD) {
            gc.setFill(Color.rgb(70, 70, 70));
        } else {
            if (type == TYPE_DOCUMENT) {
                moldsim.model.ShelfValue val = cellValue[row][col];

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
                gc.setFill(Color.rgb(200, 190, 175));
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
        if (cells[row][col] == INFECTED) {
            cells[row][col] = HEALTHY;
            Cell cell = modelGrid.getCell(col, row);
            if (cell != null) cell.cure();
        }
        draw();
    }

    public void unpaintTreatment(int row, int col) {
        if (!isInside(row, col)) return;
        if (cells[row][col] == TREATED) {
            cells[row][col] = INFECTED;
            Cell cell = modelGrid.getCell(col, row);
            if (cell != null) cell.infect(MoldSpecies.CLADOSPORIUM);
        }
        draw();
    }

    public InteractionMode getInteractionMode() { return interactionMode; }
    public void setInteractionMode(InteractionMode mode) { this.interactionMode = mode; }

    public interface CellClickListener {
        void onCellClicked(int row, int column, MouseButton button);
    }

    public int[][] copyGridState() {
        int[][] copy = new int[rows][columns];
        for (int r = 0; r < rows; r++)
            copy[r] = cells[r].clone();
        return copy;
    }

    public void restoreGridState(int[][] savedState) {
        if (savedState == null || savedState.length != rows || savedState[0].length != columns) {
            return;
        }
        for (int r = 0; r < rows; r++){
            cells[r] = savedState[r].clone();
        }
        syncModelFromView();
        draw();
    }

    public int[][] copyCellTypes() {
        int[][] copy = new int[rows][columns];
        for (int r = 0; r < rows; r++)
            copy[r] = cellType[r].clone();
        return copy;
    }

    public void restoreCellTypes(int[][] savedTypes) {
        if (savedTypes == null || savedTypes.length != rows || savedTypes[0].length != columns) {
            return;
        }
        for (int r = 0; r < rows; r++) {
            cellType[r] = savedTypes[r].clone();
        }
        draw();
    }

    public ShelfValue[][] copyCellValues() {
        ShelfValue[][] copy = new ShelfValue[rows][columns];
        for (int r = 0; r < rows; r++)
            copy[r] = cellValue[r].clone();
        return copy;
    }

    public void restoreCellValues(ShelfValue[][] savedValues) {
        if (savedValues == null || savedValues.length != rows || savedValues[0].length != columns) {
            return;
        }
        for (int r = 0; r < rows; r++) {
            cellValue[r] = savedValues[r].clone();
        }
        draw();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Synchronization
    // ═════════════════════════════════════════════════════════════════════════

    public void syncModelFromView() {
        if (modelGrid == null) return;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                syncModelCellFromView(r, c);
            }
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
                    case DEAD     -> cells[row][col] = DEAD;
                    default -> {
                        if (cells[row][col] != TREATED) cells[row][col] = HEALTHY;
                    }
                }
            }
        }
        draw();
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

        // Synchronisation de l'état biologique
        if (state == DEPOSITED_SPORE) {
            cell.setState(moldsim.model.CellState.DEPOSITED_SPORE);
            cell.setSpecies(moldsim.model.MoldSpecies.CLADOSPORIUM);
            cell.setMoldLevel(0.0);
            cell.setAge(0);

        } else if (state == SPORULATING) {
            cell.setState(moldsim.model.CellState.SPORULATING);
            cell.setSpecies(moldsim.model.MoldSpecies.CLADOSPORIUM);

            if (cell.getMoldLevel() < 1.0) {
                cell.setMoldLevel(1.0);
            }

        } else if (state == INFECTED) {
            cell.infect(moldsim.model.MoldSpecies.CLADOSPORIUM);

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
            cell.setWallMaterial(modelGrid.getMaterial());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Accessors
    // ═════════════════════════════════════════════════════════════════════════

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    private boolean isInside(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < columns;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Interfaces
    // ═════════════════════════════════════════════════════════════════════════

    public interface ShelfPlacementListener {
        void onShelfPlaced(int row, int col, int width, int height);
        void onShelfRemoved(int row, int col); // ← ajoute ça
    }

    public enum InteractionMode {
        NONE, ADD_MOLD, TREAT_WALL, TREAT_SHELF, PLACE_EVENT
    }

    public interface InteractionCompleteListener {
        void onComplete();
    }

    public void updateViewFromModel() {
        if (modelGrid == null) {
            return;
        }
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                moldsim.model.Cell cell = modelGrid.getCell(col, row);

                if (cell == null) {
                    continue;
                }

                switch (cell.getState()) {
                    case DEPOSITED_SPORE:
                        cells[row][col] = DEPOSITED_SPORE;
                        break;

                    case INFECTED:
                        cells[row][col] = INFECTED;
                        break;

                    case SPORULATING:
                        cells[row][col] = SPORULATING;
                        break;

                    case DEAD:
                        cells[row][col] = DEAD;
                        break;

                    default:
                        cells[row][col] = HEALTHY;
                        break;
                }
            }
        }

        draw();
    }

    public void clearStructure() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                cellType[row][col] = TYPE_WALL;
                cellValue[row][col] = null;
            }
        }
    }

    public void resizeGrid(int newRows, int newColumns) {
        this.rows = newRows;
        this.columns = newColumns;

        this.cells = new int[rows][columns];
        this.cellType = new int[rows][columns];
        this.cellValue = new moldsim.model.ShelfValue[rows][columns];

        setWidth(columns * cellSize);
        setHeight(rows * cellSize);

        draw();
    }

}