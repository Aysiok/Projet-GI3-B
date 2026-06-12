package moldsim.model;

/**
 * États possibles de cellule 
 * HEALTHY (sans moisissure), INFECTED,
 * DEAD 
 */
public enum CellState{
    HEALTHY,
    DEPOSITED_SPORE,
    INFECTED,
    SPORULATING,
    DEAD
}
