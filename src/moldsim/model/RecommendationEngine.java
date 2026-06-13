package moldsim.model;

import java.util.ArrayList;
import java.util.List;

public class RecommendationEngine {

    private final ArchiveRoom room;

    public RecommendationEngine(ArchiveRoom room) {
        this.room = room;
    }

    public List<String> analyze(SensorEvent event) {
        return switch (event.getType()) {
            case GLOBAL -> analyzeGlobal(event);
            case SHELF  -> analyzeShelf(event);
        };
    }

    private List<String> analyzeGlobal(SensorEvent event) {
        List<String> recommendations = new ArrayList<>();
        String wallName = getWallName(event.getWall());
        switch (event.getAlertLevel()) {
            case CRITICAL -> {
                recommendations.add("CRITICAL: wall " + wallName + " at "
                    + format(event.getMoldRate())
                    + " infected — dehumidify immediately");
                recommendations.add("Increase room ventilation");
            }
            case HIGH -> recommendations.add("HIGH: wall " + wallName + " at "
                    + format(event.getMoldRate())
                    + " infected — treat affected areas");
            case MEDIUM -> recommendations.add("WARNING: wall " + wallName + " — first signs of contamination, monitor closely");
            case LOW -> {}
        }
        return recommendations;
    }

    private List<String> analyzeShelf(SensorEvent event) {
        List<String> recommendations = new ArrayList<>();
        Shelf shelf = event.getShelf();
        String wallName = getWallName(event.getWall());
        switch (event.getAlertLevel()) {
            case CRITICAL -> recommendations.add("URGENT: shelf " + shelf.getId()
        + " [" + shelf.getValue() + "] infected on wall " + wallName
        + " — immediate removal required");
        case HIGH -> recommendations.add("PREVENTIVE: shelf " + shelf.getId()
                + " [" + shelf.getValue() + "] at risk on wall " + wallName
                + " — neighboring area infected");
            default -> {}
        }
        return recommendations;
    }

    private String getWallName(Wall wall) {
        if (wall == room.getNorthWall()) return "North";
        if (wall == room.getSouthWall()) return "South";
        if (wall == room.getEastWall())  return "East";
        if (wall == room.getWestWall())  return "West";
        return "unknow";
    }

    private String format(double rate) {
        return String.format("%.0f%%", rate * 100);
    }
}