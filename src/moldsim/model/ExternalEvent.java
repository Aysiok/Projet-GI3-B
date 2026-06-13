package moldsim.model;

public enum ExternalEvent {
    WATER_LEAK,        // augmente fortement l'humidité localement
    HVAC_FAILURE,      // coupe la ventilation
    WINDOW_OPENED,     // augmente la ventilation temporairement
    ANTI_MOLD_TREATMENT_WALL,   // traite une zone du mur
    ANTI_MOLD_TREATMENT_SHELF   // traite une étagère
}
