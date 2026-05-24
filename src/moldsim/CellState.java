package com.moldsim.model;
import java.io.Serializable;

/**
 * États possibles de cellule 
 * HEALTHY (sans moisissure), INFECTED,
 * DEAD 
 */
public enum CellState implements Serializable {
    HEALTHY,
    INFECTED,
    DEAD
}
