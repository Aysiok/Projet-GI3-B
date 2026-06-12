package moldsim.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import moldsim.model.WallMaterial;

/**
 * Dialog shown at startup to configure the 4 walls.
 */
public class WallConfigDialog {

    // Stores one wall's config
    public static class WallConfig {
        public WallMaterial material;
        public int width;
        public int height;

        public WallConfig(WallMaterial material, int width, int height) {
            this.material = material;
            this.width    = width;
            this.height   = height;
        }
    }

    private final String[] wallNames = {"North", "South", "East", "West"};

    /**
     * Shows the config dialogs and returns 4 WallConfig (one per wall).
     * Returns null if the user cancels.
     */
    public WallConfig[] showAndWait() {
        WallConfig[] configs = new WallConfig[4];

        // Configure the first wall (North)
        WallConfig first = showSingleWallDialog(wallNames[0]);
        if (first == null) return null;
        configs[0] = first;

        // Ask if same config for all other walls
        Alert copyAlert = new Alert(Alert.AlertType.CONFIRMATION);
        copyAlert.setTitle("Apply to all walls?");
        copyAlert.setHeaderText("Apply the same config to the other 3 walls?");
        copyAlert.setContentText(
            "Material: " + first.material +
            "\nWidth: " + first.width +
            "\nHeight: " + first.height
        );

        ButtonType btnYes = new ButtonType("Yes, apply to all");
        ButtonType btnNo  = new ButtonType("No, configure each one");
        copyAlert.getButtonTypes().setAll(btnYes, btnNo);

        boolean copyAll = copyAlert.showAndWait()
            .map(b -> b == btnYes)
            .orElse(true);

        if (copyAll) {
            // Same config for the 3 remaining walls
            for (int i = 1; i < 4; i++) {
                configs[i] = new WallConfig(first.material, first.width, first.height);
            }
        } else {
            // Ask each wall individually
            for (int i = 1; i < 4; i++) {
                WallConfig cfg = showSingleWallDialog(wallNames[i]);
                if (cfg == null) return null;
                configs[i] = cfg;
            }
        }

        return configs;
    }

    /** Shows a dialog for one wall and returns its config. */
    private WallConfig showSingleWallDialog(String wallName) {
        Dialog<WallConfig> dialog = new Dialog<>();
        dialog.setTitle("Configure " + wallName + " Wall");
        dialog.setHeaderText("Choose material and size for the " + wallName + " wall.");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<WallMaterial> materialBox = new ComboBox<>();
        materialBox.getItems().addAll(WallMaterial.values());
        materialBox.setValue(WallMaterial.CONCRETE);

        Spinner<Integer> widthSpinner  = new Spinner<>(10, 200, 60);
        Spinner<Integer> heightSpinner = new Spinner<>(10, 200, 50);
        widthSpinner.setEditable(true);
        heightSpinner.setEditable(true);

        grid.add(new Label("Material:"),      0, 0);
        grid.add(materialBox,                 1, 0);
        grid.add(new Label("Width (cells):"), 0, 1);
        grid.add(widthSpinner,                1, 1);
        grid.add(new Label("Height (cells):"),0, 2);
        grid.add(heightSpinner,               1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return new WallConfig(
                    materialBox.getValue(),
                    widthSpinner.getValue(),
                    heightSpinner.getValue()
                );
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }
}
