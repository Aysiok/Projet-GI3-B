package moldsim.controller;

import moldsim.model.*;
import moldsim.view.GridView;
import moldsim.view.MainView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import moldsim.view.WallConfigDialog;
/**
 * Controller for the grid interface.
 * Connects MainView controls to GridView actions.
 */


public class GridController {
    private final MainView mainView;
    private final GridView gridView;
    private List<Shelf> shelves;
    private LocationContext locationContext;
    private List<SimulationSnapshot> history;
    private int currentStepIndex;
    private Wall modelGrid;
    private WallManager wallManager;
    private moldsim.model.Environment environment;
    private moldsim.controller.SimulationController simulation;

    
    private boolean updatingTimeSlider;
    private boolean updatingControls;

    private static final double ROOM_EXTERNAL_SPORE_DEPOSITION = 0.00002;
    private static final double ROOM_INTERNAL_SPORE_DEPOSITION = 0.004;

    // Play/Pause timer
    private javafx.animation.Timeline simulationTimer;
    private boolean isRunning = false;
    private static final int SNAPSHOT_HEALTHY  = 0;
    private static final int SNAPSHOT_INFECTED = 1;
    private static final int SNAPSHOT_DEAD     = 2;
    private static final int SNAPSHOT_DEPOSITED_SPORE = 3;
    private static final int SNAPSHOT_SPORULATING = 4;
    private WallConfigDialog.WallConfig[] wallConfigs;

    public GridController(MainView mainView) {
        this.mainView = mainView;
        this.gridView = mainView.getGridView();
        this.shelves  = new ArrayList<>();
        this.wallManager = new WallManager();
        this.locationContext = new LocationContext("Archive Room A", "North Wall");
        this.history = new ArrayList<>();
        this.currentStepIndex = 0;
        this.updatingTimeSlider = false;
        this.updatingControls = false;
    }

    public GridController(MainView mainView, WallConfigDialog.WallConfig[] configs) {
        this(mainView);
        this.wallConfigs = configs;
    }

    public GridController(MainView mainView, Wall northWall) {
    this(mainView);
    this.modelGrid = northWall;
    }
    
