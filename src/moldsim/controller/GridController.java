package moldsim.controller;

import moldsim.view.GridView;
import moldsim.view.MainView;

/**
 * Controller for the grid interface.
 * It connects MainView controls to GridView actions.
 */
public class GridController {

    private final MainView mainView;
    private final GridView gridView;

    private int generation;

    public GridController(MainView mainView) {
        this.mainView = mainView;
        this.gridView = mainView.getGridView();
        this.generation = 0;
    }

    public void initialize() {
        mainView.getPlayButton().setOnAction(event -> play());
        mainView.getPauseButton().setOnAction(event -> pause());
        mainView.getStepButton().setOnAction(event -> step());
        mainView.getResetButton().setOnAction(event -> reset());

        gridView.setCellClickListener((row, column) -> {
            gridView.toggleInfection(row, column);
            updateStatistics();
            mainView.getStatusLabel().setText("Cell modified at (" + row + ", " + column + ").");
        });

        updateStatistics();
    }

    private void play() {
        mainView.getStatusLabel().setText("Simulation started.");
    }

    private void pause() {
        mainView.getStatusLabel().setText("Simulation paused.");
    }

    private void step() {
        generation++;
        gridView.stepSimulation();
        updateStatistics();
        mainView.getStatusLabel().setText("One simulation step executed.");
    }

    private void reset() {
        generation = 0;
        gridView.reset();
        updateStatistics();
        mainView.getStatusLabel().setText("Simulation reset.");
    }

    private void updateStatistics() {
        int infected = gridView.countInfectedCells();
        int total = gridView.getRows() * gridView.getColumns();

        double percentage = 0.0;
        if (total > 0) {
            percentage = infected * 100.0 / total;
        }

        mainView.getGenerationLabel().setText("Generation: " + generation);
        mainView.getInfectedLabel().setText(String.format("Infected: %d (%.1f%%)", infected, percentage));

        if (percentage < 10) {
            mainView.getRiskLabel().setText("Risk: Low");
        } else if (percentage < 30) {
            mainView.getRiskLabel().setText("Risk: Medium");
        } else {
            mainView.getRiskLabel().setText("Risk: High");
        }
    }
}