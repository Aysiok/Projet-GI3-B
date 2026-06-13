package moldsim.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import moldsim.model.Environment;
import moldsim.model.Shelf;
import moldsim.model.Wall;
import moldsim.model.WallMaterial;

public class WallContext implements Serializable{

    private static final long serialVersionUID = 1L;
    private String name;
    private final Wall wall;
    private final List<Shelf> shelves;
    private transient SimulationController simulationController;

    public WallContext(String name, int width, int height, WallMaterial material, Environment environment) {
        this.name = name;
        this.wall = new Wall(width, height, material);
        this.shelves = new ArrayList<>();
        rebuildController(environment);
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

    public void setName(String name) {
        this.name = name;
        this.simulationController.setDisplayName(name);
    }

    public void rebuildController(Environment environment) {
        this.simulationController = new SimulationController(wall, environment);
        this.simulationController.setDisplayName(name);
    }
}