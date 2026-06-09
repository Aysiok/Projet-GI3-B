package moldsim.view;

import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.*;
import javafx.scene.text.Text;

/**
 * Main view of the application.
 * Assembles the sidebar, the grid view and the bottom controls.
 * Follows the MVC pattern — no logic here, only layout.
 */
public class MainView extends BorderPane {

    // ── Sidebar controls ──────────────────────────────────────
    private Slider humiditySlider;
    private Slider temperatureSlider;
    private Slider ventilationSlider;
    private ComboBox<String> materialComboBox;
    private ComboBox<String> speciesComboBox;

    // ── Bottom controls ───────────────────────────────────────
    private Button playButton;
    private Button pauseButton;
    private Button resetButton;
    private Button stepButton;
    private Button exportPdfButton;
    private Slider speedSlider;
    private Button previousStepButton;
    private Slider timeSlider;
    private Button newShelfButton;
    

    // ── Status bar ────────────────────────────────────────────
    private Label statusLabel;
    private Label generationLabel;
    private Label infectedLabel;
    private Label riskLabel;
    private Label weekLabel;
    private Label stepLabel;

    // ── room-wall-display ────────────────────────────────────────────
    private Label currentLocationLabel;
    private TextField roomNameField;
    private TextField wallNameField;
    private Button applyLocationButton;

    // ── Grid placeholder (sera remplacé par GridView) ─────────
    private GridView gridView;

    /**
     * Builds the full layout of the application.
     */
    public MainView() {
        setTop(buildTopBar());
        setLeft(buildSidebar());
        setCenter(buildGridArea());
        setBottom(buildBottomBar());
    }

    // ── Top bar ───────────────────────────────────────────────
    private HBox buildTopBar() {
        HBox topBar = new HBox(12);
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

        topBar.getChildren().addAll(
            title,
            version,
            currentLocationLabel,
            roomNameField,
            wallNameField,
            applyLocationButton
        );

        return topBar;
}

    // ── Sidebar ───────────────────────────────────────────────
    private VBox buildSidebar() {
        VBox sidebar = new VBox(12);
        sidebar.setPadding(new Insets(14, 12, 14, 12));
        sidebar.setPrefWidth(200);
        sidebar.setStyle("-fx-background-color: #1E1E1E;");

        // Conditions
        sidebar.getChildren().add(sectionLabel("Environment"));

        Label humLabel = new Label("Humidity: 50%");
        humLabel.setStyle("-fx-text-fill: #ccc; -fx-font-size: 11;");
        humiditySlider = new Slider(0, 100, 50);
        humiditySlider.valueProperty().addListener((obs, o, n) ->
            humLabel.setText(String.format("Humidity: %.0f%%", n.doubleValue())));

        Label tempLabel = new Label("Temperature: 20°C");
        tempLabel.setStyle("-fx-text-fill: #ccc; -fx-font-size: 11;");
        temperatureSlider = new Slider(0, 40, 20);
        temperatureSlider.valueProperty().addListener((obs, o, n) ->
            tempLabel.setText(String.format("Temperature: %.0f°C", n.doubleValue())));

        Label ventLabel = new Label("Ventilation: 50%");
        ventLabel.setStyle("-fx-text-fill: #ccc; -fx-font-size: 11;");
        ventilationSlider = new Slider(0, 100, 50);
        ventilationSlider.valueProperty().addListener((obs, o, n) ->
            ventLabel.setText(String.format("Ventilation: %.0f%%", n.doubleValue())));

        Label matLabel = new Label("Wall Material");
        matLabel.setStyle("-fx-text-fill: #ccc; -fx-font-size: 11;");
        materialComboBox = new ComboBox<>();
        materialComboBox.getItems().addAll("Plaster", "Concrete", "Wood", "Brick", "Wallpaper");
        materialComboBox.setValue("Plaster");
        materialComboBox.setMaxWidth(Double.MAX_VALUE);

        Label specLabel = new Label("Mold Species");
        specLabel.setStyle("-fx-text-fill: #ccc; -fx-font-size: 11;");
        speciesComboBox = new ComboBox<>();
        speciesComboBox.getItems().addAll("Cladosporium", "Aspergillus", "Stachybotrys");
        speciesComboBox.setValue("Cladosporium");
        speciesComboBox.setMaxWidth(Double.MAX_VALUE);

        sidebar.getChildren().addAll(
            humLabel, humiditySlider,
            tempLabel, temperatureSlider,
            ventLabel, ventilationSlider,
            matLabel, materialComboBox,
            specLabel, speciesComboBox
        );

        // Stats
        sidebar.getChildren().add(sectionLabel("Statistics"));

        generationLabel = statLabel("Step: 0");
        weekLabel       = statLabel("Time elapsed: 0 week(s)");
        stepLabel       = statLabel("Saved step: 0 / 0");
        infectedLabel   = statLabel("Infected: 0 (0.0%)");
        riskLabel       = statLabel("Risk: Low");

        sidebar.getChildren().addAll(
            generationLabel,
            weekLabel,
            stepLabel,
            infectedLabel,
            riskLabel
        );

        return sidebar;
    }

