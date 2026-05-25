package moldsim.model;

import java.io.Serializable;

/**
 * Conditions globales du mur / de la pièce.
 * Affecte toute la simulation.
 */

public class Environment implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final double HUMIDITY_DEFAULT = 50.0;
    private static final double TEMPERATURE_DEFAULT = 20.0;
    private static final double VENTILATION_DEFAULT = 50.0;
    private static final WallMaterial MATERIAL_DEFAULT = WallMaterial.PLASTER;

    private double humidity;        // 0 - 100 %
    private double temperature;     // °C
    private double ventilation;     // 0 - 100 %
    private WallMaterial material;

    public Environment() { //placeholder pour l'instant, à voir après de vraies valeurs par défaut
        this.humidity = HUMIDITY_DEFAULT;
        this.temperature = TEMPERATURE_DEFAULT;
        this.ventilation = VENTILATION_DEFAULT;
        this.material = MATERIAL_DEFAULT;
    }


    public double getHumidity() {
        return humidity;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getVentilation() {
        return ventilation;
    }

    public WallMaterial getMaterial() {
        return material;
    }

    public void setHumidity(double humidity) {
        this.humidity = clamp(humidity);
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public void setVentilation(double ventilation) {
        this.ventilation = clamp(ventilation);
    }

    public void setMaterial(WallMaterial material) {
        this.material = material;
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }

    @Override
    public String toString() {
        return "Environment{" +
                "humidity=" + humidity +
                ", temperature=" + temperature +
                ", ventilation=" + ventilation +
                ", material=" + material +
                '}';
    }

}