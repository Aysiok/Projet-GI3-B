package moldsim.controller;

import moldsim.model.*;
import moldsim.view.GridView;
import moldsim.view.MainView;

import java.util.ArrayList;
import java.util.List;
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

    public void initialize() {
        environment = new moldsim.model.Environment();
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
        int planks = Math.max(1, height / 5);
        Shelf shelf = new Shelf(id, col, row, width, height, planks, gridView.getNextShelfValue());
        shelves.add(shelf);
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
        // Remet les cases en mur
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
        mainView.getStatusLabel().setText("Simulation started.");
    }

    private void pause() {
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
        markShelvesOnGrid();
        gridView.draw();

        saveCurrentSnapshot();

        updateStatistics();
        updateTimeDisplay();
        updateTimeSlider();
        updateWallNavigationView();

        mainView.getStatusLabel().setText("Simulation reset.");
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
        String roomName = mainView.getRoomNameField().getText();
        String wallName = mainView.getWallNameField().getText();

        locationContext.setRoomName(roomName);
        locationContext.setWallName(wallName);

        mainView.updateCurrentLocationLabel(locationContext.getDisplayName());

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

    mainView.getGenerationLabel().setText("Step: " + currentStepIndex);
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

        gridView.restoreGridState(snapshot.getCellStates());
        restoreEnvironmentFromSnapshot(snapshot);

        updateStatistics();
        updateTimeDisplay();
        updateTimeSlider();

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
        moldsim.model.PdfExporter.export(statsList, environment, filePath);
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

        javafx.scene.control.TextField widthField  = new javafx.scene.control.TextField("4");
        javafx.scene.control.TextField heightField = new javafx.scene.control.TextField("20");
        javafx.scene.control.ComboBox<String> valueBox = new javafx.scene.control.ComboBox<>();
        valueBox.getItems().addAll("LOW", "MEDIUM", "HIGH", "CRITICAL");
        valueBox.setValue("MEDIUM");

        grid.add(new javafx.scene.control.Label("Width:"),  0, 0);
        grid.add(widthField,  1, 0);
        grid.add(new javafx.scene.control.Label("Height:"), 0, 1);
        grid.add(heightField, 1, 1);
        grid.add(new javafx.scene.control.Label("Value:"),  0, 2);
        grid.add(valueBox,    1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == okButton) {
                try {
                    int w = Integer.parseInt(widthField.getText().trim());
                    int h = Integer.parseInt(heightField.getText().trim());
                    // stocke la valeur choisie
                    return new int[]{w, h};
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
                    "Click on the grid to place the shelf (" + dims[0] + "x" + dims[1] + ")");
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
        int[][] gridState = gridView.copyGridState();
        return new SimulationSnapshot(week, gridState, environment.getHumidity(), environment.getTemperature(), environment.getVentilation(),modelGrid.getMaterial());
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
        int width = gridView.getColumns(); //à remplacer par la taille choisi lors du lancement 
        int height = gridView.getRows(); // à remplacer par la taille choisi lors du lancement

        WallMaterial defaultMaterial = toWallMaterial(mainView.getMaterialComboBox().getValue());

        wallManager.addWall(new WallContext(
            "North Wall",
            width,
            height,
            defaultMaterial,
            environment
        ));

        wallManager.addWall(new WallContext(
            "East Wall",
            width,
            height,
            defaultMaterial,
            environment
        ));

        wallManager.addWall(new WallContext(
            "South Wall",
            width,
            height,
            defaultMaterial,
            environment
        ));

        wallManager.addWall(new WallContext(
            "West Wall",
            width,
            height,
            defaultMaterial,
            environment
        ));
    }

    private void loadCurrentWallIntoView() {
        WallContext current = wallManager.getCurrentWallContext();

        modelGrid = current.getWall();
        shelves = current.getShelves();
        simulation = current.getSimulationController();

        gridView.setSimulation(simulation, modelGrid);

        gridView.clearStructure();
        gridView.updateViewFromModel();

        markShelvesOnGrid();
        gridView.draw();

        locationContext.setWallName(current.getName());
        mainView.updateCurrentLocationLabel(locationContext.getDisplayName());

        updateWallNavigationView();
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
        resetHistoryForCurrentWall();

        mainView.getStatusLabel().setText(
            "Moved to " + wallManager.getCurrentWallContext().getName() + "."
        );
    }

    private void moveToNextWall() {
        saveCurrentWallBeforeSwitch();

        wallManager.moveToNextWall();

        loadCurrentWallIntoView();
        resetHistoryForCurrentWall();

        mainView.getStatusLabel().setText(
            "Moved to " + wallManager.getCurrentWallContext().getName() + "."
        );
    }

   

    private void saveCurrentWallBeforeSwitch() {
        gridView.syncModelFromView();
    }

    private void resetHistoryForCurrentWall() {
        history.clear();
        currentStepIndex = 0;
        saveCurrentSnapshot();
        updateTimeDisplay();
        updateTimeSlider();
    }

    private void propagateBetweenAdjacentWalls() {
        List<WallContext> walls = wallManager.getWalls();

        for (int i = 0; i < walls.size(); i++) {
            WallContext current = walls.get(i);
            WallContext next = walls.get((i + 1) % walls.size());

            propagateRightEdgeToLeftEdge(current, next);
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

                double probability = 0.08;

                if (Math.random() < probability) {
                    targetCell.infect(sourceCell.getSpecies());
                }
            }
        }
    }
}