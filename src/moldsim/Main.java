package moldsim;
import moldsim.controller.*;
import moldsim.model.*;

import java.util.ArrayList;
import java.util.List;

import java.util.Scanner;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import moldsim.controller.GridController;
import moldsim.view.MainView;

/**
 * Entry point of the JavaFX application.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainView mainView = new MainView();

        GridController gridController = new GridController(mainView);
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