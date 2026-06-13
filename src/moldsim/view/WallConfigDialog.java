package moldsim.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import moldsim.model.WallMaterial;

import moldsim.model.GridScale;

/**
 * Dialog shown at startup to configure the 4 walls.
 */
public class WallConfigDialog {

    public static class WallConfig {
        public WallMaterial material;
        public int width;
        public int height;

        public double widthMeters;
        public double heightMeters;

        public WallConfig(WallMaterial material, double widthMeters, double heightMeters) {
            this.material = material;
            this.widthMeters = widthMeters;
            this.heightMeters = heightMeters;
            this.width = moldsim.model.GridScale.metersToCells(widthMeters);
            this.height = moldsim.model.GridScale.metersToCells(heightMeters);
        }

        public WallConfig(WallMaterial material, int width, int height) {
            this.material = material;
            this.width = width;
            this.height = height;
            this.widthMeters = moldsim.model.GridScale.cellsToMeters(width);
            this.heightMeters = moldsim.model.GridScale.cellsToMeters(height);
        }
    }

    private final String[] wallNames = {"North", "South", "East", "West"};

    public WallConfig[] showAndWait() {
        WallConfig[] configs = new WallConfig[4];
    
        WallConfig first = showSingleWallDialog(wallNames[0], -1.0);
        if (first == null) {
            return null;
        }
    
        configs[0] = first;
    
        Alert copyAlert = new Alert(Alert.AlertType.CONFIRMATION);
        copyAlert.setTitle("Apply to all walls?");
        copyAlert.setHeaderText("Apply the same config to South, East and West?");
        copyAlert.setContentText(
            "Material: " + first.material
            + "\nWidth: " + first.widthMeters + " m (" + first.width + " cells)"
            + "\nHeight: " + first.heightMeters + " m (" + first.height + " cells)"
            + "\nScale: " + GridScale.getScaleDescription()
        );
    
        ButtonType btnYes = new ButtonType("Yes, apply to all");
        ButtonType btnNo  = new ButtonType("No, configure each one");
        copyAlert.getButtonTypes().setAll(btnYes, btnNo);
    
        ButtonType clicked = copyAlert.showAndWait().orElse(btnNo);
        boolean copyAll = clicked == btnYes;
    
        if (copyAll) {
            for (int i = 1; i < 4; i++) {
                configs[i] = new WallConfig(
                    first.material,
                    first.widthMeters,
                    first.heightMeters
                );
            }
        } else {
            for (int i = 1; i < 4; i++) {
                WallConfig cfg = showSingleWallDialog(wallNames[i], first.heightMeters);
    
                if (cfg == null) {
                    return null;
                }
    
                configs[i] = cfg;
            }
        }
    
        return configs;
    }

    /**
     * Shows a config dialog for one wall.
     * If fixedHeight > 0, the height field is locked and shown as read-only.
     */
    private WallConfig showSingleWallDialog(String wallName, double fixedHeightMeters) {
        Dialog<WallConfig> dialog = new Dialog<>();
        dialog.setTitle("Configure " + wallName + " Wall");
    
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
    
        ComboBox<WallMaterial> materialBox = new ComboBox<>();
        materialBox.getItems().addAll(WallMaterial.values());
        materialBox.setValue(WallMaterial.CONCRETE);
    
        TextField widthField = new TextField("3.0");
    
        grid.add(new Label("Material:"), 0, 0);
        grid.add(materialBox, 1, 0);
    
        grid.add(new Label("Width (m):"), 0, 1);
        grid.add(widthField, 1, 1);
    
        grid.add(new Label("Height (m):"), 0, 2);
    
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    
        if (fixedHeightMeters <= 0) {
            dialog.setHeaderText(
                "Choose material and size for the " + wallName + " wall.\n"
                + GridScale.getScaleDescription()
            );
    
            TextField heightField = new TextField("2.5");
            grid.add(heightField, 1, 2);
    
            dialog.setResultConverter(btn -> {
                if (btn == ButtonType.OK) {
                    try {
                        double widthMeters = GridScale.parseMeters(widthField.getText());
                        double heightMeters = GridScale.parseMeters(heightField.getText());
    
                        return new WallConfig(materialBox.getValue(), widthMeters, heightMeters);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
    
                return null;
            });
    
        } else {
            dialog.setHeaderText(
                "Configure " + wallName + " wall.\n"
                + "Height is fixed at " + fixedHeightMeters + " m.\n"
                + GridScale.getScaleDescription()
            );
    
            Label fixedHeightLabel = new Label(fixedHeightMeters + " m (fixed)");
            fixedHeightLabel.setDisable(true);
            grid.add(fixedHeightLabel, 1, 2);
    
            dialog.setResultConverter(btn -> {
                if (btn == ButtonType.OK) {
                    try {
                        double widthMeters = GridScale.parseMeters(widthField.getText());
    
                        return new WallConfig(materialBox.getValue(), widthMeters, fixedHeightMeters);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
    
                return null;
            });
        }
    
        return dialog.showAndWait().orElse(null);
    }
}