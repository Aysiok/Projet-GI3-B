package moldsim.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import moldsim.controller.SimulationController;
import moldsim.model.Wall;

public class GridView extends Canvas {

    public static final int HEALTHY  = 0;
    public static final int INFECTED = 1;
    public static final int DEAD     = 2;
    public static final int SPORE    = 3; // Nouvel état

    public enum DrawMode { POINT, BRUSH, RECTANGLE }
    private DrawMode drawMode = DrawMode.POINT;

    private final int rows;
    private final int columns;
    private final double cellSize;
    private final int[][] cells;
    private final int[][] cellType; 
    private SimulationController simulation;
    private Wall modelGrid;
    private CellClickListener cellClickListener;
    
    private boolean placementMode = false;
    private int ghostRow = -1;
    private int ghostCol = -1;
    private int ghostWidth  = 4;
    private int ghostHeight = 20;
    private ShelfPlacementListener shelfPlacementListener;
    private moldsim.model.ShelfValue nextShelfValue = moldsim.model.ShelfValue.MEDIUM;
    private final moldsim.model.ShelfValue[][] cellValue;
    
    public static final int TYPE_WALL     = 0;
    public static final int TYPE_SHELF    = 1;
    public static final int TYPE_DOCUMENT = 2;

    // Variables pour le dessin
    private int dragStartRow = -1;
    private int dragStartCol = -1;
    private int dragCurrentRow = -1;
    private int dragCurrentCol = -1;
    private boolean isDraggingRectangle = false;

