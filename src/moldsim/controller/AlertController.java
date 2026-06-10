package moldsim.controller;

import moldsim.model.AlertLevel;
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
        if (recommendationEngine != null
         && event.getAlertLevel().ordinal() >= AlertLevel.HIGH.ordinal()) {
            List<String> recs = recommendationEngine.analyze();
            recs.forEach(r -> System.out.println("[RECOMMANDATION] " + r));
        }
    }

    private void log(SensorEvent event) {
        System.out.printf("[ALERTE] Semaine %d | %s | taux %.1f%%%n",
            event.getWeek(),
            event.getAlertLevel(),
            event.getMoldRate() * 100
        );
    }

    public List<SensorEvent> getHistory() { return history; }
}