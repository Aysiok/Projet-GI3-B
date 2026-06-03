package moldsim.model;

/**
 * Observer interface for the Observer design pattern.
 * Any class that wants to receive alerts must implement this interface.
 */
public interface Observer {

    /**
     * Called when an observable notifies its observers.
     * @param source  the object that triggered the notification
     * @param message a description of the event
     */
    void update(Object source, String message);
}