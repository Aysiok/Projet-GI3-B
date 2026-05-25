package moldsim.model;

/**
 *  comment une cellule voit ses voisins :
 * FOUR (Von Neumann : haut, bas, gauche, droite) et
 * EIGHT (Moore : ajoute les 4 diagonales)
 */
public enum NeighborhoodMode{
    FOUR,
    EIGHT
}
