package moldsim.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import moldsim.model.*;

/**
 * JavaFX Canvas-based grid renderer for the mold simulation.
 * <p>
 * Responsible for rendering the simulation grid, handling user input,
 * and synchronizing visual state with the model.
 */
public class GridView extends Canvas {

    // ── cell states ──────────────────────────────────────────────────────────
    /** Cell state: healthy (no infection). */
    private static final int HEALTHY = 0;
    /** Cell state: infected by mold. */
    private static final int INFECTED = 1;
    /** Cell state: dead (no activity). */
    private static final int DEAD = 2;
    /** Cell state: treated with anti-mold product. */
    private static final int TREATED = 3;
    /** Cell state: deposited spores present. */
    private static final int DEPOSITED_SPORE = 4;
    /** Cell state: actively sporulating mold. */
    private static final int SPORULATING = 5;

    // ── cell types (public so the controller can reference them) ─────────────
    /** Cell type: wall surface. */
    public static final int TYPE_WALL = 0;
    /** Cell type: shelf structure. */
    public static final int TYPE_SHELF = 1;
    /** Cell type: document-sensitive area. */
    public static final int TYPE_DOCUMENT = 2;
    

    // ── grid data ─────────────────────────────────────────────────────────────

    /** Number of rows in grid. */
    private int rows;
    /** Number of columns in grid. */
    private int columns;
    /** Size of each cell in pixels. */
    private final double cellSize;
    /** Grid storing cell states. */
    private int[][] cells;
    /** Grid storing cell types (wall, shelf, document). */
    private int[][] cellType;// 0=wall, 1=shelf, 2=document
    /** Grid storing shelf values for document cells. */
    private ShelfValue[][] cellValue;
    
    // ── model references ──────────────────────────────────────────────────────
    /** Reference to underlying simulation wall model. */
    private Wall modelGrid;

    // ── listeners ─────────────────────────────────────────────────────────────
    /** Listener for cell click events. */
    private CellClickListener cellClickListener;
    /** Listener for shelf placement events. */
    private ShelfPlacementListener shelfPlacementListener;

    // ── placement ghost ───────────────────────────────────────────────────────
    /** Indicates whether shelf placement mode is active. */
    private boolean placementMode = false;
    /** Row of placement preview. */
    private int ghostRow = -1;
    /** Column of placement preview. */
    private int ghostCol = -1;
    /** Width of preview shelf. */
    private int ghostWidth = 4;
    /** Height of preview shelf. */
    private int ghostHeight = 20;

    // ── misc ──────────────────────────────────────────────────────────────────
    /** Default shelf value for new shelves. */
    private ShelfValue nextShelfValue = ShelfValue.MEDIUM;
    /** Current drawing mode. */
    private DrawMode drawMode = DrawMode.POINT;
    /** Prevents immediate re-trigger after rectangle selection. */
    private boolean justFinishedRectangle = false;
    /** Current interaction mode. */
    private InteractionMode interactionMode = InteractionMode.NONE;
    /** Listener called when interaction completes. */
    private InteractionCompleteListener interactionCompleteListener;


    // ═════════════════════════════════════════════════════════════════════════
    //  Construction
    // ═════════════════════════════════════════════════════════════════════════

    /** Rectangle selection start row. */
    private int dragStartRow = -1;
    /** Rectangle selection start column. */
    private int dragStartCol = -1;
    /** Rectangle selection current row. */
    private int dragCurrentRow = -1;
    /** Rectangle selection current column. */
    private int dragCurrentCol = -1;
    /** True if rectangle selection active. */
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
                    boolean placed = shelfPlacementListener.onShelfPlaced(row, col, ghostWidth, ghostHeight);
                    if (placed) disablePlacementMode();

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

    /**
     * Sets the current drawing mode.
     *
     * @param mode drawing mode
     */
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

    /**
     * Enables shelf placement preview mode.
     *
     * @param width shelf width
     * @param height shelf height
     */
    public void enablePlacementMode(int width, int height) {
        this.placementMode = true;
        this.ghostWidth    = width;
        this.ghostHeight   = height;
    }

    /**
     * Disables shelf placement mode.
     */
    public void disablePlacementMode() {
        this.placementMode = false;
        this.ghostRow = -1;
        this.ghostCol = -1;
        draw();
    }

    /**
     * Applies the current interaction mode to a cell.
     *
     * @param row row index
     * @param col column index
     */
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

    /**
     * Sets the type of a cell.
     *
     * @param row row index
     * @param col column index
     * @param type cell type
     */
    public void setCellType(int row, int col, int type) {
        if (isInside(row, col)) {
            cellType[row][col] = type;
        }
    }

