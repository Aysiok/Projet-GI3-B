package moldsim.model;

/**
 * Represents the patrimonial value of a document in the archive.
 * Used to prioritize which documents to save first.
 */
public enum ShelfValue {
    LOW,
    MEDIUM,
    HIGH,
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