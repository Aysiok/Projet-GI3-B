package moldsim;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import moldsim.controller.GridController;
import moldsim.model.ArchiveRoom;
import moldsim.model.Environment;
import moldsim.model.Wall;
import moldsim.model.WallMaterial;
import moldsim.view.MainView;
import moldsim.view.WallConfigDialog;
import moldsim.view.WallConfigDialog.WallConfig;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {

        // Show wall config dialog at startup
        WallConfigDialog configDialog = new WallConfigDialog();
        WallConfig[] wallConfigs = configDialog.showAndWait();

        // If user cancels, use default values
        if (wallConfigs == null) {
            wallConfigs = new WallConfig[] {
                new WallConfig(WallMaterial.CONCRETE, 60, 50),
                new WallConfig(WallMaterial.CONCRETE, 60, 50),
                new WallConfig(WallMaterial.CONCRETE, 60, 50),
                new WallConfig(WallMaterial.CONCRETE, 60, 50)
            };
        }

        // Build the 4 walls from user config
        Wall northWall = new Wall(wallConfigs[0].width, wallConfigs[0].height, wallConfigs[0].material);
        Wall southWall = new Wall(wallConfigs[1].width, wallConfigs[1].height, wallConfigs[1].material);
        Wall eastWall  = new Wall(wallConfigs[2].width, wallConfigs[2].height, wallConfigs[2].material);
        Wall westWall  = new Wall(wallConfigs[3].width, wallConfigs[3].height, wallConfigs[3].material);

        // Pass north wall to GridController (the one displayed)
        MainView mainView = new MainView();
        GridController gridController = new GridController(mainView, northWall);
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
