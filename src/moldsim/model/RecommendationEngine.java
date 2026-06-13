package moldsim.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Engine responsible for generating textual recommendations based on sensor events.
 * <p>
 * It analyzes global and shelf-specific alerts and produces human-readable
 * recommendations for maintenance and risk mitigation.
 */
public class RecommendationEngine {
    /**
     * Name of the wall currently associated with this engine.
     */
    private String wallName;

    /**
     * Creates a recommendation engine for a specific wall.
     *
     * @param wallName name of the wall
     */
    public RecommendationEngine(String wallName) {
         this.wallName = wallName;
    }

    /**
     * Analyzes a sensor event and generates recommendations.
     *
     * @param event sensor event to analyze
     * @return list of generated recommendations
     */
    public List<String> analyze(SensorEvent event) {
        return switch (event.getType()) {
            case GLOBAL -> analyzeGlobal(event);
            case SHELF -> analyzeShelf(event);
        };
    }

    /**
     * Generates recommendations for global wall-level events.
     *
     * @param event sensor event
     * @return list of recommendations
     */
    private List<String> analyzeGlobal(SensorEvent event) {
        List<String> recommendations = new ArrayList<>();
        String wallName = getWallName(event.getWall());
        switch (event.getAlertLevel()) {
            case CRITICAL -> {
                recommendations.add("CRITICAL: wall " + wallName + " at " + format(event.getMoldRate()) + " infected — dehumidify immediately");
                recommendations.add("Increase room ventilation");
            }
            case HIGH -> recommendations.add("HIGH: wall " + wallName + " at " + format(event.getMoldRate()) + " infected — treat affected areas");
            case MEDIUM -> recommendations.add("WARNING: wall " + wallName + " — first signs of contamination, monitor closely");
            case LOW -> {}
        }
        return recommendations;
    }

    /**
     * Generates recommendations for shelf-specific events.
     *
     * @param event sensor event
     * @return list of recommendations
     */
    private List<String> analyzeShelf(SensorEvent event) {
        List<String> recommendations = new ArrayList<>();
        Shelf shelf = event.getShelf();
        String wallName = getWallName(event.getWall());
        switch (event.getAlertLevel()) {
            case CRITICAL -> recommendations.add("URGENT: shelf " + shelf.getId() + " [" + shelf.getValue() + "] infected on wall " + wallName + " — immediate removal required");
            case HIGH -> recommendations.add("PREVENTIVE: shelf " + shelf.getId() + " [" + shelf.getValue() + "] at risk on wall " + wallName + " — neighboring area infected");
            default -> {}
        }
        return recommendations;
    }

    /**
     * Updates the wall name used in recommendations.
     *
     * @param wallName new wall name
     */
    public void setWallName(String wallName) {
        this.wallName = wallName;
    }

    /**
     * Returns the name of a wall (currently using stored context).
     *
     * @param wall wall instance
     * @return wall name
     */
    private String getWallName(Wall wall) {
        return wallName;
    }
    
    /**
     * Formats a rate as a percentage string.
     *
     * @param rate value between 0 and 1
     * @return formatted percentage string
     */
    private String format(double rate) {
        return String.format("%.0f%%", rate * 100);
    }
}