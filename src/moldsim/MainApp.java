package moldsim;

import moldsim.model.*;
import moldsim.controller.SimulationController;
import moldsim.view.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Point d'entrée de l'application graphique (JavaFX).
 * Cette classe assemble le Modèle, la Vue et le Contrôleur.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 1. Initialisation du Modèle (M)
        Grid grid = new Grid(20, 20, false, NeighborhoodMode.FOUR);
        Environment env = new Environment();
        Simulation simulation = new Simulation(grid, env);
        
        // On infecte une cellule au centre pour le test
        grid.getCell(10, 10).infect(MoldSpecies.CLADOSPORIUM);

        // 2. Initialisation de la Vue (V)
        MainView view = new MainView(grid.getWidth(), grid.getHeight());

        // 3. Initialisation du Contrôleur (C)
        SimulationController controller = new SimulationController(simulation, view);

        // 4. Configuration de la fenêtre
        Scene scene = new Scene(view.getRoot(), 800, 600);
        primaryStage.setTitle("Simulateur Moisissure - Projet GI3");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Premier affichage
        view.updateDisplay(grid, env);
    }

    public static void main(String[] args) {
        launch(args); // Lance JavaFX
    }
}