    /**
     * Sets the shelf value for a cell.
     *
     * @param row row index
     * @param col column index
     * @param value shelf value
     */
    public void setCellValue(int row, int col, ShelfValue value) {
        if (isInside(row, col)) {
            cellValue[row][col] = value;
        }
    }

    /**
     * Sets default shelf value for new shelves.
     *
     * @param value shelf value
     */
    public void setNextShelfValue(ShelfValue value) {
        this.nextShelfValue = value;
    }

    /**
     * Returns default shelf value for new shelves.
     *
     * @return shelf value
     */
    public ShelfValue getNextShelfValue() {
        return nextShelfValue;
    }

    /**
     * Sets listener for interaction completion.
     *
     * @param l listener
     */
    public void setInteractionCompleteListener(InteractionCompleteListener l) {
        this.interactionCompleteListener = l;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Listeners
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Sets listener for cell click events.
     *
     * @param listener listener
     */
    public void setCellClickListener(CellClickListener listener) {
        this.cellClickListener = listener;
    }

    /**
     * Sets listener for shelf placement events.
     *
     * @param listener listener
     */
    public void setShelfPlacementListener(ShelfPlacementListener listener) {
        this.shelfPlacementListener = listener;
    }

    /**
     * Sets the underlying simulation model grid.
     *
     * @param modelGrid model wall
     */
    public void setModelGrid(Wall modelGrid) {
        this.modelGrid = modelGrid;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Simulation
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Toggles infection state of a cell.
     *
     * @param row row index
     * @param column column index
     */
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

    /**
     * Resets grid to healthy state.
     */
    public void reset() {
        reset(false);
    }

    /**
     * Resets grid state and optionally clears shelf values.
     *
     * @param resetValues whether to clear shelf values
     */
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

    /**
     * Counts infected and sporulating cells.
     *
     * @return number of infected cells
     */
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

    /**
     * Renders the entire grid.
     */
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
            int clampedRow = rows - ghostHeight; // gravité

            int planks = Math.max(1, ghostHeight / 5);
            int interval = ghostHeight / (planks + 1);
            int remainder = ghostHeight % (planks + 1);
            // Fond de l'étagère transparent
            gc.setFill(Color.rgb(101, 67, 33, 0.2));
            gc.fillRect(clampedCol * cellSize, clampedRow * cellSize,
                        ghostWidth * cellSize, ghostHeight * cellSize);
            gc.setStroke(Color.rgb(101, 67, 33, 0.8));
            gc.setLineWidth(1.5);
            gc.strokeRect(clampedCol * cellSize, clampedRow * cellSize,
                        ghostWidth * cellSize, ghostHeight * cellSize);

            // Planches
            for (int p = 0; p < planks; p++) {
                int extra = (p + 1) <= remainder ? (p + 1) : remainder;
                int plankRow = clampedRow + (p + 1) * interval + extra;
                gc.setFill(Color.rgb(101, 67, 33, 0.6));
                gc.fillRect(clampedCol * cellSize, plankRow * cellSize,
                ghostWidth * cellSize, cellSize);
            }
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
            if (type == TYPE_DOCUMENT) {
                gc.setFill(Color.rgb(183, 183, 183));
            } else if (type == TYPE_SHELF){
                gc.setFill(Color.rgb(83, 82, 82));
            } else {
                gc.setFill(Color.rgb(133, 133, 133));
            }
        } else if (state == DEAD) {
            gc.setFill(Color.rgb(70, 70, 70));
        } else {
            if (type == TYPE_DOCUMENT) {
                ShelfValue val = cellValue[row][col];

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

    /**
     * Applies mold infection to a cell.
     *
     * @param row row index
     * @param col column index
     */
    public void paintMold(int row, int col) {
        if (!isInside(row, col)) return;
        cells[row][col] = INFECTED;
        draw();
    }

    /**
     * Applies treatment to a cell.
     *
     * @param row row index
     * @param col column index
     */
    public void paintTreatment(int row, int col) {
        if (!isInside(row, col)) return;
        int state = cells[row][col];
        if (state == INFECTED || state == SPORULATING || state == DEPOSITED_SPORE) {
            cells[row][col] = TREATED;
        }
        draw();
    }

    /**
     * Removes mold from a cell.
     *
     * @param row row index
     * @param col column index
     */
    public void eraseMold(int row, int col) {
        if (!isInside(row, col)) return;
        if (cells[row][col] == INFECTED) {
            cells[row][col] = HEALTHY;
        }
        draw();
    }

    /**
     * Reverts treatment on a cell.
     *
     * @param row row index
     * @param col column index
     */
    public void unpaintTreatment(int row, int col) {
        if (!isInside(row, col)) return;
        if (cells[row][col] == TREATED) {
            cells[row][col] = SPORULATING;
        }
        draw();
    }

    /**
     * Returns current interaction mode.
     *
     * @return interaction mode
     */
    public InteractionMode getInteractionMode() { return interactionMode; }
    /**
     * Sets interaction mode.
     *
     * @param mode interaction mode
     */
    public void setInteractionMode(InteractionMode mode) { this.interactionMode = mode; }

    public interface CellClickListener {
        void onCellClicked(int row, int column, MouseButton button);
    }

    /**
     * Creates a copy of the grid state.
     *
     * @return copied grid state
     */
    public int[][] copyGridState() {
        int[][] copy = new int[rows][columns];
        for (int r = 0; r < rows; r++)
            copy[r] = cells[r].clone();
        return copy;
    }

    /**
     * Restores a previously saved grid state.
     *
     * @param savedState saved grid
     */
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

    /**
     * Copies cell type grid.
     *
     * @return copied type grid
     */
    public int[][] copyCellTypes() {
        int[][] copy = new int[rows][columns];
        for (int r = 0; r < rows; r++)
            copy[r] = cellType[r].clone();
        return copy;
    }

    /**
     * Restores cell type grid.
     *
     * @param savedTypes saved types
     */
    public void restoreCellTypes(int[][] savedTypes) {
        if (savedTypes == null || savedTypes.length != rows || savedTypes[0].length != columns) {
            return;
        }
        for (int r = 0; r < rows; r++) {
            cellType[r] = savedTypes[r].clone();
        }
        draw();
    }

    /**
     * Copies shelf values grid.
     *
     * @return copied values grid
     */
    public ShelfValue[][] copyCellValues() {
        ShelfValue[][] copy = new ShelfValue[rows][columns];
        for (int r = 0; r < rows; r++)
            copy[r] = cellValue[r].clone();
        return copy;
    }

    /**
     * Restores shelf values grid.
     *
     * @param savedValues saved values
     */
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

    /**
     * Synchronizes model from view state.
     */
    public void syncModelFromView() {
        if (modelGrid == null) return;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                syncModelCellFromView(r, c);
            }
        }
    }

    /**
     * Synchronizes view from model state.
     */
    public void syncViewFromModel() {
        if (modelGrid == null) return;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                Cell cell = modelGrid.getCell(col, row);
                if (cell == null) continue;
                switch (cell.getState()) {
                    case INFECTED -> cells[row][col] = INFECTED;
                    case SPORULATING -> cells[row][col] = SPORULATING;
                    case DEPOSITED_SPORE -> cells[row][col] = DEPOSITED_SPORE;
                    case DEAD -> cells[row][col] = DEAD;
                    default -> {
                        if (cells[row][col] != TREATED){
                            cells[row][col] = HEALTHY;
                        }
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
            cell.setState(CellState.DEPOSITED_SPORE);
            cell.setSpecies(MoldSpecies.CLADOSPORIUM);
            cell.setMoldLevel(0.0);
            cell.setAge(0);

        } else if (state == SPORULATING) {
            cell.setState(CellState.SPORULATING);
            cell.setSpecies(MoldSpecies.CLADOSPORIUM);

            if (cell.getMoldLevel() < 1.0) {
                cell.setMoldLevel(1.0);
            }

        } else if (state == INFECTED) {
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
        boolean onShelfPlaced(int row, int col, int width, int height);
        void onShelfRemoved(int row, int col);
    }

    public interface InteractionCompleteListener {
        void onComplete();
    }

    /**
     * Updates full view from model.
     */
    public void updateViewFromModel() {
        if (modelGrid == null) {
            return;
        }
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                Cell cell = modelGrid.getCell(col, row);
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

    /**
     * Clears structural data (shelves and values).
     */
    public void clearStructure() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                cellType[row][col] = TYPE_WALL;
                cellValue[row][col] = null;
            }
        }
    }

    /**
     * Resizes the grid.
     *
     * @param newRows new row count
     * @param newColumns new column count
     */
    public void resizeGrid(int newRows, int newColumns) {
        this.rows = newRows;
        this.columns = newColumns;
        this.cells = new int[rows][columns];
        this.cellType = new int[rows][columns];
        this.cellValue = new ShelfValue[rows][columns];
        setWidth(columns * cellSize);
        setHeight(rows * cellSize);
        draw();
    }

}
