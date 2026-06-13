package moldsim.model;

import java.io.*;

/**
 * Handles binary save and load of simulation state.
 */
public class BinaryExporter {

    /**
     * Saves the simulation state to a binary file.
     */
    public static void save(SimulationState state, String filePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(filePath))) {
            oos.writeObject(state);
            System.out.println("Simulation saved to: " + filePath);
        } catch (IOException e) {
            System.err.println("Save failed: " + e.getMessage());
        }
    }

    /**
     * Loads a simulation state from a binary file.
     */
    public static SimulationState load(String filePath) {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(filePath))) {
            return (SimulationState) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Load failed: " + e.getMessage());
            return null;
        }
    }
}