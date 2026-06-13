package moldsim.view;

import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.geometry.Pos;

/**
 * Main JavaFX view of the Mold Simulation application.
 * <p>
 * This class defines the full user interface layout including:
 * left environment controls, central grid visualization,
 * right event controls, and bottom simulation controls.
 * It acts as the primary UI container for all interactive components.
 */
public class MainView extends BorderPane {

    // ── Left Sidebar controls ──────────────────────────────────
    /** Slider controlling humidity level. */
    private Slider humiditySlider;
    /** Slider controlling temperature level. */
    private Slider temperatureSlider;
    /** Slider controlling ventilation level. */
    private Slider ventilationSlider;
    /** Dropdown for selecting wall material. */
    private ComboBox<String> materialComboBox;
    /** Dropdown for selecting mold species. */
    private ComboBox<String> speciesComboBox;
    /** List view displaying simulation alerts. */
    private ListView<String> alertLogView;

    // ── Right Sidebar controls ──────────────────────────────────

    /** Button triggering water leak event. */
    private Button waterLeakButton;
    /** Button triggering HVAC failure event. */
    private Button hvacFailureButton;
    /** Button triggering window open event. */
    private Button windowOpenedButton;
    /** Button applying treatment to wall. */
    private Button treatWallButton;
    /** Button applying treatment to shelf. */
    private Button treatShelfButton;
    /** Slider controlling event effect radius. */
    private Slider eventRadiusSlider;
    /** Button to activate mold drawing mode. */
    private Button addMoldButton;

    // ── Bottom controls ───────────────────────────────────────
    /** Button to start simulation. */
    private Button playButton;
    /** Button to pause simulation. */
    private Button pauseButton;
    /** Button to reset simulation. */
    private Button resetButton;
    /** Button to advance simulation by one step. */
    private Button stepButton;
    /** Button to export simulation report as PDF. */
    private Button exportPdfButton;
    /** Slider controlling simulation speed. */
    private Slider speedSlider;
    /** Button to go to previous simulation step. */
    private Button previousStepButton;
    /** Slider controlling simulation time navigation. */
    private Slider timeSlider;
    /** Button to create a new shelf. */
    private Button newShelfButton;

    /** Dropdown for selecting drawing tool mode. */
    private ComboBox<String> drawToolComboBox;

    /** Label displaying simulation status. */
    private Label statusLabel;
    /** Label displaying infected cell statistics. */
    private Label infectedLabel;
    /** Label displaying risk level. */
    private Label riskLabel;
    /** Label displaying current simulation week. */
    private Label weekLabel;

    // ── room-wall-display ───────────────────────────────────
    /** Label showing current room and wall context. */
    private Label currentLocationLabel;
    /** Input field for room name. */
    private TextField roomNameField;
    /** Input field for wall name. */
    private TextField wallNameField;
    /** Button to apply location changes. */
    private Button applyLocationButton;
    /** Button to save simulation state. */
    private Button saveButton;
    /** Button to load simulation state. */
    private Button loadButton;

    // ── GridView ─────────────────────────────────
    /** Main grid visualization component. */
    private GridView gridView;

    // ── Wall preview ─────────
    /** Preview of previous wall. */
    private WallPreviewView leftWallPreview;
    /** Preview of next wall. */
    private WallPreviewView rightWallPreview;

    /** Button to navigate to previous wall. */
    private Button previousWallButton;
    /** Button to navigate to next wall. */
    private Button nextWallButton;

    /** Label for previous wall name. */
    private Label leftWallNameLabel;
    /** Label for next wall name. */
    private Label rightWallNameLabel;
    /** Label for current wall name. */
    private Label currentWallNameLabel;


    /**
     * Builds the full layout of the application.
     */
    public MainView() {
        setTop(buildTopBar());
        setLeft(buildSidebar());
        setRight(buildEventSidebar());
        setCenter(buildGridArea());
        setBottom(buildBottomBar());
    }

