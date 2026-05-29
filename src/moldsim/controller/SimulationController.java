package moldsim.controller;

import moldsim.model.*;
import moldsim.view.MainView;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 * Le Chef d'orchestre.
 * Il fait le lien entre le Modèle (données) et la Vue (affichage).
 */
public class SimulationController {
    private final Simulation simulation;
    private final MainView view;
    private final Timeline timeline; // L'horloge interne

    public SimulationController(Simulation simulation, MainView view) {
        this.simulation = simulation;
        this.view = view;
        
        // Configuration de l'horloge : 1 tour par seconde
        this.timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            simulation.step(); // Fait avancer la simulation
            view.updateDisplay(simulation.getGrid(), simulation.getEnvironment()); // Met à jour l'écran
        }));
        timeline.setCycleCount(Animation.INDEFINITE);

        // Connexion des boutons
        view.getPlayButton().setOnAction(e -> togglePlay());
    }

    private void togglePlay() {
        if (timeline.getStatus() == Animation.Status.RUNNING) {
            timeline.pause();
            view.setPlayButtonText("Play");
        } else {
            timeline.play();
            view.setPlayButtonText("Pause");
        }
    }
}