    public GridView(int rows, int columns, double cellSize) {
        this.rows      = rows;
        this.columns   = columns;
        this.cellSize  = cellSize;
        this.cells     = new int[rows][columns];
        this.cellType  = new int[rows][columns]; 
        this.cellValue = new moldsim.model.ShelfValue[rows][columns];

        setWidth(columns * cellSize);
        setHeight(rows * cellSize);

        setOnMousePressed(event -> {
            int column = (int) (event.getX() / cellSize);
            int row    = (int) (event.getY() / cellSize);

            if (event.getButton() == MouseButton.PRIMARY) {
                if (placementMode) {
                    if (shelfPlacementListener != null) {
                        shelfPlacementListener.onShelfPlaced(row, column, ghostWidth, ghostHeight);
                    }
                    disablePlacementMode();
                } else if (drawMode == DrawMode.RECTANGLE) {
                    dragStartRow = row;
                    dragStartCol = column;
                    dragCurrentRow = row;
                    dragCurrentCol = column;
                    isDraggingRectangle = true;
                } else if (drawMode == DrawMode.BRUSH) {
                    setCellStateAndSync(row, column, INFECTED);
                    draw();
                } else {
                    if (cellClickListener != null && isInside(row, column)) {
                        cellClickListener.onCellClicked(row, column);
                        cellClickListener.onInteractionComplete();
                    }
                }
            } else if (event.getButton() == MouseButton.SECONDARY) {
                if (shelfPlacementListener != null) {
                    shelfPlacementListener.onShelfRemoved(row, column);
                }
            }
        });

        setOnMouseDragged(event -> {
            int column = (int) (event.getX() / cellSize);
            int row    = (int) (event.getY() / cellSize);

            if (!placementMode) {
                if (drawMode == DrawMode.BRUSH && isInside(row, column)) {
                    setCellStateAndSync(row, column, INFECTED);
                    draw();
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
                    if (cellClickListener != null) cellClickListener.onInteractionComplete();
                } else if (drawMode == DrawMode.RECTANGLE && isDraggingRectangle) {
                    int rMin = Math.min(dragStartRow, dragCurrentRow);
                    int rMax = Math.max(dragStartRow, dragCurrentRow);
                    int cMin = Math.min(dragStartCol, dragCurrentCol);
                    int cMax = Math.max(dragStartCol, dragCurrentCol);

                    for (int r = rMin; r <= rMax; r++) {
                        for (int c = cMin; c <= cMax; c++) {
                            setCellStateAndSync(r, c, INFECTED);
                        }
                    }
                    isDraggingRectangle = false;
                    if (cellClickListener != null) cellClickListener.onInteractionComplete();
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

    public void setNextShelfValue(moldsim.model.ShelfValue value) {
        this.nextShelfValue = value;
    }

    public moldsim.model.ShelfValue getNextShelfValue() {
        return nextShelfValue;
    }

    public void disablePlacementMode() {
        this.placementMode = false;
        this.ghostRow = -1;
        this.ghostCol = -1;
    }

    public void setCellValue(int row, int col, moldsim.model.ShelfValue value) {
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

    public void setCellThicknessListener(CellClickListener listener) {}

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

        syncModelCellFromView(row, column);
        draw();
    }

    public void stepSimulation() {
        if (simulation != null) {
            simulation.step();
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < columns; col++) {
                    moldsim.model.Cell cell = modelGrid.getCell(col, row);
                    if (cell == null) continue;
                    switch (cell.getState()) {
                        case INFECTED: cells[row][col] = INFECTED; break;
                        case SPORE:    cells[row][col] = SPORE;    break; 
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

        // Fond
        gc.setFill(Color.rgb(240, 230, 210));
        gc.fillRect(0, 0, getWidth(), getHeight());

        for (int row = 0; row < rows; row++){
            for (int column = 0; column < columns; column++){
                drawCell(gc, row, column);
            }
        }

        // Prévisualisation Etagère
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
        } else if (state == SPORE) {
            gc.setFill(Color.rgb(180, 160, 60)); 
        } else {
            if (type == TYPE_DOCUMENT) {
                moldsim.model.ShelfValue val = cellValue[row][column];
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

    private boolean isInside(int row, int column) {
        return row >= 0 && row < rows && column >= 0 && column < columns;
    }

    public int getRows()    { return rows; }
    public int getColumns() { return columns; }

    public interface CellClickListener {
        void onCellClicked(int row, int column);
        void onInteractionComplete(); 
    }

    public int[][] copyGridState() {
        int[][] copy = new int[rows][columns];
        for (int row = 0; row < rows; row++) {
            System.arraycopy(cells[row], 0, copy[row], 0, columns);
        }
        return copy;
    }

    public void restoreGridState(int[][] savedState) {
        if (savedState == null) return;
        for (int row = 0; row < rows; row++) {
            System.arraycopy(savedState[row], 0, cells[row], 0, columns);
        }
        syncModelFromView();
        draw();
    }

    public void syncModelFromView() {
        if (modelGrid == null) return;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                syncModelCellFromView(row, col);
            }
        }
    }

    private void syncModelCellFromView(int row, int col) {
        if (modelGrid == null) return;
        moldsim.model.Cell cell = modelGrid.getCell(col, row);
        if (cell == null) return;

        int state = cells[row][col];
        int type = cellType[row][col];

        if (state == INFECTED) {
            cell.infect(moldsim.model.MoldSpecies.CLADOSPORIUM);
        } else if (state == DEAD) {
            cell.kill();
        } else if (state == SPORE) {
            cell.setState(moldsim.model.CellState.SPORE);
        } else {
            cell.setState(moldsim.model.CellState.HEALTHY);
            cell.setSpecies(null);
            cell.setMoldLevel(0.0);
            cell.setAge(0);
        }

        if (type == TYPE_SHELF) {
            cell.setWallMaterial(moldsim.model.WallMaterial.WOOD);
        } else if (type == TYPE_DOCUMENT) {
            cell.setWallMaterial(moldsim.model.WallMaterial.DOCUMENT);
        } else {
            cell.setWallMaterial(moldsim.model.WallMaterial.PLASTER);
        }
    }

    public interface ShelfPlacementListener {
        void onShelfPlaced(int row, int col, int width, int height);
        void onShelfRemoved(int row, int col); 
    }
}
