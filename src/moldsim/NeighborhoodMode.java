package com.moldsim.model;
import java.io.Serializable;

/**
 *  comment une cellule voit ses voisins :
 * FOUR (Von Neumann : haut, bas, gauche, droite) et
 * EIGHT (Moore : ajoute les 4 diagonales)
 */
public enum NeighborhoodMode implements Serializable {
    FOUR,
    EIGHT
}
