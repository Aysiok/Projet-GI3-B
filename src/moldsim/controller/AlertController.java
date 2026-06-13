package moldsim.controller;

import java.util.ArrayList;
import java.util.List;
import moldsim.model.RecommendationEngine;
import moldsim.model.SensorEvent;

public class AlertController {

    private final List<SensorEvent> history;
    private RecommendationEngine recommendationEngine;
    private AlertLogger logger;
    
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
                if (logger != null) {
                    logger.log("[RECOMMENDATION] " + r);
                }
            });
        }
}

    private void log(SensorEvent event) {
    if (logger == null) return;
    switch (event.getType()) {
        case GLOBAL ->
            logger.log(String.format("[GLOBAL ALERT] %s | Week %d | %s | rate %.1f%%",
                contextName, event.getWeek(), event.getAlertLevel(), event.getMoldRate() * 100));
        case SHELF -> {
            if (event.getShelf() == null) {
                logger.log(String.format("[ERROR] %s | Week %d | SHELF event without shelf",
                    contextName, event.getWeek()));
                return;
            }
            logger.log(String.format("[SHELF ALERT] %s | Week %d | %s | shelf %s | rate %.1f%%",
                contextName, event.getWeek(), event.getAlertLevel(),
                event.getShelf().getId(), event.getMoldRate() * 100));
        }
    }
}

    public void setLogger(AlertLogger logger) {
        this.logger = logger;
    }

    public void clearHistory() {
        history.clear();
    }

    public List<SensorEvent> getHistory() {
        return history;
    }

    public RecommendationEngine getRecommendationEngine() {
        return recommendationEngine;
    }
}