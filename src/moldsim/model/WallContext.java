package moldsim.model;

import java.util.ArrayList;
import java.util.List;

import moldsim.controller.SimulationController;

public class WallContext {

    private final String name;
    private final Wall wall;
    private final List<Shelf> shelves;
    private final SimulationController simulationController;

    public WallContext(String name, int width, int height, WallMaterial material, Environment environment) {
        this.name = name;
        this.wall = new Wall(width, height, material);
        this.shelves = new ArrayList<>();
        this.simulationController = new SimulationController(wall, environment);
    }

    public String getName() {
        return name;
    }

    public Wall getWall() {
        return wall;
    }

    public List<Shelf> getShelves() {
        return shelves;
    }

    public SimulationController getSimulationController() {
        return simulationController;
    }
}