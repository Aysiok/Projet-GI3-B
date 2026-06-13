package moldsim.model;

import java.io.*;

/**
 * Utility class responsible for serializing and deserializing simulation state
 * using binary (Java object) format.
 * <p>
 * This class provides methods to persist a SimulationState to disk and reload
 * it later using Java object streams.
 */
public class BinaryExporter {

    /**
     * Saves the given simulation state to a binary file.
     *
     * @param state simulation state to save
     * @param filePath destination file path
     */
    public static void save(SimulationState state, String filePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(state);
            System.out.println("Simulation saved to: " + filePath);
        } catch (IOException e) {
            System.err.println("Save failed: " + e.getMessage());
        }
    }

    /**
     * Loads a simulation state from a binary file.
     *
     * @param filePath path of the binary file to read
     * @return deserialized simulation state, or null if loading fails
     */
    public static SimulationState load(String filePath) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            return (SimulationState) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Load failed: " + e.getMessage());
            return null;
        }
    }
}