    private HBox buildTopBar() {
        HBox topBar = new HBox(12);
        saveButton = new Button("💾 Save");
        saveButton.setStyle("-fx-background-color: #4A6FA5; -fx-text-fill: white;");
        loadButton = new Button("📂 Load");
        loadButton.setStyle("-fx-background-color: #4A6FA5; -fx-text-fill: white;");
        topBar.setPadding(new Insets(10, 16, 10, 16));
        topBar.setStyle("-fx-background-color: #2C2C2C;");

        Text title = new Text("ArchiveShield — Mold Risk Simulator");
        title.setStyle("-fx-fill: white; -fx-font-size: 16; -fx-font-weight: bold;");

        Label version = new Label("v1.0");
        version.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");

        currentLocationLabel = new Label("Current view: Archive Room A — North Wall");
        currentLocationLabel.setStyle("-fx-text-fill: #DDD; -fx-font-size: 12; -fx-font-weight: bold;");

        roomNameField = new TextField("Archive Room A");
        roomNameField.setPrefWidth(130);

        wallNameField = new TextField("North Wall");
        wallNameField.setPrefWidth(100);

        applyLocationButton = new Button("Apply");

        topBar.getChildren().addAll(title, version, currentLocationLabel, roomNameField, wallNameField, applyLocationButton, saveButton, loadButton);

        return topBar;
    }

    // ── Left Sidebar ───────────────────────────────────────────────
    private VBox buildSidebar() {
        VBox sidebar = new VBox(12);
        sidebar.setPadding(new Insets(14, 12, 14, 12));
        sidebar.setPrefWidth(200);
        sidebar.setStyle("-fx-background-color: #1E1E1E;");

        sidebar.getChildren().add(sectionLabel("Environment"));

        Label humLabel = new Label("Humidity: 50%");
        humLabel.setStyle("-fx-text-fill: #ccc; -fx-font-size: 11;");
        humiditySlider = new Slider(0, 100, 50);
        humiditySlider.valueProperty().addListener((obs, o, n) -> humLabel.setText(String.format("Humidity: %.0f%%", n.doubleValue())));

        Label tempLabel = new Label("Temperature: 20°C");
        tempLabel.setStyle("-fx-text-fill: #ccc; -fx-font-size: 11;");
        temperatureSlider = new Slider(0, 40, 20);
        temperatureSlider.valueProperty().addListener((obs, o, n) -> tempLabel.setText(String.format("Temperature: %.0f°C", n.doubleValue())));

        Label ventLabel = new Label("Ventilation: 50%");
        ventLabel.setStyle("-fx-text-fill: #ccc; -fx-font-size: 11;");
        ventilationSlider = new Slider(0, 100, 50);
        ventilationSlider.valueProperty().addListener((obs, o, n) -> ventLabel.setText(String.format("Ventilation: %.0f%%", n.doubleValue())));

        Label matLabel = new Label("Wall Material");
        matLabel.setStyle("-fx-text-fill: #ccc; -fx-font-size: 11;");
        materialComboBox = new ComboBox<>();
        materialComboBox.getItems().addAll("Plaster", "Concrete", "Wood", "Brick", "Document");
        materialComboBox.setValue("Plaster");
        materialComboBox.setMaxWidth(Double.MAX_VALUE);

        Label specLabel = new Label("Mold Species");
        specLabel.setStyle("-fx-text-fill: #ccc; -fx-font-size: 11;");
        speciesComboBox = new ComboBox<>();
        speciesComboBox.getItems().addAll("Cladosporium", "Aspergillus", "Stachybotrys");
        speciesComboBox.setValue("Cladosporium");
        speciesComboBox.setMaxWidth(Double.MAX_VALUE);

        sidebar.getChildren().addAll(humLabel, humiditySlider, tempLabel, temperatureSlider, ventLabel, ventilationSlider, matLabel, materialComboBox, specLabel, speciesComboBox);

        sidebar.getChildren().add(sectionLabel("Draw Tool"));
        drawToolComboBox = new ComboBox<>();
        drawToolComboBox.getItems().addAll("Point", "Brush", "Rectangle");
        drawToolComboBox.setValue("Point");
        drawToolComboBox.setMaxWidth(Double.MAX_VALUE);
        
        sidebar.getChildren().add(drawToolComboBox);

        sidebar.getChildren().add(sectionLabel("Statistics"));

        weekLabel       = statLabel("Time elapsed: 0 week(s)");
        infectedLabel   = statLabel("Infected: 0 (0.0%)");
        riskLabel       = statLabel("Risk: Low");

        sidebar.getChildren().addAll(weekLabel, infectedLabel, riskLabel);

        return sidebar;
    }

