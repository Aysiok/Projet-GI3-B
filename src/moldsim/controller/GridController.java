package moldsim.controller;

import moldsim.model.*;
import moldsim.view.GridView;
import moldsim.view.InteractionMode;
import moldsim.view.DrawMode;
import moldsim.view.MainView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.input.MouseButton;
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
    private Environment environment;
    private SimulationController simulation;
    private ExternalEvent pendingEvent = null;
    private InteractionMode previousMode = InteractionMode.NONE;
    private WallManager wallManager;
    private final PdfExportService pdfExportService = new PdfExportService();
    
    private boolean updatingTimeSlider;
    private boolean updatingControls;

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
        modelGrid = new Wall(gridView.getColumns(), gridView.getRows());
        environment = new Environment();
        environment.setHumidity(mainView.getHumiditySlider().getValue());
        environment.setTemperature(mainView.getTemperatureSlider().getValue());
        environment.setVentilation(mainView.getVentilationSlider().getValue());

        createDefaultWalls();
        loadCurrentWallIntoView();

        for (WallContext wc : wallManager.getWalls()) {
            wc.getSimulationController().getAlertController().setLogger(message -> mainView.getStatusLabel().setText(message));
        }

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

        mainView.getDrawToolComboBox().valueProperty().addListener((obs, oldVal, newVal) -> {
            switch (newVal) {
                case "Brush": gridView.setDrawMode(DrawMode.BRUSH); break;
                case "Rectangle": gridView.setDrawMode(DrawMode.RECTANGLE); break;
                default: gridView.setDrawMode(DrawMode.POINT); break;
            }
        });

        mainView.getSpeedSlider().valueProperty().addListener((obs, oldVal, newVal) -> {
            double v = newVal.doubleValue();
            if (Double.isNaN(v)) {
                mainView.getSpeedSlider().setValue(oldVal.doubleValue());
                return;
            }
            double speed = Math.max(1.0, Math.min(10.0, v));
            if (isRunning && simulationTimer != null) {
                double delay = Math.max(0.05, 1.1 - (speed / 10.0));
                simulationTimer.stop();
                simulationTimer.getKeyFrames().setAll(
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(delay), e -> step())
                );
                simulationTimer.play();
            }
        });

        markShelvesOnGrid();
        gridView.draw();

        saveCurrentSnapshot();
        updateTimeDisplay();

        mainView.getPlayButton().setOnAction(event -> play());
        mainView.getPauseButton().setOnAction(event -> pause());
        mainView.getStepButton().setOnAction(event -> step());
        mainView.getResetButton().setOnAction(event -> reset());
        mainView.getNewShelfButton().setOnAction(event -> openNewShelfDialog());
        mainView.getExportPdfButton().setOnAction(event -> exportPdf());
        mainView.getSaveButton().setOnAction(event -> saveSimulation());
        mainView.getLoadButton().setOnAction(event -> loadSimulation());

        mainView.getWaterLeakButton().setOnAction(e -> {
            previousMode = gridView.getInteractionMode();
            gridView.setInteractionMode(InteractionMode.NONE);
            updateModeButtons();
            pendingEvent = ExternalEvent.WATER_LEAK;
            mainView.getStatusLabel().setText("Click on the grid to place a water leak.");
        });

        mainView.getHvacFailureButton().setOnAction(e -> {
            simulation.getEventManager().apply(ExternalEvent.HVAC_FAILURE, modelGrid, 0, 0, 0);
            mainView.getVentilationSlider().setValue(environment.getVentilation());
            markCurrentStepAsModified("HVAC failure at week " + currentStepIndex + ". Future steps were cleared.");
        });

        mainView.getWindowOpenedButton().setOnAction(e -> {
            simulation.getEventManager().apply(ExternalEvent.WINDOW_OPENED, modelGrid, 0, 0, 0);
            mainView.getVentilationSlider().setValue(environment.getVentilation());
            markCurrentStepAsModified("Window opened at week " + currentStepIndex + ". Future steps were cleared.");
        });

        mainView.getTreatShelfButton().setOnAction(e -> {
            gridView.setInteractionMode(InteractionMode.NONE);
            gridView.setDrawMode(DrawMode.POINT);
            mainView.getDrawToolComboBox().setValue("Point");
            updateModeButtons();
            pendingEvent = ExternalEvent.ANTI_MOLD_TREATMENT_SHELF;
            mainView.getStatusLabel().setText("Click on a shelf to treat it.");
        });

        mainView.getAddMoldButton().setOnAction(e -> {
            if (gridView.getInteractionMode() == InteractionMode.ADD_MOLD) {
                gridView.setInteractionMode(InteractionMode.NONE);
            } else {
                gridView.setInteractionMode(InteractionMode.ADD_MOLD);
                mainView.getStatusLabel().setText("Draw mode: Mold — left click to paint, right click to erase.");
            }
            updateModeButtons();
        });

        mainView.getTreatWallButton().setOnAction(e -> {
            if (gridView.getInteractionMode() == InteractionMode.TREAT_WALL) {
                gridView.setInteractionMode(InteractionMode.NONE);
            } else {
                gridView.setInteractionMode(InteractionMode.TREAT_WALL);
                mainView.getStatusLabel().setText("Draw mode: Treatment — left click to treat, right click to erase.");
            }
            updateModeButtons();
        });

        gridView.setShelfPlacementListener(new GridView.ShelfPlacementListener() {
            @Override
            public boolean onShelfPlaced(int row, int col, int width, int height) {
                int forcedRow = gridView.getRows() - height;
                int clampedCol = Math.max(0, Math.min(col, gridView.getColumns() - width));

                for (Shelf existing : shelves) {
                    boolean overlapX = clampedCol < existing.getX() + existing.getWidth()
                                    && clampedCol + width > existing.getX();
                    boolean overlapY = forcedRow < existing.getY() + existing.getHeight()
                                    && forcedRow + height > existing.getY();
                    if (overlapX && overlapY) {
                        mainView.getStatusLabel().setText("Cannot place shelf here — overlaps with " + existing.getId() + ".");
                        return false;
                    }
                }
                String id = "S" + (shelves.size() + 1);
                int planks = Math.max(1, height / 5);
                Shelf shelf = new Shelf(id, clampedCol, forcedRow, width, height, planks, gridView.getNextShelfValue());
                shelves.add(shelf);
                simulation.updateShelves(shelves);
                markShelvesOnGrid();
                gridView.syncModelFromView();
                gridView.draw();
                markCurrentStepAsModified("Shelf " + id + " placed at week " + currentStepIndex + ". Future steps were cleared.");
                return true;
            }

            @Override
            public void onShelfRemoved(int row, int col) {
                shelves.removeIf(shelf ->
                    col >= shelf.getX() && col < shelf.getX() + shelf.getWidth() &&
                    row >= shelf.getY() && row < shelf.getY() + shelf.getHeight()
                );
                for (int r = 0; r < gridView.getRows(); r++)
                    for (int c = 0; c < gridView.getColumns(); c++) {
                        gridView.setCellType(r, c, GridView.TYPE_WALL);
                        gridView.setCellValue(r, c, null);
                    }
                simulation.updateShelves(shelves);
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
            if (updatingTimeSlider) return;
            int targetIndex = newValue.intValue();
            goToStep(targetIndex);
        });

        gridView.setCellClickListener((row, column, button) -> {
            boolean isErase = button == MouseButton.SECONDARY;
            InteractionMode mode = gridView.getInteractionMode();

            if (pendingEvent != null) {
                int radius = 3;
                switch (pendingEvent) {
                    case WATER_LEAK -> {
                        simulation.getEventManager().apply(ExternalEvent.WATER_LEAK, modelGrid, column, row, radius);
                        gridView.syncViewFromModel();
                        markCurrentStepAsModified("Water leak at week " + currentStepIndex + ". Future steps were cleared.");
                        updatingControls = true;
                        mainView.getHumiditySlider().setValue(environment.getHumidity());
                        updatingControls = false;
                    }
                    case ANTI_MOLD_TREATMENT_WALL -> {
                        simulation.getEventManager().apply(ExternalEvent.ANTI_MOLD_TREATMENT_WALL, modelGrid, column, row, radius);
                        gridView.syncViewFromModel();
                        markCurrentStepAsModified("Wall treated at week " + currentStepIndex + ". Future steps were cleared.");
                    }
                    case ANTI_MOLD_TREATMENT_SHELF -> {
                        Shelf target = shelves.stream().filter(s -> column >= s.getX() && column < s.getX() + s.getWidth() && row >= s.getY() && row < s.getY() + s.getHeight()).findFirst().orElse(null);
                        if (target != null) {
                            simulation.getEventManager().treatShelf(modelGrid, target);
                            for (int cy = target.getY(); cy < target.getY() + target.getHeight(); cy++) {
                                for (int cx = target.getX(); cx < target.getX() + target.getWidth(); cx++) {
                                    gridView.paintTreatment(cy, cx);
                                }
                            }
                            markCurrentStepAsModified("Shelf " + target.getId() + " treated at week " + currentStepIndex + ". Future steps were cleared.");
                        } else {
                            mainView.getStatusLabel().setText("No shelf at this location.");
                        }
                    }
                    default -> {}
                }
                pendingEvent = null;
                gridView.setInteractionMode(previousMode);
                updateModeButtons();
            } else {
                switch (mode) {
                    case ADD_MOLD -> {
                        if (isErase) {
                            gridView.eraseMold(row, column);
                        } else {
                            gridView.paintMold(row, column);
                        }
                        markCurrentStepAsModified("Cell modified at week " + currentStepIndex + "...");
                    }
                    case TREAT_WALL -> {
                        if (isErase) {
                            gridView.unpaintTreatment(row, column);
                        } else {
                            gridView.paintTreatment(row, column);
                        }
                        markCurrentStepAsModified("Treatment applied at week " + currentStepIndex + "...");
                    }
                    case TREAT_SHELF, PLACE_EVENT, NONE -> {}
                }
            }
        });

        updateStatistics();
        mainView.updateCurrentLocationLabel(locationContext.getDisplayName());
        mainView.getApplyLocationButton().setOnAction(event -> updateLocationFromInput()); 

        gridView.setInteractionCompleteListener(() -> {
            gridView.syncModelFromView();
            markCurrentStepAsModified("Draw action at week " + currentStepIndex + ".");
        });    
    }

    /**
     * Updates the grid view to display all shelves and their contents.
     */
    private void markShelvesOnGrid() {
    gridView.clearStructure();

    for (Shelf shelf : shelves) {
        int startX = shelf.getX();
        int startY = shelf.getY();
        int w      = shelf.getWidth();
        int h      = shelf.getHeight();
        int planks = shelf.getPlankCount();

        int[] plankRows = new int[planks];
        int interval = h / (planks + 1);
        int remainder = h % (planks + 1);

        for (int p = 0; p < planks; p++) {
            int extra = (p + 1) <= remainder ? (p + 1) : remainder;
            plankRows[p] = startY + (p + 1) * interval + extra;
        }

        // Ligne tout en haut de l'étagère
        for (int col = startX; col < startX + w; col++) {
            gridView.setCellType(startY, col, GridView.TYPE_DOCUMENT);
            gridView.setCellValue(startY, col, shelf.getValue());
            Cell cell = modelGrid.getCell(col, startY);
            if (cell != null) cell.setWallMaterial(WallMaterial.DOCUMENT);
        }

        // Planches = bois
        for (int p = 0; p < planks; p++) {
            int plankRow = plankRows[p];
            for (int col = startX; col < startX + w; col++) {
                gridView.setCellType(plankRow, col, GridView.TYPE_SHELF);
                Cell cell = modelGrid.getCell(col, plankRow);
                if (cell != null) cell.setWallMaterial(WallMaterial.WOOD);
            }
        }

        // Espaces entre planches = documents
        for (int p = 0; p < planks; p++) {
            int plankRow     = plankRows[p];
            int prevPlankRow = p == 0 ? startY : plankRows[p - 1];

            for (int row = prevPlankRow + 1; row < plankRow; row++) {
                for (int col = startX; col < startX + w; col++) {
                    gridView.setCellType(row, col, GridView.TYPE_DOCUMENT);
                    gridView.setCellValue(row, col, shelf.getValue());
                    Cell cell = modelGrid.getCell(col, row);
                    if (cell != null) cell.setWallMaterial(WallMaterial.DOCUMENT);
                }
            }
        }

        // Zone après la dernière planche jusqu'en bas
        int lastPlankRow = plankRows[planks - 1];
        for (int row = lastPlankRow + 1; row < startY + h; row++) {
            for (int col = startX; col < startX + w; col++) {
                gridView.setCellType(row, col, GridView.TYPE_DOCUMENT);
                gridView.setCellValue(row, col, shelf.getValue());
                Cell cell = modelGrid.getCell(col, row);
                if (cell != null) cell.setWallMaterial(WallMaterial.DOCUMENT);
            }
        }
    }
}
    /**
     * Starts automatic simulation execution.
     */
    private void play() {
        if (isRunning) return;
        isRunning = true;

        double speed = Math.max(1.0, Math.min(10.0, mainView.getSpeedSlider().getValue()));
        double delay = Math.max(0.05, 1.1 - (speed / 10.0));
        
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

    /**
     * Pauses automatic simulation execution.
     */
    private void pause() {
        if (!isRunning) return;
        isRunning = false;

        if (simulationTimer != null) {
            simulationTimer.stop();
        }

        mainView.getStatusLabel().setText("Simulation paused.");
    }

    /**
     * Advances the simulation by one step.
     */
    private void step() {
        if (currentStepIndex < history.size() - 1) goToStep(currentStepIndex + 1);
        else advanceOneNewStep();
    }

    /**
     * Moves back to the previous simulation step.
     */
    private void previousStep() {
        if (currentStepIndex <= 0) {
            mainView.getStatusLabel().setText("Already at initial step.");
            return;
        }
        goToStep(currentStepIndex - 1);
    }

    /**
     * Resets the simulation and all walls to their initial state.
     */
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

    /**
     * Updates infection statistics and risk indicators.
     */
    private void updateStatistics() {
        int infected = gridView.countInfectedCells();
        int total = gridView.getRows() * gridView.getColumns();
        double pct = total > 0 ? infected * 100.0 / total : 0.0;

        mainView.getInfectedLabel().setText(String.format("Infected: %d (%.1f%%)", infected, pct));

        if (pct < 10){
            mainView.getRiskLabel().setText("Risk: Low");
        }
        else if(pct < 30){
            mainView.getRiskLabel().setText("Risk: Moderate");
        }
        else {
            mainView.getRiskLabel().setText("Risk: High");
        }
    }
    /**
     * Updates the current location using user-provided values.
     */
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

    /**
     * Saves the current simulation state as a snapshot.
     */
    private void saveCurrentSnapshot() {
        SimulationSnapshot snapshot = createSnapshot(currentStepIndex);
        history.add(snapshot);
        currentStepIndex = history.size() - 1;
        updateTimeSlider();
    }

    /**
     * Updates the displayed simulation time.
     */
    private void updateTimeDisplay() {
        int week = currentStepIndex;
        mainView.getWeekLabel().setText("Time elapsed: " + week + " week(s)");
    }

    /**
     * Updates the appearance of interaction mode buttons.
     */
    private void updateModeButtons() {
        InteractionMode mode = gridView.getInteractionMode();
        String active   = "-fx-background-color: #FFD700; -fx-text-fill: black;";
        String moldBase = "-fx-background-color: #3A7A3A; -fx-text-fill: white;";
        String treatBase= "-fx-background-color: #5A3A7A; -fx-text-fill: white;";

        mainView.getAddMoldButton().setStyle(mode == InteractionMode.ADD_MOLD ? active : moldBase);
        mainView.getTreatWallButton().setStyle(mode == InteractionMode.TREAT_WALL ? active : treatBase);
    }

    /**
     * Restores a specific simulation step.
     *
     * @param targetIndex index of the step to restore
     */
    private void goToStep(int targetIndex) {
        if (targetIndex < 0 || targetIndex >= history.size()) return;
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

    /**
     * Marks the current step as modified and updates the history.
     *
     * @param message status message to display
     */
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

    /**
     * Replaces the snapshot associated with the current step.
     */
    private void replaceCurrentSnapshot() {
        SimulationSnapshot updatedSnapshot = createSnapshot(currentStepIndex);
        history.set(currentStepIndex, updatedSnapshot);
    }

    /**
     * Exports simulation results to a PDF document.
     */
    private void exportPdf() {
        if (history.isEmpty()) {
            mainView.getStatusLabel().setText("No simulation data to export.");
            return;
        }

        List<moldsim.model.Statistics> statsList = new ArrayList<>();
        List<List<String>> allLogs = new ArrayList<>();
        int previousInfected = 0;

        // Sauvegarder l'état courant
        int savedIndex = currentStepIndex;

        for (SimulationSnapshot snap : history) {
            // Restaurer chaque mur pour calculer les stats
            restoreAllWallsFromSnapshot(snap);

            moldsim.model.Statistics stats = new moldsim.model.Statistics(modelGrid, previousInfected);
            previousInfected = stats.getInfectedCells();
            statsList.add(stats);

            List<String> logs = new ArrayList<>(snap.getAlertLogs());
            allLogs.add(logs);
        }

        // Restaurer l'état courant
        goToStep(savedIndex);

        String filePath = "report_" + System.currentTimeMillis() + ".pdf";
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

    /**
     * Computes and stores the next simulation step.
     */
    private void advanceOneNewStep() {
        gridView.syncModelFromView();

        for (WallContext wallContext : wallManager.getWalls()) {
            wallContext.getSimulationController().step();
        }
        wallManager.getWalls().get(0).getSimulationController().depositSporesAcrossRoom(wallManager.getWalls());
        propagateBetweenAdjacentWalls();
        gridView.updateViewFromModel();

        int nextWeek = currentStepIndex + 1;
        history.add(createSnapshot(nextWeek));
        currentStepIndex = history.size() - 1;
        updateStatistics();
        int week = currentStepIndex;
        simulation.getAlertController().getHistory().stream()
        .filter(e -> e.getWeek() == week)
        .forEach(e -> {
            String msg = "Week " + e.getWeek() + " — [" + e.getType() + "] "
                + e.getAlertLevel()
                + (e.getShelf() != null
                    ? " — shelf " + e.getShelf().getId()
                    : String.format(" — rate %.0f%%", e.getMoldRate() * 100));
            javafx.application.Platform.runLater(() ->
                mainView.getAlertLogView().getItems().add(0, msg));

            // Recommandations
            if (simulation.getAlertController().getRecommendationEngine() != null) {
                simulation.getAlertController().getRecommendationEngine()
                    .analyze(e)
                    .forEach(r -> javafx.application.Platform.runLater(() ->
                        mainView.getAlertLogView().getItems().add(0, "  → " + r)));
            }
        });
        updateTimeDisplay();
        updateTimeSlider();
        updateWallNavigationView();

        mainView.getStatusLabel().setText("Advanced to week " + nextWeek + ".");
    }

    /**
     * Synchronizes the time slider with the current history state.
     */
    private void updateTimeSlider() {
        updatingTimeSlider = true;
        int maxIndex = Math.max(0, history.size() - 1);
        mainView.getTimeSlider().setMax(maxIndex);
        mainView.getTimeSlider().setValue(currentStepIndex);
        updatingTimeSlider = false;
    }

    /**
     * Opens the dialog used to create and place a new shelf.
     */
    private void openNewShelfDialog() {
        javafx.scene.control.Dialog<int[]> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("New Shelf");
        dialog.setHeaderText("Enter shelf dimensions (in cells)");

        javafx.scene.control.ButtonType okButton = new javafx.scene.control.ButtonType("Place", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
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
                ShelfValue chosenValue = switch (valueBox.getValue()) {
                    case "LOW"      -> ShelfValue.LOW;
                    case "HIGH"     -> ShelfValue.HIGH;
                    case "CRITICAL" -> ShelfValue.CRITICAL;
                    default         -> ShelfValue.MEDIUM;
                };
                gridView.enablePlacementMode(dims[0], dims[1]);
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

    /**
     * Handles modifications to simulation parameters.
     *
     * @param parameterName name of the modified parameter
     */
    private void markSimulationParametersChanged(String parameterName) {
        if (updatingControls) return;
        markCurrentStepAsModified(parameterName + " changed at week " + currentStepIndex + ". Future steps were cleared.");
    }

    /**
     * Creates a snapshot of the current simulation state.
     *
     * @param week simulation week associated with the snapshot
     * @return the created snapshot
     */
    private SimulationSnapshot createSnapshot(int week) {
        gridView.syncModelFromView();
        List<int[][]> wallStates = copyAllWallStates();

        List<String> logs = simulation.getAlertController().getHistory().stream()
            .filter(e -> e.getWeek() == week)
            .map(e -> "Week " + e.getWeek() + " — [" + e.getType() + "] "
                + e.getAlertLevel()
                + (e.getShelf() != null
                    ? " — shelf " + e.getShelf().getId()
                    : String.format(" — rate %.0f%%", e.getMoldRate() * 100)))
            .collect(java.util.stream.Collectors.toList());

        if (simulation.getAlertController().getRecommendationEngine() != null) {
            simulation.getAlertController().getHistory().stream()
                .filter(e -> e.getWeek() == week)
                .forEach(e -> simulation.getAlertController()
                    .getRecommendationEngine()
                    .analyze(e)
                    .forEach(r -> logs.add("  → " + r)));
        }

        return new SimulationSnapshot(
            week,
            wallStates,
            environment.getHumidity(),
            environment.getTemperature(),
            environment.getVentilation(),
            modelGrid.getMaterial(),
            logs
        );
    }

    /**
     * Restores environment settings from a snapshot.
     *
     * @param snapshot snapshot containing environment data
     */
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

    /**
     * Converts a wall material into its display label.
     *
     * @param material wall material
     * @return display label
     */
    private String toMaterialLabel(WallMaterial material) {
        switch (material) {
            case CONCRETE: return "Concrete";
            case WOOD:     return "Wood";
            case BRICK:    return "Brick";
            case DOCUMENT: return "Document";
            default:       return "Plaster";
        }
    }

    /**
     * Converts a display label into a wall material.
     *
     * @param label material label
     * @return corresponding wall material
     */
    private WallMaterial toWallMaterial(String label) {
        switch (label) {
            case "Concrete": return WallMaterial.CONCRETE;
            case "Wood":     return WallMaterial.WOOD;
            case "Brick":    return WallMaterial.BRICK;
            case "Document": return WallMaterial.DOCUMENT;
            default:         return WallMaterial.PLASTER;
        }
    }

    /**
     * Creates the default wall configuration.
     */
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

    /**
     * Adds a configured wall to the wall manager.
     *
     * @param name wall name
     * @param config wall configuration
     */
    private void addConfiguredWall(String name, WallConfigDialog.WallConfig config) {
        wallManager.addWall(new WallContext(
            name,
            config.width,
            config.height,
            config.material,
            environment
        ));
    }

    /**
     * Loads the currently selected wall into the user interface.
     */
    private void loadCurrentWallIntoView() {
        WallContext current = wallManager.getCurrentWallContext();

        modelGrid = current.getWall();
        shelves = current.getShelves();
        simulation = current.getSimulationController();

        gridView.resizeGrid(modelGrid.getHeight(), modelGrid.getWidth());
        gridView.setModelGrid(modelGrid);

        gridView.clearStructure();
        gridView.updateViewFromModel();

        markShelvesOnGrid();
        gridView.draw();

        updatingControls = true;
        mainView.getMaterialComboBox().setValue(toMaterialLabel(modelGrid.getMaterial()));
        updatingControls = false;

        refreshCurrentLocationDisplay();
    }

    /**
     * Updates wall navigation previews and labels.
     */
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

    /**
     * Switches to the previous wall.
     */
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

    /**
     * Switches to the next wall.
     */
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

    /**
     * Saves the current wall state before navigation.
     */
    private void saveCurrentWallBeforeSwitch() {
        gridView.syncModelFromView();
    }

    /**
     * Propagates contamination between adjacent walls.
     */
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

    /**
     * Propagates contamination from a wall's right edge to another wall's left edge.
     *
     * @param sourceContext source wall context
     * @param targetContext target wall context
     */
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

    /**
     * Propagates contamination from a wall's left edge to another wall's right edge.
     *
     * @param sourceContext source wall context
     * @param targetContext target wall context
     */
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

    /**
     * Copies the state of all walls.
     *
     * @return copied wall states
     */
    private List<int[][]> copyAllWallStates() {
        List<int[][]> allStates = new ArrayList<>();

        for (WallContext wallContext : wallManager.getWalls()) {
            allStates.add(copyWallState(wallContext.getWall()));
        }

        return allStates;
    }

    /**
     * Creates a copy of a wall state.
     *
     * @param wall wall to copy
     * @return copied state matrix
     */
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

    /**
     * Restores all walls from a snapshot.
     *
     * @param snapshot snapshot to restore
     */
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

    /**
     * Restores a wall state from saved data.
     *
     * @param wall wall to restore
     * @param savedState saved state matrix
     */
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

    /**
     * Resets every wall to a healthy state.
     */
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

    /**
     * Refreshes location information displayed in the interface.
     */
    private void refreshCurrentLocationDisplay() {
        WallContext current = wallManager.getCurrentWallContext();

        locationContext.setWallName(current.getName());

        mainView.getWallNameField().setText(current.getName());
        mainView.updateCurrentLocationLabel(locationContext.getDisplayName());

        updateWallNavigationView();
    }

    /**
     * Saves the current simulation to a file.
     */
    private void saveSimulation() {
        gridView.syncModelFromView();
        SimulationState state = new SimulationState(wallManager.getWalls(), environment, currentStepIndex,history);
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Save Simulation");
        chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Simulation files", "*.sim"));
        java.io.File file = chooser.showSaveDialog(mainView.getScene().getWindow());
        if (file != null) {
            BinaryExporter.save(state, file.getAbsolutePath());
            mainView.getStatusLabel().setText("Saved: " + file.getName());
        }
    }

    /**
     * Loads a simulation from a file.
     */
    private void loadSimulation() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Load Simulation");
        chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Simulation files", "*.sim"));
        java.io.File file = chooser.showOpenDialog(mainView.getScene().getWindow());
        if (file != null) {
            SimulationState state = BinaryExporter.load(file.getAbsolutePath());
            if (state != null) {
                environment.setHumidity(state.getHumidity());
                environment.setTemperature(state.getTemperature());
                environment.setVentilation(state.getVentilation());
                mainView.getHumiditySlider().setValue(state.getHumidity());
                mainView.getTemperatureSlider().setValue(state.getTemperature());
                mainView.getVentilationSlider().setValue(state.getVentilation());

                wallManager = new WallManager();
                for (WallContext wc : state.getWallContexts()) {
                    wc.rebuildController(environment);
                    wallManager.addWall(wc);
                }
                history.clear();
                history.addAll(state.getHistory());
                currentStepIndex = state.getStep();

                loadCurrentWallIntoView();
                updateTimeDisplay();
                updateTimeSlider();
                updateStatistics();
                mainView.getAlertLogView().getItems().clear();
                for (int i = history.size() - 1; i >= 0; i--) {
                    SimulationSnapshot snap = history.get(i);
                    if (snap.getAlertLogs() != null) {
                        for (String log : snap.getAlertLogs()) {
                            mainView.getAlertLogView().getItems().add(0, log);
                        }
                    }
                }
                mainView.getStatusLabel().setText("Loaded: " + file.getName());
            }
        }
    }
}
