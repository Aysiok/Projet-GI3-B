package moldsim.model;

import java.io.Serializable;

/**
 * Global environmental conditions of the archive room.
 * <p>
 * This class defines humidity, temperature, and ventilation values
 * used by the simulation to compute mold growth conditions.
 */
public class Environment implements Serializable {

    /**
     * Serialization version identifier.
     */
    private static final long serialVersionUID = 1L;

    private static final double HUMIDITY_DEFAULT = 50.0;
    private static final double TEMPERATURE_DEFAULT = 20.0;
    private static final double VENTILATION_DEFAULT = 50.0;

    /** Humidity level (0 to 100). */
    private double humidity;

    /** Temperature in degrees Celsius. */
    private double temperature;

    /** Ventilation level (0 to 100). */
    private double ventilation;

    /**
     * Creates an environment with default values.
     */
    public Environment() {
        this.humidity = HUMIDITY_DEFAULT;
        this.temperature = TEMPERATURE_DEFAULT;
        this.ventilation = VENTILATION_DEFAULT;
    }


    /**
     * Returns humidity level.
     *
     * @return humidity
     */
    public double getHumidity() {
        return humidity;
    }

    /**
     * Returns temperature.
     *
     * @return temperature
     */
    public double getTemperature() {
        return temperature;
    }

    /**
     * Returns ventilation level.
     *
     * @return ventilation
     */
    public double getVentilation() {
        return ventilation;
    }

    /**
     * Sets humidity level (clamped between 0 and 100).
     *
     * @param humidity humidity value
     */
    public void setHumidity(double humidity) {
        this.humidity = clamp(humidity);
    }

    /**
     * Sets temperature.
     *
     * @param temperature temperature value
     */
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    /**
     * Sets ventilation level (clamped between 0 and 100).
     *
     * @param ventilation ventilation value
     */
    public void setVentilation(double ventilation) {
        this.ventilation = clamp(ventilation);
    }

    /**
     * Clamps a value between 0 and 100.
     *
     * @param value input value
     * @return clamped value
     */
    private double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }

    /**
     * Returns a string representation of the environment.
     *
     * @return formatted environment string
     */
    @Override
    public String toString() {
        return "Environment{" + "humidity=" + humidity + ", temperature=" + temperature + ", ventilation=" + ventilation +'}';
    }

}