    // ── Grid area ─────────────────────────────────────────────
    private HBox buildGridArea() {
        HBox container = new HBox(10);
        container.setPadding(new Insets(10));
        container.setAlignment(Pos.TOP_CENTER);
        container.setStyle("-fx-background-color: #1A1A1A;");

        int rows = 50;
        int columns = 60;
        double cellSize = 11.0;

        leftWallPreview = new WallPreviewView(rows, 5, cellSize);
        rightWallPreview = new WallPreviewView(rows, 5, cellSize);

        gridView = new GridView(rows, columns, cellSize);

        previousWallButton = new Button("◀");
        nextWallButton = new Button("▶");

        leftWallNameLabel = new Label("Previous wall");
        leftWallNameLabel.setStyle("-fx-text-fill: #ccc; -fx-font-size: 11;");

        rightWallNameLabel = new Label("Next wall");
        rightWallNameLabel.setStyle("-fx-text-fill: #ccc; -fx-font-size: 11;");

        currentWallNameLabel = new Label("Current wall");
        currentWallNameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12; -fx-font-weight: bold;");

        HBox leftNav = new HBox(4);
        leftNav.setAlignment(Pos.CENTER);
        leftNav.getChildren().addAll(previousWallButton, leftWallNameLabel);

        HBox rightNav = new HBox(4);
        rightNav.setAlignment(Pos.CENTER);
        rightNav.getChildren().addAll(rightWallNameLabel, nextWallButton);

        VBox leftBox = new VBox(5);
        leftBox.setAlignment(Pos.TOP_CENTER);
        leftBox.getChildren().addAll(leftWallPreview, leftNav);

        VBox centerBox = new VBox(5);
        centerBox.setAlignment(Pos.TOP_CENTER);
        centerBox.getChildren().addAll(gridView, currentWallNameLabel);

        VBox rightBox = new VBox(5);
        rightBox.setAlignment(Pos.TOP_CENTER);
        rightBox.getChildren().addAll(rightWallPreview, rightNav);

        container.getChildren().addAll(leftBox, centerBox, rightBox);

        return container;
    }

    private VBox buildBottomBar() {
        VBox bottom = new VBox(6);
        bottom.setPadding(new Insets(8, 16, 8, 16));
        bottom.setStyle("-fx-background-color: #1E1E1E;");

        HBox buttons = new HBox(8);
        playButton      = new Button("▶ Play");
        pauseButton     = new Button("⏸ Pause");
        resetButton     = new Button("↺ Reset");
        stepButton      = new Button("⏭ Step");
        exportPdfButton = new Button("📄 Export PDF");
        newShelfButton = new Button("📦 New Shelf");
        newShelfButton.setStyle("-fx-background-color: #7A6248; -fx-text-fill: white;");
        previousStepButton = new Button("◀ Previous");
        Label timeLabel = new Label("Time:");
        timeLabel.setStyle("-fx-text-fill: #ccc; -fx-font-size: 11;");

        timeSlider = new Slider(0, 0, 0);
        timeSlider.setPrefWidth(180);
        timeSlider.setShowTickLabels(true);
        timeSlider.setShowTickMarks(true);
        timeSlider.setMajorTickUnit(1);
        timeSlider.setMinorTickCount(0);
        timeSlider.setSnapToTicks(true);

        playButton.setStyle("-fx-background-color: #1D9E75; -fx-text-fill: white;");
        pauseButton.setStyle("-fx-background-color: #BA7517; -fx-text-fill: white;");
        resetButton.setStyle("-fx-background-color: #555; -fx-text-fill: white;");
        stepButton.setStyle("-fx-background-color: #555; -fx-text-fill: white;");
        exportPdfButton.setStyle("-fx-background-color: #4A7C6F; -fx-text-fill: white;");

        Label speedLabel = new Label("Speed:");
        speedLabel.setStyle("-fx-text-fill: #ccc; -fx-font-size: 11;");
        speedSlider = new Slider(1, 10, 3);
        speedSlider.setPrefWidth(100);

        buttons.getChildren().addAll( 
            playButton, pauseButton, previousStepButton, stepButton, resetButton, 
            exportPdfButton, speedLabel, speedSlider, timeLabel, timeSlider, newShelfButton
        );

        statusLabel = new Label("Ready — place a contamination focus on the grid.");
        statusLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");

        bottom.getChildren().addAll(buttons, statusLabel);
        return bottom;
    }

    // ── Right Sidebar ───────────────────────────────────────────────