    public void initialize() {
        if (modelGrid == null) {
            modelGrid = new Wall(gridView.getColumns(), gridView.getRows());
       }
        environment = new Environment();
        environment.setHumidity(mainView.getHumiditySlider().getValue());
        environment.setTemperature(mainView.getTemperatureSlider().getValue());
        environment.setVentilation(mainView.getVentilationSlider().getValue());

        createDefaultWalls();
        loadCurrentWallIntoView();

        mainView.getHumiditySlider().valueProperty().addListener((obs, oldValue, newValue) -> {
            environment.setHumidity(newValue.doubleValue());
            markSimulationParametersChanged("Humidity");
        });

        mainView.getTemperatureSlider().valueProperty().addListener((obs, oldValue, newValue) -> {
            environment.setTemperature(newValue.doubleValue());
            markSimulationParametersChanged("Temperature");
        });

        mainView.getVentilationSlider().valueProperty().addListener((obs, oldValue, newValue) -> {
            environment.setVentilation(newValue.doubleValue());
            markSimulationParametersChanged("Ventilation");
        });



        mainView.getMaterialComboBox().valueProperty().addListener((obs, oldValue, newValue) -> {
            WallMaterial mat = toWallMaterial(newValue);
            modelGrid.setMaterial(mat);
            if (!updatingControls) {
                gridView.syncModelFromView();
                markCurrentStepAsModified("Wall material changed at week " + currentStepIndex + ". Future steps were cleared.");
            }
        });

        // Passe les étagères à la vue pour le rendu
        markShelvesOnGrid();
        gridView.draw();

        saveCurrentSnapshot();
        updateTimeDisplay();

        // Boutons
        mainView.getPlayButton().setOnAction(event -> play());
        mainView.getPauseButton().setOnAction(event -> pause());
        mainView.getStepButton().setOnAction(event -> step());
        mainView.getResetButton().setOnAction(event -> reset());
        mainView.getNewShelfButton().setOnAction(event -> openNewShelfDialog());
        mainView.getExportPdfButton().setOnAction(event -> exportPdf());

        gridView.setShelfPlacementListener(new GridView.ShelfPlacementListener() {
        @Override
        public void onShelfPlaced(int row, int col, int width, int height) {
            String id = "S" + (shelves.size() + 1);
            int planks = Math.max(1, height / 10);
            Shelf shelf = new Shelf(id, col, row, width, height, planks, gridView.getNextShelfValue());
            shelves.add(shelf);
            simulation.updateShelves(shelves);
            markShelvesOnGrid();
            gridView.syncModelFromView();
            gridView.draw();

            markCurrentStepAsModified("Shelf " + id + " placed at week " + currentStepIndex + ". Future steps were cleared.");
        }

        @Override
        public void onShelfRemoved(int row, int col) {
            shelves.removeIf(shelf ->
                col >= shelf.getX() && col < shelf.getX() + shelf.getWidth() &&
                row >= shelf.getY() && row < shelf.getY() + shelf.getHeight()
            );
            simulation.updateShelves(shelves);
            for (int r = 0; r < gridView.getRows(); r++)
                for (int c = 0; c < gridView.getColumns(); c++) {
                    gridView.setCellType(r, c, GridView.TYPE_WALL);
                    gridView.setCellValue(r, c, null);
                }
            // Remarque les étagères restantes
            markShelvesOnGrid();
            gridView.syncModelFromView();
            gridView.draw();

            markCurrentStepAsModified("Shelf removed at week " + currentStepIndex + ". Future steps were cleared.");
        }
});
        
        mainView.getPreviousStepButton().setOnAction(event -> previousStep());
        mainView.getPreviousWallButton().setOnAction(event -> moveToPreviousWall());
        mainView.getNextWallButton().setOnAction(event -> moveToNextWall());

        mainView.getTimeSlider().valueProperty().addListener((obs, oldValue, newValue) -> {
            if (updatingTimeSlider) {
                return;
            }

            int targetIndex = newValue.intValue();
            goToStep(targetIndex);
        });

        gridView.setCellClickListener((row, column) -> {
            gridView.toggleInfection(row, column);

            markCurrentStepAsModified(
                "Cell modified at week " + currentStepIndex
                + ". Future steps were cleared."
            );
        });

        updateStatistics();
        mainView.updateCurrentLocationLabel(locationContext.getDisplayName());

        mainView.getApplyLocationButton().setOnAction(event -> updateLocationFromInput()); 
    }

    private void markShelvesOnGrid() {
        gridView.clearStructure();

        for (Shelf shelf : shelves) {
            int startX = shelf.getX();
            int startY = shelf.getY();
            int w      = shelf.getWidth();
            int h      = shelf.getHeight();
            int planks = shelf.getPlankCount();
            double plankSpacing = (double) h / (planks + 1);

            for (int p = 0; p < planks; p++) {
                int plankRow = startY + (int) ((p + 1) * plankSpacing);

                for (int col = startX; col < startX + w; col++) {
                    gridView.setCellType(plankRow, col, GridView.TYPE_SHELF);

                    Cell cell = modelGrid.getCell(col, plankRow);
                    if (cell != null) {
                        cell.setWallMaterial(WallMaterial.WOOD);
                    }
                }
            }

            for (int p = 0; p < planks; p++) {
                int plankRow = startY + (int) ((p + 1) * plankSpacing);
                int prevPlankRow = p == 0
                        ? startY
                        : startY + (int) (p * plankSpacing);

                for (int row = prevPlankRow + 1; row < plankRow; row++) {
                    for (int col = startX; col < startX + w; col++) {
                        gridView.setCellType(row, col, GridView.TYPE_DOCUMENT);
                        gridView.setCellValue(row, col, shelf.getValue());

                        Cell cell = modelGrid.getCell(col, row);
                        if (cell != null) {
                            cell.setWallMaterial(WallMaterial.DOCUMENT);
                        }
                    }
                }
            }
        }
    }

