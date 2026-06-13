package moldsim.controller;

import moldsim.model.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for generating and opening PDF simulation reports.
 * <p>
 * This class collects simulation statistics, alert history, and environment
 * data, then delegates report generation to the PDF exporter.
 */
public class PdfExportService {

    /**
     * Exports simulation data to a PDF report.
     *
     * @param history simulation history snapshots
     * @param walls wall contexts used to restore states
     * @param environment simulation environment
     * @param alertSource source of alert information
     * @return the generated PDF file path, or null if no data is available
     */
    public String export(List<SimulationSnapshot> history, List<WallContext> walls, Environment environment, SimulationController alertSource) {
        if (history.isEmpty()) return null;

        List<Statistics> statsList = new ArrayList<>();
        List<List<String>> allLogs = new ArrayList<>();
        int previousInfected = 0;

        for (SimulationSnapshot snap : history) {
            restoreAllWallsFromSnapshot(snap, walls);

            Wall firstWall = walls.get(0).getWall();
            Statistics stats = new Statistics(firstWall, previousInfected);
            previousInfected = stats.getInfectedCells();
            statsList.add(stats);

            List<String> logs = alertSource.getAlertController().getHistory().stream().filter(e -> e.getWeek() == snap.getWeek()).map(e -> "[" + e.getType() + "] " + e.getAlertLevel() + " — " + (e.getShelf() != null ? "shelf " + e.getShelf().getId() : String.format("rate %.0f%%", e.getMoldRate() * 100))).collect(Collectors.toList());
            allLogs.add(logs);
        }

        String filePath = "report_" + System.currentTimeMillis() + ".pdf";
        PdfExporter.export(statsList, environment, allLogs, filePath);
        return filePath;
    }

    /**
     * Opens a generated PDF file using the system default application.
     *
     * @param filePath path of the PDF file to open
     * @throws IOException if the file cannot be opened
     */
    public void openFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (file.exists() && java.awt.Desktop.isDesktopSupported()) {
            java.awt.Desktop.getDesktop().open(file);
        }
    }

    /**
     * Restores all wall states from a simulation snapshot.
     *
     * @param snapshot snapshot containing saved wall states
     * @param walls wall contexts to restore
     */
    private void restoreAllWallsFromSnapshot(SimulationSnapshot snapshot, List<WallContext> walls) {
        List<int[][]> allStates = snapshot.getWallCellStates();
        int limit = Math.min(allStates.size(), walls.size());
        for (int i = 0; i < limit; i++) {
            restoreWallState(walls.get(i).getWall(), allStates.get(i));
        }
    }

    /**
     * Restores a wall using a previously saved state matrix.
     *
     * @param wall wall to restore
     * @param savedState saved cell state matrix
     */
    private void restoreWallState(Wall wall, int[][] savedState) {
        if (wall == null || savedState == null) return;
        int height = Math.min(wall.getHeight(), savedState.length);
        for (int row = 0; row < height; row++) {
            int width = Math.min(wall.getWidth(), savedState[row].length);
            for (int col = 0; col < width; col++) {
                Cell cell = wall.getCell(col, row);
                if (cell == null) continue;
                int state = savedState[row][col];
                switch (state) {
                    case 3 -> {
                        cell.setState(CellState.DEPOSITED_SPORE);
                        cell.setSpecies(MoldSpecies.CLADOSPORIUM);
                        cell.setMoldLevel(0.0);
                        cell.setAge(0);
                    }
                    case 4 -> {
                        cell.setState(CellState.SPORULATING);
                        cell.setSpecies(MoldSpecies.CLADOSPORIUM);
                    }
                    case 1 -> {
                        cell.setState(CellState.HEALTHY);
                        cell.setSpecies(null);
                        cell.setMoldLevel(0.0);
                        cell.setAge(0);
                        cell.infect(MoldSpecies.CLADOSPORIUM);
                    }
                    case 2 -> cell.kill();
                    default -> {
                        cell.setState(CellState.HEALTHY);
                        cell.setSpecies(null);
                        cell.setMoldLevel(0.0);
                        cell.setAge(0);
                    }
                }
            }
        }
    }
}