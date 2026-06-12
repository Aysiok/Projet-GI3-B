package moldsim.controller;

import java.util.ArrayList;
import java.util.List;
import moldsim.model.RecommendationEngine;
import moldsim.model.SensorEvent;

public class AlertController {

    private final List<SensorEvent> history;
    private RecommendationEngine recommendationEngine;
    
    private String contextName = "Current wall";

    public void setContextName(String contextName) {
        if (contextName == null || contextName.trim().isEmpty()) {
            this.contextName = "Current wall";
        } else {
            this.contextName = contextName.trim();
        }
    }

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
            recommendationEngine.analyze(event).forEach(r -> {
                String message = contextualizeRecommendation(r);
                System.out.println("[RECOMMANDATION] " + message);
            });
        }
    }

    private void log(SensorEvent event) {
    switch (event.getType()) {
        case GLOBAL : 
            System.out.printf("[ALERTE GLOBALE] %s | Semaine %d | %s | taux %.1f%%%n", contextName, event.getWeek(), event.getAlertLevel(), event.getMoldRate() * 100);
            break;
        case SHELF : 
            if (event.getShelf() == null) {
                System.err.printf(
                    "[ERREUR] %s | Semaine %d | event SHELF reçu sans étagère associée%n",
                    contextName, event.getWeek());
                return;
            }
            System.out.printf("[ALERTE ÉTAGÈRE] %s | Semaine %d | %s | étagère %s | taux %.1f%%%n", contextName, event.getWeek(), event.getAlertLevel(), event.getShelf().getId(), event.getMoldRate() * 100);
            break;
    }
}

    public List<SensorEvent> getHistory() {
        return history;
    }

    private String contextualizeRecommendation(String recommendation) {
        if (recommendation == null) {
            return "";
        }

        return recommendation
                .replace("mur Nord", contextName)
                .replace("Mur Nord", contextName)
                .replace("mur nord", contextName)
                .replace("North Wall", contextName);
    }
}