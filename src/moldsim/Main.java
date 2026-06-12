package moldsim;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import moldsim.controller.GridController;
import moldsim.model.Wall;
import moldsim.model.WallMaterial;
import moldsim.view.MainView;
import moldsim.view.WallConfigDialog;
import moldsim.view.WallConfigDialog.WallConfig;

public class Main extends Application {

    private static WallConfig[] wallConfigs = null;
    private static boolean configShown = false;

    @Override
    public void start(Stage primaryStage) {

        if (!configShown) {
            configShown = true;
            WallConfigDialog configDialog = new WallConfigDialog();
            wallConfigs = configDialog.showAndWait();

            if (wallConfigs == null) {
                wallConfigs = new WallConfig[] {
                    new WallConfig(WallMaterial.CONCRETE, 60, 50),
                    new WallConfig(WallMaterial.CONCRETE, 60, 50),
                    new WallConfig(WallMaterial.CONCRETE, 60, 50),
                    new WallConfig(WallMaterial.CONCRETE, 60, 50)
                };
            }
        }

        Wall northWall = new Wall(
            wallConfigs[0].width,
            wallConfigs[0].height,
            wallConfigs[0].material
        );

        MainView mainView = new MainView();
        GridController gridController = new GridController(mainView, wallConfigs);        
        gridController.initialize();

        Scene scene = new Scene(mainView, 1000, 700);
        primaryStage.setTitle("ArchiveShield — Mold Risk Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}