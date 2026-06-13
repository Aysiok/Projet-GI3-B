package moldsim;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import moldsim.controller.GridController;
import moldsim.model.WallMaterial;
import moldsim.view.MainView;
import moldsim.view.WallConfigDialog;
import moldsim.view.WallConfigDialog.WallConfig;

/**
 * Entry point of the ArchiveShield Mold Risk Simulator application.
 * <p>
 * Initializes the JavaFX application, shows the wall configuration dialog
 * on first launch, and starts the main simulation UI.
 */
public class Main extends Application {

    /** Global configuration of all simulation walls. */
    private static WallConfig[] wallConfigs = null;
    /** Ensures configuration dialog is shown only once. */
    private static boolean configShown = false;

    /**
     * Starts the JavaFX application and initializes the simulation UI.
     *
     * @param primaryStage primary stage provided by JavaFX
     */
    @Override
    public void start(Stage primaryStage) {

        if (!configShown) {
            configShown = true;
            WallConfigDialog configDialog = new WallConfigDialog();
            wallConfigs = configDialog.showAndWait();

            if (wallConfigs == null) {
                wallConfigs = new WallConfig[] {
                    new WallConfig(WallMaterial.CONCRETE, 3.0, 2.5),
                    new WallConfig(WallMaterial.CONCRETE, 3.0, 2.5),
                    new WallConfig(WallMaterial.CONCRETE, 3.0, 2.5),
                    new WallConfig(WallMaterial.CONCRETE, 3.0, 2.5)
                };
            }
        }
        MainView mainView = new MainView();
        GridController gridController = new GridController(mainView, wallConfigs);        
        gridController.initialize();

        Scene scene = new Scene(mainView, 1300, 700);
        primaryStage.setTitle("ArchiveShield — Mold Risk Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Launches the JavaFX application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}