    private VBox buildEventSidebar() {
        VBox sidebar = new VBox(12);
        sidebar.setPadding(new Insets(14, 12, 14, 12));
        sidebar.setPrefWidth(180);
        sidebar.setStyle("-fx-background-color: #1E1E1E;");

        sidebar.getChildren().add(sectionLabel("External Events"));

        waterLeakButton = new Button("Water Leak");
        hvacFailureButton = new Button("HVAC Failure");
        windowOpenedButton = new Button("Open Window");

        waterLeakButton.setMaxWidth(Double.MAX_VALUE);
        hvacFailureButton.setMaxWidth(Double.MAX_VALUE);
        windowOpenedButton.setMaxWidth(Double.MAX_VALUE);

        waterLeakButton.setStyle("-fx-background-color: #1A5C8A; -fx-text-fill: white;");
        hvacFailureButton.setStyle("-fx-background-color: #8A1A1A; -fx-text-fill: white;");
        windowOpenedButton.setStyle("-fx-background-color: #1A7A4A; -fx-text-fill: white;");

        Label radiusLabel = new Label("Radius: 3");
        radiusLabel.setStyle("-fx-text-fill: #ccc; -fx-font-size: 11;");
        eventRadiusSlider = new Slider(1, 10, 3);
        eventRadiusSlider.setShowTickLabels(true);
        eventRadiusSlider.setMajorTickUnit(3);
        eventRadiusSlider.setSnapToTicks(true);
        eventRadiusSlider.setMaxWidth(Double.MAX_VALUE);
        eventRadiusSlider.valueProperty().addListener((obs, o, n) ->
            radiusLabel.setText(String.format("Radius: %.0f", n.doubleValue())));

        sidebar.getChildren().addAll(
            waterLeakButton, hvacFailureButton, windowOpenedButton);

        sidebar.getChildren().add(sectionLabel("Drawing Modes"));

        addMoldButton = new Button("Add Mold");
        addMoldButton.setMaxWidth(Double.MAX_VALUE);
        addMoldButton.setStyle("-fx-background-color: #3A7A3A; -fx-text-fill: white;");

        sidebar.getChildren().add(addMoldButton);
        sidebar.getChildren().add(sectionLabel("Treatments"));

        treatWallButton = new Button("Treat Wall Zone");
        treatShelfButton = new Button("Treat Shelf");
        treatWallButton.setMaxWidth(Double.MAX_VALUE);
        treatShelfButton.setMaxWidth(Double.MAX_VALUE);
        treatWallButton.setStyle("-fx-background-color: #5A3A7A; -fx-text-fill: white;");
        treatShelfButton.setStyle("-fx-background-color: #5A3A7A; -fx-text-fill: white;");

        sidebar.getChildren().addAll(treatWallButton, treatShelfButton);
        sidebar.getChildren().add(sectionLabel("Alert Log"));
        alertLogView = new ListView<>();
        alertLogView.setPrefHeight(200);
        alertLogView.setMaxWidth(Double.MAX_VALUE);
        alertLogView.setStyle("-fx-background-color: #111; -fx-control-inner-background: #111;");

        sidebar.getChildren().add(alertLogView);

        return sidebar;
    }


    // ── Helpers ───────────────────────────────────────────────
    private Label sectionLabel(String text) {
        Label lbl = new Label(text.toUpperCase());
        lbl.setStyle("-fx-text-fill: #555; -fx-font-size: 10; -fx-font-weight: bold; -fx-padding: 8 0 0 0;");
        return lbl;
    }

