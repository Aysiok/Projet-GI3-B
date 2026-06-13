package moldsim.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import moldsim.model.Environment;
import moldsim.model.Shelf;
import moldsim.model.Wall;
import moldsim.model.WallMaterial;

/**
 * Stores all data associated with a wall in the simulation.
 * <p>
 * A wall context groups the wall model, its shelves, display name,
 * and the simulation controller responsible for managing its state.
 */
public class WallContext implements Serializable{

    /**
     * Serialization version identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Display name of the wall.
     */
    private String name;

    /**
     * Wall associated with this context.
     */
    private final Wall wall;

    /**
     * Shelves attached to the wall.
     */
    private final List<Shelf> shelves;

    /**
     * Simulation controller associated with the wall.
     */
    private transient SimulationController simulationController;

    /**
     * Creates a wall context.
     *
     * @param name wall display name
     * @param width wall width
     * @param height wall height
     * @param material wall material
     * @param environment simulation environment
     */
    public WallContext(String name, int width, int height, WallMaterial material, Environment environment) {
        this.name = name;
        this.wall = new Wall(width, height, material);
        this.shelves = new ArrayList<>();
        rebuildController(environment);
    }

    /**
     * Returns the wall name.
     *
     * @return wall name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the associated wall.
     *
     * @return wall instance
     */
    public Wall getWall() {
        return wall;
    }

    /**
     * Returns the shelves attached to the wall.
     *
     * @return list of shelves
     */
    public List<Shelf> getShelves() {
        return shelves;
    }

    /**
     * Returns the simulation controller.
     *
     * @return simulation controller
     */
    public SimulationController getSimulationController() {
        return simulationController;
    }

    /**
     * Updates the wall name.
     *
     * @param name new wall name
     */
    public void setName(String name) {
        this.name = name;
        this.simulationController.setDisplayName(name);
    }

    /**
     * Recreates the simulation controller using the provided environment.
     *
     * @param environment simulation environment
     */
    public void rebuildController(Environment environment) {
        this.simulationController = new SimulationController(wall, environment);
        this.simulationController.setDisplayName(name);
    }
}