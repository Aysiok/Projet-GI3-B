package moldsim.controller;

import moldsim.model.SensorEvent;
import moldsim.model.RecommendationEngine;
import java.util.ArrayList;
import java.util.List;

public class AlertController {

    private final List<SensorEvent> history;
    private RecommendationEngine recommendationEngine;

    public AlertController() {
        this.history = new ArrayList<>();
    }

    public void setRecommendationEngine(RecommendationEngine engine) {
        this.recommendationEngine = engine;
    }

    public void handle(SensorEvent event) {
        history.add(event);
        log(event);
        if (recommendationEngine != null) {
            recommendationEngine.analyze(event).forEach(r -> System.out.println("[RECOMMANDATION] " + r));
        }
    }

    private void log(SensorEvent event) {
    switch (event.getType()) {
        case GLOBAL : 
            System.out.printf("[ALERTE GLOBALE] Semaine %d | %s | taux %.1f%%%n", event.getWeek(), event.getAlertLevel(), event.getMoldRate() * 100);
            break;
        case SHELF : 
            if (event.getShelf() == null) {
                System.err.printf(
                    "[ERREUR] Semaine %d | event SHELF reçu sans étagère associée%n",
                    event.getWeek());
                return;
            }
            System.out.printf("[ALERTE ÉTAGÈRE] Semaine %d | %s | étagère %s | taux %.1f%%%n", event.getWeek(), event.getAlertLevel(), event.getShelf().getId(), event.getMoldRate() * 100);
            break;
    }
}

    public List<SensorEvent> getHistory() {
        return history;
    }
}