    private Label statLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #ccc; -fx-font-size: 12;");
        return lbl;
    }

    /**
     * Returns humidity slider.
     *
     * @return humidity slider
     */
    public Slider getHumiditySlider()      { return humiditySlider; }
    /**
     * Returns temperature slider.
     *
     * @return temperature slider
     */
    public Slider getTemperatureSlider()   { return temperatureSlider; }
    /**
     * Returns ventilation slider.
     *
     * @return ventilation slider
     */
    public Slider getVentilationSlider()   { return ventilationSlider; }
    /**
     * Returns simulation speed slider.
     *
     * @return speed slider
     */
    public Slider getSpeedSlider()         { return speedSlider; }
    /**
     * Returns material selection dropdown.
     *
     * @return material combo box
     */
    public ComboBox<String> getMaterialComboBox() { return materialComboBox; }
    /**
     * Returns species selection dropdown.
     *
     * @return species combo box
     */
    public ComboBox<String> getSpeciesComboBox()  { return speciesComboBox; }
    /**
     * Returns drawing tool selection dropdown.
     *
     * @return draw tool combo box
     */
    public ComboBox<String> getDrawToolComboBox() { return drawToolComboBox; } 
    /**
     * Returns play button.
     *
     * @return play button
     */
    public Button getPlayButton()          { return playButton; }
    /**
     * Returns pause button.
     *
     * @return pause button
     */
    public Button getPauseButton()         { return pauseButton; }
    /**
     * Returns reset button.
     *
     * @return reset button
     */
    public Button getResetButton()         { return resetButton; }
    /**
     * Returns step button.
     *
     * @return step button
     */
    public Button getStepButton()          { return stepButton; }
    /**
     * Returns export PDF button.
     *
     * @return export PDF button
     */
    public Button getExportPdfButton()     { return exportPdfButton; }
    /**
     * Returns status label.
     *
     * @return status label
     */
    public Label getStatusLabel()          { return statusLabel; }
    /**
     * Returns infected statistics label.
     *
     * @return infected label
     */
    public Label getInfectedLabel()        { return infectedLabel; }
    /**
     * Returns risk level label.
     *
     * @return risk label
     */
    public Label getRiskLabel()            { return riskLabel; }
    /**
     * Returns grid view component.
     *
     * @return grid view
     */
    public GridView getGridView()          { return gridView; }

    /**
     * Returns current location label.
     *
     * @return current location label
     */
    public Label getCurrentLocationLabel() {
        return currentLocationLabel;
    }

    /**
     * Returns room name input field.
     *
     * @return room name field
     */
    public TextField getRoomNameField() {
        return roomNameField;
    }

    /**
     * Returns wall name input field.
     *
     * @return wall name field
     */
    public TextField getWallNameField() {
        return wallNameField;
    }

    /**
     * Returns apply location button.
     *
     * @return apply location button
     */
    public Button getApplyLocationButton() {
        return applyLocationButton;
    }

    /**
     * Updates current location label text.
     *
     * @param locationText new location text
     */
    public void updateCurrentLocationLabel(String locationText) {
        currentLocationLabel.setText("Current view: " + locationText);
    }

    /**
     * Returns week label.
     *
     * @return week label
     */
    public Label getWeekLabel() {
        return weekLabel;
    }

    /**
     * Returns previous step button.
     *
     * @return previous step button
     */
    public Button getPreviousStepButton() {
        return previousStepButton;
    }

    /**
     * Returns time navigation slider.
     *
     * @return time slider
     */
    public Slider getTimeSlider() {
        return timeSlider;
    }

    /**
     * Returns new shelf button.
     *
     * @return new shelf button
     */
    public Button getNewShelfButton() { 
        return newShelfButton;
    }

    /**
     * Returns left wall preview component.
     *
     * @return left wall preview
     */
    public WallPreviewView getLeftWallPreview() {
        return leftWallPreview;
    }

    /**
     * Returns right wall preview component.
     *
     * @return right wall preview
     */
    public WallPreviewView getRightWallPreview() {
        return rightWallPreview;
    }

    /**
     * Returns previous wall navigation button.
     *
     * @return previous wall button
     */
    public Button getPreviousWallButton() {
        return previousWallButton;
    }

    /**
     * Returns next wall navigation button.
     *
     * @return next wall button
     */
    public Button getNextWallButton() {
        return nextWallButton;
    }

    /**
     * Updates wall navigation labels.
     *
     * @param previousWall previous wall name
     * @param currentWall current wall name
     * @param nextWall next wall name
     */
    public void updateWallNavigationLabels(String previousWall, String currentWall, String nextWall) {
        leftWallNameLabel.setText(previousWall);
        currentWallNameLabel.setText(currentWall);
        rightWallNameLabel.setText(nextWall);
    }

    /**
     * Returns save button.
     *
     * @return save button
     */
    public Button getSaveButton() { 
        return saveButton;
    }

    /**
     * Returns load button.
     *
     * @return load button
     */
    public Button getLoadButton() {
        return loadButton;
    }

    /**
     * Returns water leak event button.
     *
     * @return water leak button
     */
    public Button getWaterLeakButton() {
        return waterLeakButton; 
    }

    /**
     * Returns HVAC failure event button.
     *
     * @return HVAC failure button
     */
    public Button getHvacFailureButton() { 
        return hvacFailureButton; 
    }

    /**
     * Returns window opened event button.
     *
     * @return window opened button
     */
    public Button getWindowOpenedButton() { 
        return windowOpenedButton; 
    }

    /**
     * Returns treat wall button.
     *
     * @return treat wall button
     */
    public Button getTreatWallButton() {
        return treatWallButton; 
    }

    /**
     * Returns treat shelf button.
     *
     * @return treat shelf button
     */
    public Button getTreatShelfButton() {
        return treatShelfButton; 
    }

    /**
     * Returns event radius slider.
     *
     * @return event radius slider
     */
    public Slider getEventRadiusSlider(){
        return eventRadiusSlider; 
    }
    
    /**
     * Returns add mold button.
     *
     * @return add mold button
     */
    public Button getAddMoldButton() {
        return addMoldButton; 
    }

    /**
     * Returns alert log view.
     *
     * @return alert log view
     */
    public ListView<String> getAlertLogView() { 
        return alertLogView; 
    }

}