    // ── Grid area ─────────────────────────────────────────────
    private javafx.scene.layout.StackPane buildGridArea() {
        javafx.scene.layout.StackPane stack = new javafx.scene.layout.StackPane();
        stack.setStyle("-fx-background-color: #1A1A1A;");
        gridView = new GridView(50, 60, 11.0);
        stack.getChildren().add(gridView);
        return stack;
    }

    // ── Bottom controls ───────────────────────────────────────
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
            playButton,
            pauseButton,
            previousStepButton,
            stepButton,
            resetButton,
            exportPdfButton,
            speedLabel,
            speedSlider,
            timeLabel,
            timeSlider,
            newShelfButton
        );

        statusLabel = new Label("Ready — place a contamination focus on the grid.");
        statusLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");

        bottom.getChildren().addAll(buttons, statusLabel);
        
        return bottom;
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

    // ── Getters pour le Controller ────────────────────────────
    public Slider getHumiditySlider()      { return humiditySlider; }
    public Slider getTemperatureSlider()   { return temperatureSlider; }
    public Slider getVentilationSlider()   { return ventilationSlider; }
    public Slider getSpeedSlider()         { return speedSlider; }
    public ComboBox<String> getMaterialComboBox() { return materialComboBox; }
    public ComboBox<String> getSpeciesComboBox()  { return speciesComboBox; }
    public Button getPlayButton()          { return playButton; }
    public Button getPauseButton()         { return pauseButton; }
    public Button getResetButton()         { return resetButton; }
    public Button getStepButton()          { return stepButton; }
    public Button getExportPdfButton()     { return exportPdfButton; }
    public Label getStatusLabel()          { return statusLabel; }
    public Label getGenerationLabel()      { return generationLabel; }
    public Label getInfectedLabel()        { return infectedLabel; }
    public Label getRiskLabel()            { return riskLabel; }
    public GridView getGridView()          { return gridView; }

    public Label getCurrentLocationLabel() {
    return currentLocationLabel;
    }

    public TextField getRoomNameField() {
        return roomNameField;
    }

    public TextField getWallNameField() {
        return wallNameField;
    }

    public Button getApplyLocationButton() {
        return applyLocationButton;
    }

    public void updateCurrentLocationLabel(String locationText) {
        currentLocationLabel.setText("Current view: " + locationText);
    }

    public Label getWeekLabel() {
    return weekLabel;
    }   

    public Label getStepLabel() {
        return stepLabel;
    }

    public Button getPreviousStepButton() {
        return previousStepButton;
    }

    public Slider getTimeSlider() {
        return timeSlider;
    }
    public Button getNewShelfButton() { 
        return newShelfButton;
    }

    

}