    private void play() {
    if (isRunning) return;
    isRunning = true;

    // Create a timer that calls step() automatically
    double delay = 1.1 - (mainView.getSpeedSlider().getValue() / 10.0);
    simulationTimer = new javafx.animation.Timeline(
        new javafx.animation.KeyFrame(
            javafx.util.Duration.seconds(delay),
            e -> step()
        )
    );
    simulationTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);
    simulationTimer.play();

    mainView.getStatusLabel().setText("Simulation running...");
}

    private void pause() {
    if (!isRunning) return;
    isRunning = false;

    if (simulationTimer != null) {
        simulationTimer.stop();
    }

    mainView.getStatusLabel().setText("Simulation paused.");
}

    private void step() {
        if (currentStepIndex < history.size() - 1) {
            goToStep(currentStepIndex + 1);
        } else {
            advanceOneNewStep();
        }
    }

    private void previousStep() {
        if (currentStepIndex <= 0) {
            mainView.getStatusLabel().setText("Already at initial step.");
            return;
        }

        goToStep(currentStepIndex - 1);
    }


    private void reset() {
        currentStepIndex = 0;
        history.clear();
        gridView.reset();

        resetAllWalls();

        gridView.updateViewFromModel();
        markShelvesOnGrid();
        gridView.draw();
        simulation.resetSensors();
        saveCurrentSnapshot();
        updateStatistics();
        updateTimeDisplay();
        updateTimeSlider();
        mainView.getStatusLabel().setText("Simulation reset.");
        updateWallNavigationView();

        mainView.getStatusLabel().setText("Simulation reset for all walls.");
    }

    private void updateStatistics() {
        int infected = gridView.countInfectedCells();
        int total    = gridView.getRows() * gridView.getColumns();
        double pct   = total > 0 ? infected * 100.0 / total : 0.0;

        mainView.getInfectedLabel().setText(
            String.format("Infected: %d (%.1f%%)", infected, pct));

        if (pct < 10) {
            mainView.getRiskLabel().setText("Risk: Low");
        } else if (pct < 30) {
            mainView.getRiskLabel().setText("Risk: Moderate");
        } else {
            mainView.getRiskLabel().setText("Risk: High");
        }
    }
    private void updateLocationFromInput() {
        String roomName = mainView.getRoomNameField().getText().trim();
        String wallName = mainView.getWallNameField().getText().trim();

        if (roomName.isEmpty()) {
            roomName = "Archive Room A";
        }

        if (wallName.isEmpty()) {
            wallName = wallManager.getCurrentWallContext().getName();
        }

        WallContext current = wallManager.getCurrentWallContext();

        locationContext.setRoomName(roomName);
        locationContext.setWallName(wallName);

        current.setName(wallName);

        refreshCurrentLocationDisplay();

        mainView.getStatusLabel().setText(
            "Current location changed to " + locationContext.getDisplayName() + "."
        );
    }

    private void saveCurrentSnapshot() {
        SimulationSnapshot snapshot = createSnapshot(currentStepIndex);

        history.add(snapshot);
        currentStepIndex = history.size() - 1;

        updateTimeSlider();
    }

    private void updateTimeDisplay() {
    int week = currentStepIndex;

    mainView.getWeekLabel().setText("Time elapsed: " + week + " week(s)");
    mainView.getStepLabel().setText(
        "History: " + currentStepIndex + " / " + (history.size() - 1)
    );
}

    private void goToStep(int targetIndex) {
        if (targetIndex < 0 || targetIndex >= history.size()) {
            return;
        }

        currentStepIndex = targetIndex;
        SimulationSnapshot snapshot = history.get(currentStepIndex);

        restoreAllWallsFromSnapshot(snapshot);
        restoreEnvironmentFromSnapshot(snapshot);
        simulation.resetSensors();

        gridView.updateViewFromModel();
        markShelvesOnGrid();
        gridView.draw();

        updateStatistics();
        updateTimeDisplay();
        updateTimeSlider();
        updateWallNavigationView();

        mainView.getStatusLabel().setText("Moved to week " + snapshot.getWeek() + ".");
    }

    private void markCurrentStepAsModified(String message) {
        replaceCurrentSnapshot();

        if (currentStepIndex < history.size() - 1) {
            history = new ArrayList<>(history.subList(0, currentStepIndex + 1));
        }

        updateStatistics();
        updateTimeDisplay();
        updateTimeSlider();

        mainView.getStatusLabel().setText(message);
    }

   private void replaceCurrentSnapshot() {
        SimulationSnapshot updatedSnapshot = createSnapshot(currentStepIndex);
        history.set(currentStepIndex, updatedSnapshot);
    }

    private void exportPdf() {
        moldsim.model.Statistics stats = new moldsim.model.Statistics(modelGrid, 0);
        java.util.List<moldsim.model.Statistics> statsList = new java.util.ArrayList<>();
        statsList.add(stats);

        String filePath = "report_" + System.currentTimeMillis() + ".pdf";
        java.util.List<java.util.List<String>> allLogs = new java.util.ArrayList<>();
        moldsim.model.PdfExporter.export(statsList, environment, allLogs, filePath);
        mainView.getStatusLabel().setText("PDF exported: " + filePath);

        try {
            java.io.File file = new java.io.File(filePath);
            if (file.exists() && java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(file);
            }
        } catch (java.io.IOException e) {
            mainView.getStatusLabel().setText("PDF exported but could not open: " + e.getMessage());
        }
    }

    private void advanceOneNewStep() {
        gridView.syncModelFromView();

        for (WallContext wallContext : wallManager.getWalls()) {
            wallContext.getSimulationController().step();
        }

        depositSporesAcrossRoom();

        propagateBetweenAdjacentWalls();

        gridView.updateViewFromModel();

        int nextWeek = currentStepIndex + 1;

        history.add(createSnapshot(nextWeek));
        currentStepIndex = history.size() - 1;

        updateStatistics();
        updateTimeDisplay();
        updateTimeSlider();
        updateWallNavigationView();

        mainView.getStatusLabel().setText("Advanced to week " + nextWeek + ".");
    }

    private void updateTimeSlider() {
        updatingTimeSlider = true;

        int maxIndex = Math.max(0, history.size() - 1);

        mainView.getTimeSlider().setMax(maxIndex);
        mainView.getTimeSlider().setValue(currentStepIndex);

        updatingTimeSlider = false;
    }
    private void openNewShelfDialog() {
        javafx.scene.control.Dialog<int[]> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("New Shelf");
        dialog.setHeaderText("Enter shelf dimensions (in cells)");

        javafx.scene.control.ButtonType okButton =
            new javafx.scene.control.ButtonType("Place", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButton, javafx.scene.control.ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        javafx.scene.control.TextField widthField  = new javafx.scene.control.TextField("1.0");
        javafx.scene.control.TextField heightField = new javafx.scene.control.TextField("2.0");
        javafx.scene.control.ComboBox<String> valueBox = new javafx.scene.control.ComboBox<>();
        valueBox.getItems().addAll("LOW", "MEDIUM", "HIGH", "CRITICAL");
        valueBox.setValue("MEDIUM");

        grid.add(new javafx.scene.control.Label("Width (m):"),  0, 0);
        grid.add(widthField,  1, 0);
        grid.add(new javafx.scene.control.Label("Height (m):"), 0, 1);
        grid.add(heightField, 1, 1);
        grid.add(new javafx.scene.control.Label("Value:"),  0, 2);
        grid.add(valueBox,    1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == okButton) {
                try {
                    double widthMeters = GridScale.parseMeters(widthField.getText());
                    double heightMeters = GridScale.parseMeters(heightField.getText());

                    int widthCells = GridScale.metersToCells(widthMeters);
                    int heightCells = GridScale.metersToCells(heightMeters);

                    return new int[]{widthCells, heightCells};

                } catch (NumberFormatException e) {
                    return null;
                }
            }

            return null;
        });

        dialog.showAndWait().ifPresent(dims -> {
            if (dims != null) {
                // Récupère la valeur patrimoniale choisie
                ShelfValue chosenValue = switch (valueBox.getValue()) {
                    case "LOW"      -> ShelfValue.LOW;
                    case "HIGH"     -> ShelfValue.HIGH;
                    case "CRITICAL" -> ShelfValue.CRITICAL;
                    default         -> ShelfValue.MEDIUM;
                };
                gridView.enablePlacementMode(dims[0], dims[1]);
                // Stocke la valeur pour l'utiliser au placement
                gridView.setNextShelfValue(chosenValue);
                mainView.getStatusLabel().setText(
                    "Click on the grid to place the shelf ("
                    + String.format("%.2f", GridScale.cellsToMeters(dims[0]))
                    + " m × "
                    + String.format("%.2f", GridScale.cellsToMeters(dims[1]))
                    + " m, "
                    + dims[0] + " × " + dims[1] + " cells)"
                );
            }
        });
    }

    private void markSimulationParametersChanged(String parameterName) {
        if (updatingControls) {
            return;
        }

        markCurrentStepAsModified(
            parameterName + " changed at week " + currentStepIndex + ". Future steps were cleared."
        );
    }

    private SimulationSnapshot createSnapshot(int week) {
        // Important : on sauvegarde d'abord les modifications visibles dans le modèle.
        gridView.syncModelFromView();

        List<int[][]> wallStates = copyAllWallStates();

        return new SimulationSnapshot(
            week,
            wallStates,
            environment.getHumidity(),
            environment.getTemperature(),
            environment.getVentilation(),
            modelGrid.getMaterial()
        );
    }

    private void restoreEnvironmentFromSnapshot(SimulationSnapshot snapshot) {
        updatingControls = true;

        mainView.getHumiditySlider().setValue(snapshot.getHumidity());
        mainView.getTemperatureSlider().setValue(snapshot.getTemperature());
        mainView.getVentilationSlider().setValue(snapshot.getVentilation());
        mainView.getMaterialComboBox().setValue(toMaterialLabel(snapshot.getMaterial()));

        updatingControls = false;

        environment.setHumidity(snapshot.getHumidity());
        environment.setTemperature(snapshot.getTemperature());
        environment.setVentilation(snapshot.getVentilation());
        modelGrid.setMaterial(snapshot.getMaterial());
    }

    private String toMaterialLabel(moldsim.model.WallMaterial material) {
        switch (material) {
            case CONCRETE: return "Concrete";
            case WOOD:     return "Wood";
            case BRICK:    return "Brick";
            case DOCUMENT: return "Document";
            default:       return "Plaster";
        }
    }

    private moldsim.model.WallMaterial toWallMaterial(String label) {
        switch (label) {
            case "Concrete": return moldsim.model.WallMaterial.CONCRETE;
            case "Wood":     return moldsim.model.WallMaterial.WOOD;
            case "Brick":    return moldsim.model.WallMaterial.BRICK;
            case "Document": return moldsim.model.WallMaterial.DOCUMENT;
            default:         return moldsim.model.WallMaterial.PLASTER;
        }
    }
    


    private void createDefaultWalls() {
        if (wallConfigs == null) {
            WallMaterial mat = toWallMaterial(mainView.getMaterialComboBox().getValue());
            wallConfigs = new WallConfigDialog.WallConfig[] {
                new WallConfigDialog.WallConfig(mat, gridView.getColumns(), gridView.getRows()),
                new WallConfigDialog.WallConfig(mat, gridView.getColumns(), gridView.getRows()),
                new WallConfigDialog.WallConfig(mat, gridView.getColumns(), gridView.getRows()),
                new WallConfigDialog.WallConfig(mat, gridView.getColumns(), gridView.getRows())
            };
        } 

        addConfiguredWall("North Wall", wallConfigs[0]);
        addConfiguredWall("East Wall",  wallConfigs[2]);
        addConfiguredWall("South Wall", wallConfigs[1]);
        addConfiguredWall("West Wall",  wallConfigs[3]);
    }

    private void addConfiguredWall(String name, WallConfigDialog.WallConfig config) {
        wallManager.addWall(new WallContext(
            name,
            config.width,
            config.height,
            config.material,
            environment
        ));
    }

    private void loadCurrentWallIntoView() {
        WallContext current = wallManager.getCurrentWallContext();

        modelGrid = current.getWall();
        shelves = current.getShelves();
        simulation = current.getSimulationController();

        gridView.resizeGrid(modelGrid.getHeight(), modelGrid.getWidth());
        gridView.setSimulation(simulation, modelGrid);

        gridView.clearStructure();
        gridView.updateViewFromModel();

        markShelvesOnGrid();
        gridView.draw();

        updatingControls = true;
        mainView.getMaterialComboBox().setValue(toMaterialLabel(modelGrid.getMaterial()));
        updatingControls = false;

        refreshCurrentLocationDisplay();
    }

    private void updateWallNavigationView() {
        WallContext previous = wallManager.getPreviousWallContext();
        WallContext current = wallManager.getCurrentWallContext();
        WallContext next = wallManager.getNextWallContext();

        mainView.getLeftWallPreview().drawPreview(previous.getWall(), true);
        mainView.getRightWallPreview().drawPreview(next.getWall(), false);

        mainView.updateWallNavigationLabels(
            previous.getName(),
            current.getName(),
            next.getName()
        );
    }

    private void moveToPreviousWall() {
        saveCurrentWallBeforeSwitch();

        wallManager.moveToPreviousWall();

        loadCurrentWallIntoView();

        updateStatistics();
        updateTimeDisplay();
        updateTimeSlider();

        mainView.getStatusLabel().setText(
            "Moved to " + wallManager.getCurrentWallContext().getName() + "."
        );
    }

    private void moveToNextWall() {
        saveCurrentWallBeforeSwitch();

        wallManager.moveToNextWall();

        loadCurrentWallIntoView();

        updateStatistics();
        updateTimeDisplay();
        updateTimeSlider();

        mainView.getStatusLabel().setText(
            "Moved to " + wallManager.getCurrentWallContext().getName() + "."
        );
    }

   

    private void saveCurrentWallBeforeSwitch() {
        gridView.syncModelFromView();
    }

    

    private void propagateBetweenAdjacentWalls() {
        List<WallContext> walls = wallManager.getWalls();

        for (int i = 0; i < walls.size(); i++) {
            WallContext current = walls.get(i);
            WallContext next = walls.get((i + 1) % walls.size());
            WallContext previous = walls.get((i - 1 + walls.size()) % walls.size());

            // Bord droit du mur courant vers bord gauche du mur suivant
            propagateRightEdgeToLeftEdge(current, next);

            // Bord gauche du mur courant vers bord droit du mur précédent
            propagateLeftEdgeToRightEdge(current, previous);
        }
    }

    private void propagateRightEdgeToLeftEdge(WallContext sourceContext, WallContext targetContext) {
        Wall sourceWall = sourceContext.getWall();
        Wall targetWall = targetContext.getWall();

        int sourceRightCol = sourceWall.getWidth() - 1;
        int targetLeftCol = 0;

        int commonHeight = Math.min(sourceWall.getHeight(), targetWall.getHeight());

        for (int row = 0; row < commonHeight; row++) {
            Cell sourceCell = sourceWall.getCell(sourceRightCol, row);
            Cell targetCell = targetWall.getCell(targetLeftCol, row);

            if (sourceCell != null
                    && targetCell != null
                    && sourceCell.isInfected()
                    && !targetCell.isInfected()
                    && targetCell.getState() == CellState.HEALTHY
                    && sourceCell.getSpecies() != null) {

                double probability = targetContext
                        .getSimulationController()
                        .computeInfectionProbability(
                            targetCell,
                            sourceCell.getSpecies(),
                            targetWall.getMaterial()
                        );

                if (Math.random() < probability) {
                    targetCell.infect(sourceCell.getSpecies());
                }
            }
        }
    }

    private void propagateLeftEdgeToRightEdge(WallContext sourceContext, WallContext targetContext) {
        Wall sourceWall = sourceContext.getWall();
        Wall targetWall = targetContext.getWall();

        int sourceLeftCol = 0;
        int targetRightCol = targetWall.getWidth() - 1;

        int commonHeight = Math.min(sourceWall.getHeight(), targetWall.getHeight());

        for (int row = 0; row < commonHeight; row++) {
            Cell sourceCell = sourceWall.getCell(sourceLeftCol, row);
            Cell targetCell = targetWall.getCell(targetRightCol, row);

            if (sourceCell != null
                    && targetCell != null
                    && sourceCell.isInfected()
                    && !targetCell.isInfected()
                    && targetCell.getState() == CellState.HEALTHY
                    && sourceCell.getSpecies() != null) {

                double probability = targetContext
                        .getSimulationController()
                        .computeInfectionProbability(
                            targetCell,
                            sourceCell.getSpecies(),
                            targetWall.getMaterial()
                        );

                if (Math.random() < probability) {
                    targetCell.infect(sourceCell.getSpecies());
                }
            }
        }
    }

    private List<int[][]> copyAllWallStates() {
        List<int[][]> allStates = new ArrayList<>();

        for (WallContext wallContext : wallManager.getWalls()) {
            allStates.add(copyWallState(wallContext.getWall()));
        }

        return allStates;
    }

    private int[][] copyWallState(Wall wall) {
        int height = wall.getHeight();
        int width = wall.getWidth();

        int[][] state = new int[height][width];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                Cell cell = wall.getCell(col, row);

                if (cell == null) {
                    state[row][col] = SNAPSHOT_HEALTHY;
                } else if (cell.getState() == CellState.DEPOSITED_SPORE) {
                    state[row][col] = SNAPSHOT_DEPOSITED_SPORE;
                } else if (cell.getState() == CellState.INFECTED) {
                    state[row][col] = SNAPSHOT_INFECTED;
                } else if (cell.getState() == CellState.SPORULATING) {
                    state[row][col] = SNAPSHOT_SPORULATING;
                } else if (cell.getState() == CellState.DEAD) {
                    state[row][col] = SNAPSHOT_DEAD;
                } else {
                    state[row][col] = SNAPSHOT_HEALTHY;
                }
            }
        }

        return state;
    }

    private void restoreAllWallsFromSnapshot(SimulationSnapshot snapshot) {
        List<int[][]> allStates = snapshot.getWallCellStates();
        List<WallContext> wallContexts = wallManager.getWalls();

        int limit = Math.min(allStates.size(), wallContexts.size());

        for (int i = 0; i < limit; i++) {
            Wall wall = wallContexts.get(i).getWall();
            int[][] savedState = allStates.get(i);

            restoreWallState(wall, savedState);
        }
    }

    private void restoreWallState(Wall wall, int[][] savedState) {
        if (wall == null || savedState == null) {
            return;
        }

        int height = Math.min(wall.getHeight(), savedState.length);

        for (int row = 0; row < height; row++) {
            int width = Math.min(wall.getWidth(), savedState[row].length);

            for (int col = 0; col < width; col++) {
                Cell cell = wall.getCell(col, row);

                if (cell == null) {
                    continue;
                }

                int state = savedState[row][col];

                if (state == SNAPSHOT_DEPOSITED_SPORE) {
                    cell.setState(CellState.DEPOSITED_SPORE);
                    cell.setSpecies(MoldSpecies.CLADOSPORIUM);
                    cell.setMoldLevel(0.0);
                    cell.setAge(0);

                } else if (state == SNAPSHOT_SPORULATING) {
                    cell.setState(CellState.SPORULATING);
                    cell.setSpecies(MoldSpecies.CLADOSPORIUM);

                } else if (state == SNAPSHOT_INFECTED) {
                    cell.setState(CellState.HEALTHY);
                    cell.setSpecies(null);
                    cell.setMoldLevel(0.0);
                    cell.setAge(0);
                    cell.infect(MoldSpecies.CLADOSPORIUM);

                } else if (state == SNAPSHOT_DEAD) {
                    cell.kill();

                } else {
                    cell.setState(CellState.HEALTHY);
                    cell.setSpecies(null);
                    cell.setMoldLevel(0.0);
                    cell.setAge(0);
                }
            }
        }
    }

    private void resetAllWalls() {
        for (WallContext wallContext : wallManager.getWalls()) {
            Wall wall = wallContext.getWall();

            for (int row = 0; row < wall.getHeight(); row++) {
                for (int col = 0; col < wall.getWidth(); col++) {
                    Cell cell = wall.getCell(col, row);

                    if (cell != null) {
                        cell.setState(CellState.HEALTHY);
                        cell.setSpecies(null);
                        cell.setMoldLevel(0.0);
                        cell.setAge(0);
                    }
                }
            }
        }
    }

    private void refreshCurrentLocationDisplay() {
        WallContext current = wallManager.getCurrentWallContext();

        locationContext.setWallName(current.getName());

        mainView.getWallNameField().setText(current.getName());
        mainView.updateCurrentLocationLabel(locationContext.getDisplayName());

        updateWallNavigationView();
    }

    //Spore methods for room

    private void depositSporesAcrossRoom() {
        MoldSpecies species = MoldSpecies.CLADOSPORIUM;

        int totalCells = 0;
        int sporulatingCount = 0;

        for (WallContext wallContext : wallManager.getWalls()) {
            Wall wall = wallContext.getWall();

            totalCells += wall.getWidth() * wall.getHeight();
            sporulatingCount += countCellsByState(wall, CellState.SPORULATING);
        }

        if (totalCells <= 0) {
            return;
        }

        double sporulatingRatio = (double) sporulatingCount / totalCells;
        double sporePressure = 1.0 - Math.exp(-8.0 * sporulatingRatio);

        double humidityFactor = computeHumiditySuitability(species);
        double temperatureFactor = computeTemperatureSuitability(species);
        double ventilationFactor = computeVentilationBlockingFactor();

        double environmentalSuitability =
                humidityFactor
                * temperatureFactor
                * ventilationFactor;

        double probability =
                environmentalSuitability
                * (
                    ROOM_EXTERNAL_SPORE_DEPOSITION
                    + ROOM_INTERNAL_SPORE_DEPOSITION * sporePressure
                );

        for (WallContext wallContext : wallManager.getWalls()) {
            depositSporesOnWall(wallContext.getWall(), species, probability);
        }
    }

    private void depositSporesOnWall(Wall wall, MoldSpecies species, double probability) {
        for (Cell[] row : wall.getGrid()) {
            for (Cell cell : row) {
                if (cell.getState() == CellState.HEALTHY) {
                    if (Math.random() < probability) {
                        cell.setState(CellState.DEPOSITED_SPORE);
                        cell.setSpecies(species);
                        cell.setMoldLevel(0.0);
                        cell.setAge(0);
                    }
                }
            }
        }
    }

    private int countCellsByState(Wall wall, CellState state) {
        int count = 0;

        for (Cell[] row : wall.getGrid()) {
            for (Cell cell : row) {
                if (cell.getState() == state) {
                    count++;
                }
            }
        }

        return count;
    }

    private double computeHumiditySuitability(MoldSpecies species) {
        double humidity = environment.getHumidity();

        if (humidity < species.getMinHumidity()) {
            return 0.0;
        }

        double value = (humidity - species.getMinHumidity()) / (100.0 - species.getMinHumidity());
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double computeTemperatureSuitability(MoldSpecies species) {
        double temperature = environment.getTemperature();

        if (temperature < species.getMinTemperature()
                || temperature > species.getMaxTemperature()) {
            return 0.0;
        }

        double tempMid = (species.getMinTemperature() + species.getMaxTemperature()) / 2.0;
        double tempRange = tempMid - species.getMinTemperature();

        if (tempRange <= 0.0) {
            return 0.0;
        }

        double value = 1.0 - Math.abs(temperature - tempMid) / tempRange;
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double computeVentilationBlockingFactor() {
        double ventilation = environment.getVentilation();

        double factor = 1.0 - ventilation / 100.0;

        return Math.max(0.0, Math.min(1.0, factor));
    }

}
