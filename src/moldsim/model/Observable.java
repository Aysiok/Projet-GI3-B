package moldsim.model;

/**
 * Observable interface for the Observer design pattern.
 * Any class that can be observed must implement this interface.
 */
public interface Observable {

    /**
     * Adds an observer to the notification list.
     * @param observer the observer to add
     */
    void addObserver(Observer observer);

    /**
     * Removes an observer from the notification list.
     * @param observer the observer to remove
     */
    void removeObserver(Observer observer);

    /**
     * Notifies all registered observers with a message.
     * @param message a description of the event
     */
    void notifyObservers(String message);
}