package moldsim.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import moldsim.model.WallMaterial;

/**
 * Dialog shown at startup to configure the 4 walls.
 */
public class WallConfigDialog {

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

    public WallConfig[] showAndWait() {
        WallConfig[] configs = new WallConfig[4];

        // Step 1 — configure North wall (all 3 fields)
        WallConfig first = showSingleWallDialog(wallNames[0], -1);
        if (first == null) return null;
        configs[0] = first;

        // Step 2 — ask if same config for all
        Alert copyAlert = new Alert(Alert.AlertType.CONFIRMATION);
        copyAlert.setTitle("Apply to all walls?");
        copyAlert.setHeaderText("Apply the same config to South, East and West?");
        copyAlert.setContentText(
            "Material: " + first.material +
            "\nWidth: " + first.width +
            "\nHeight: " + first.height + " (will be fixed for all walls)"
        );
        ButtonType btnYes = new ButtonType("Yes, apply to all");
        ButtonType btnNo  = new ButtonType("No, configure each one");
        copyAlert.getButtonTypes().setAll(btnYes, btnNo);

        ButtonType clicked = copyAlert.showAndWait().orElse(btnNo);
        boolean copyAll = (clicked == btnYes);

        if (copyAll) {
            for (int i = 1; i < 4; i++) {
                configs[i] = new WallConfig(first.material, first.width, first.height);
            }
        } else {
            // Step 3 — configure South, East, West with fixed height
            for (int i = 1; i < 4; i++) {
                WallConfig cfg = showSingleWallDialog(wallNames[i], first.height);
                if (cfg == null) return null;
                configs[i] = cfg;
            }
        }

        return configs;
    }

    /**
     * Shows a config dialog for one wall.
     * If fixedHeight > 0, the height field is locked and shown as read-only.
     */
    private WallConfig showSingleWallDialog(String wallName, int fixedHeight) {
        Dialog<WallConfig> dialog = new Dialog<>();
        dialog.setTitle("Configure " + wallName + " Wall");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<WallMaterial> materialBox = new ComboBox<>();
        materialBox.getItems().addAll(WallMaterial.values());
        materialBox.setValue(WallMaterial.CONCRETE);

        Spinner<Integer> widthSpinner = new Spinner<>(10, 200, 60);
        widthSpinner.setEditable(true);

        grid.add(new Label("Material:"),      0, 0);
        grid.add(materialBox,                 1, 0);
        grid.add(new Label("Width (cells):"), 0, 1);
        grid.add(widthSpinner,                1, 1);
        grid.add(new Label("Height (cells):"),0, 2);

        if (fixedHeight <= 0) {
            // North wall — height is free
            dialog.setHeaderText("Choose material and size for the " + wallName + " wall.");
            Spinner<Integer> heightSpinner = new Spinner<>(10, 200, 50);
            heightSpinner.setEditable(true);
            grid.add(heightSpinner, 1, 2);

            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialog.setResultConverter(btn -> {
                if (btn == ButtonType.OK)
                    return new WallConfig(materialBox.getValue(), widthSpinner.getValue(), heightSpinner.getValue());
                return null;
            });
        } else {
            // South/East/West — height locked
            dialog.setHeaderText("Configure " + wallName + " wall — height is fixed at " + fixedHeight + " cells.");
            Label fixedLabel = new Label(fixedHeight + "  (fixed)");
            fixedLabel.setDisable(true);
            grid.add(fixedLabel, 1, 2);

            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialog.setResultConverter(btn -> {
                if (btn == ButtonType.OK)
                    return new WallConfig(materialBox.getValue(), widthSpinner.getValue(), fixedHeight);
                return null;
            });
        }

        return dialog.showAndWait().orElse(null);
    }
}