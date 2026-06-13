package moldsim.model;

/**
 * Represents the importance level of a shelf in the simulation.
 * <p>
 * This value is used to prioritize protection and treatment of shelves
 * depending on their sensitivity or patrimonial importance.
 */
public enum ShelfValue {
    /** Low importance shelf. */
    LOW,

    /** Medium importance shelf. */
    MEDIUM,

    /** High importance shelf. */
    HIGH,

    /** Critical importance shelf requiring maximum protection. */
    CRITICAL;

    @Override
    public String toString() {
        return switch (this) {
            case LOW      -> "Low";
            case MEDIUM   -> "Medium";
            case HIGH     -> "High";
            case CRITICAL -> "Critical";
        };
    }
}