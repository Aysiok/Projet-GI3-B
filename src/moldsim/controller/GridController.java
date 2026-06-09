package moldsim.controller;

import moldsim.model.DocumentValue;
import moldsim.model.Shelf;
import moldsim.view.GridView;
import moldsim.view.MainView;
import moldsim.model.LocationContext;
import moldsim.model.SimulationSnapshot;

import java.util.ArrayList;
import java.util.List;
/**
 * Controller for the grid interface.
 * Connects MainView controls to GridView actions.
 */
public class GridController {
    private final MainView mainView;
    private final GridView gridView;
    private final List<Shelf> shelves;
    private int generation;
    private LocationContext locationContext;
    private List<SimulationSnapshot> history;
    private int currentStepIndex;
    private moldsim.model.Grid modelGrid;
    private moldsim.model.Environment environment;
    private moldsim.controller.SimulationController simulation;

    
    private boolean updatingTimeSlider;

    public GridController(MainView mainView) {
        this.mainView = mainView;
        this.gridView = mainView.getGridView();
        this.shelves  = new ArrayList<>();
        this.generation = 0;
        this.locationContext = new LocationContext("Archive Room A", "North Wall");
        this.history = new ArrayList<>();
        this.currentStepIndex = 0;
        this.updatingTimeSlider = false;
    }

    public void initialize() {
        modelGrid   = new moldsim.model.Grid(gridView.getColumns(), gridView.getRows(), false, moldsim.model.NeighborhoodMode.EIGHT);
        environment = new moldsim.model.Environment();
        environment.setHumidity(mainView.getHumiditySlider().getValue());
        environment.setTemperature(mainView.getTemperatureSlider().getValue());
        environment.setVentilation(mainView.getVentilationSlider().getValue());
        simulation  = new moldsim.controller.SimulationController(modelGrid, environment);
        gridView.setSimulation(simulation, modelGrid);

        mainView.getHumiditySlider().valueProperty().addListener((obs, o, n) ->
            environment.setHumidity(n.doubleValue()));
        mainView.getTemperatureSlider().valueProperty().addListener((obs, o, n) ->
            environment.setTemperature(n.doubleValue()));
        mainView.getVentilationSlider().valueProperty().addListener((obs, o, n) ->
            environment.setVentilation(n.doubleValue()));
        mainView.getMaterialComboBox().valueProperty().addListener((obs, o, n) -> {
            moldsim.model.WallMaterial mat;
            switch (n) {
                case "Concrete":  mat = moldsim.model.WallMaterial.CONCRETE;  break;
                case "Wood":      mat = moldsim.model.WallMaterial.WOOD;       break;
                case "Brick":     mat = moldsim.model.WallMaterial.BRICK;      break;
                case "Document": mat = moldsim.model.WallMaterial.DOCUMENT;  break;
                default:          mat = moldsim.model.WallMaterial.PLASTER;    break;
            }
            environment.setMaterial(mat);
            for (int row = 0; row < modelGrid.getHeight(); row++)
                for (int col = 0; col < modelGrid.getWidth(); col++)
                    modelGrid.getCell(col, row).setWallMaterial(mat);
        });


        // Passe les étagères à la vue pour le rendu
        gridView.setShelves(shelves);
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
        gridView.setShelves(shelves);
        markShelvesOnGrid();
        gridView.draw();
        mainView.getStatusLabel().setText("Shelf " + id + " placed.");
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
        gridView.setShelves(shelves);
        markShelvesOnGrid();
        gridView.draw();
        mainView.getStatusLabel().setText("Shelf removed.");
    }
});
        
        mainView.getPreviousStepButton().setOnAction(event -> previousStep());

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
        for (Shelf shelf : shelves) {
            int startX = shelf.getX();
            int startY = shelf.getY();
            int w      = shelf.getWidth();
            int h      = shelf.getHeight();
            int planks = shelf.getPlankCount();
            double plankSpacing = (double) h / (planks + 1);
        
                    // Planches = bois
            for (int p = 0; p < planks; p++) {
                int plankRow = startY + (int) ((p + 1) * plankSpacing);
                for (int col = startX; col < startX + w; col++) {
                    gridView.setCellType(plankRow, col, GridView.TYPE_SHELF);
                    moldsim.model.Cell cell = modelGrid.getCell(col, plankRow);
                    if (cell != null) cell.setWallMaterial(moldsim.model.WallMaterial.WOOD);
                }
            }

            // Espaces entre planches = documents
            for (int p = 0; p < planks; p++) {
                int plankRow     = startY + (int) ((p + 1) * plankSpacing);
                int prevPlankRow = p == 0
                    ? startY
                    : startY + (int) (p * plankSpacing);

                for (int row = prevPlankRow + 1; row < plankRow; row++) {
                    for (int col = startX; col < startX + w; col++) {
                        gridView.setCellType(row, col, GridView.TYPE_DOCUMENT);
                        gridView.setCellValue(row, col, shelf.getValue());
                        moldsim.model.Cell cell = modelGrid.getCell(col, row);
                        if (cell != null) cell.setWallMaterial(moldsim.model.WallMaterial.DOCUMENT);
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
        generation = 0;
        currentStepIndex = 0;
        

        history.clear();

        gridView.reset();
        markShelvesOnGrid();
        gridView.draw();

        saveCurrentSnapshot();

        updateStatistics();
        updateTimeDisplay();
        updateTimeSlider();

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
        int step = currentStepIndex;
        int week = step;

        int[][] gridState = gridView.copyGridState();

        SimulationSnapshot snapshot = new SimulationSnapshot(step, week, gridState);

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
        int week = currentStepIndex;
        int[][] gridState = gridView.copyGridState();

        SimulationSnapshot updatedSnapshot =
            new SimulationSnapshot(currentStepIndex, week, gridState);

        history.set(currentStepIndex, updatedSnapshot);
    }

    private void exportPdf() {
        // TODO: implement after merge with CLI branch
        mainView.getStatusLabel().setText("PDF export coming soon.");
        }
    

    private void advanceOneNewStep() {
        gridView.syncModelFromView();
        gridView.stepSimulation();

        int nextStep = currentStepIndex + 1;
        int nextWeek = nextStep;

        int[][] gridState = gridView.copyGridState();

        history.add(new SimulationSnapshot(nextStep, nextWeek, gridState));
        currentStepIndex = history.size() - 1;

        updateStatistics();
        updateTimeDisplay();
        updateTimeSlider();

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
                DocumentValue chosenValue = switch (valueBox.getValue()) {
                    case "LOW"      -> DocumentValue.LOW;
                    case "HIGH"     -> DocumentValue.HIGH;
                    case "CRITICAL" -> DocumentValue.CRITICAL;
                    default         -> DocumentValue.MEDIUM;
                };
                gridView.enablePlacementMode(dims[0], dims[1]);
                // Stocke la valeur pour l'utiliser au placement
                gridView.setNextShelfValue(chosenValue);
                mainView.getStatusLabel().setText(
                    "Click on the grid to place the shelf (" + dims[0] + "x" + dims[1] + ")");
            }
        });
    }
}