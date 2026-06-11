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
                recommendations.add("CRITIQUE : mur " + wallName + " à "
                    + format(event.getMoldRate())
                    + " infecté — déshumidifier immédiatement");
                recommendations.add("Augmenter la ventilation de la pièce");
            }
            case HIGH -> recommendations.add("ÉLEVÉ : mur " + wallName + " à "
                    + format(event.getMoldRate())
                    + " infecté — traiter les zones touchées");
            case MEDIUM -> recommendations.add("ATTENTION : mur " + wallName
                    + " — premiers signes de contamination, surveiller");
            case LOW -> {}
        }
        return recommendations;
    }

    private List<String> analyzeShelf(SensorEvent event) {
        List<String> recommendations = new ArrayList<>();
        Shelf shelf = event.getShelf();
        String wallName = getWallName(event.getWall());
        switch (event.getAlertLevel()) {
            case CRITICAL -> recommendations.add("URGENT : étagère " + shelf.getId()
                    + " [" + shelf.getValue() + "] infectée sur mur " + wallName
                    + " — évacuation immédiate");
            case HIGH -> recommendations.add("PRÉVENTIF : étagère " + shelf.getId()
                    + " [" + shelf.getValue() + "] menacée sur mur " + wallName
                    + " — zone voisine infectée");
            default -> {}
        }
        return recommendations;
    }

    private String getWallName(Wall wall) {
        if (wall == room.getNorthWall()) return "Nord";
        if (wall == room.getSouthWall()) return "Sud";
        if (wall == room.getEastWall())  return "Est";
        if (wall == room.getWestWall())  return "Ouest";
        return "inconnu";
    }

    private String format(double rate) {
        return String.format("%.0f%%", rate * 100);
    }
}