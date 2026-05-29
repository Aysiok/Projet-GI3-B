package moldsim.view;

import moldsim.model.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

/**
 * L'Interface Utilisateur (Écran).
 * Elle affiche la grille et les contrôles. Elle ne calcule rien.
 */
public class MainView {
    private final BorderPane root;
    private final GridPane gridDisplay;
    private final Label lblStats;
    private final Button btnPlay;

    public MainView(int width, int height) {
        root = new BorderPane();
        
        // 1. Centre : La Grille de cellules
        gridDisplay = new GridPane();
        gridDisplay.setPadding(new Insets(10));
        gridDisplay.setAlignment(Pos.CENTER);
        root.setCenter(gridDisplay);
        
        // 2. Bas : Barre de statut
        lblStats = new Label("Prêt");
        HBox bottomBar = new HBox(lblStats);
        bottomBar.setPadding(new Insets(10));
        bottomBar.setStyle("-fx-background-color: #f0f0f0;");
        root.setBottom(bottomBar);
        
        // 3. Droite : Panneau de contrôle
        VBox controls = new VBox(15);
        controls.setPadding(new Insets(15));
        controls.setStyle("-fx-background-color: #e8e8e8;");
        
        btnPlay = new Button("Play");
        btnPlay.setPrefWidth(100);
        
        Button btnReset = new Button("Reset (Console)");
        btnReset.setPrefWidth(100);
        // Note: Pour l'instant, Reset relance juste la vue console pour vérifier
        
        controls.getChildren().addAll(new Label("Contrôles"), btnPlay, btnReset);
        root.setRight(controls);
    }
    
    /**
     * Met à jour l'affichage de la grille en fonction des données du modèle.
     */
    public void updateDisplay(Grid grid, Environment env) {
        gridDisplay.getChildren().clear();
        
        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                Cell cell = grid.getCell(x, y);
                Rectangle rect = new Rectangle(18, 18);
                
                // Choix de la couleur selon l'état
                if (cell.getState() == CellState.DEAD) {
                    rect.setFill(Color.DARKGRAY);
                } else if (cell.isInfected()) {
                    // Intensité de la couleur selon le niveau de moisissure
                    double intensity = Math.min(1.0, cell.getMoldLevel() / 50.0);
                    rect.setFill(Color.color(0.0, 0.5 + (intensity * 0.5), 0.0)); // Vert foncé à vert clair
                } else {
                    rect.setFill(Color.BEIGE); // Sain
                }
                
                rect.setStroke(Color.LIGHTGRAY); // Bordure
                gridDisplay.add(rect, x, y);
            }
        }
        
        // Mise à jour des stats
        long infectedCount = 0;
        for (int y=0; y<grid.getHeight(); y++) for(int x=0; x<grid.getWidth(); x++) if(grid.getCell(x,y).isInfected()) infectedCount++;
        
        lblStats.setText(String.format("Humidité: %.0f%% | Temp: %.0f°C | Infectés: %d", 
            env.getHumidity(), env.getTemperature(), infectedCount));
    }
    
    // Getters pour que le contrôleur accède aux boutons
    public Button getPlayButton() { return btnPlay; }
    public BorderPane getRoot() { return root; }
    
    public void setPlayButtonText(String text) {
        btnPlay.setText(text);
    }
}
