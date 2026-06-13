
package moldsim.controller;

/**
 * Interface responsible for logging alert-related messages.
 * Implementations may write messages to the console, a file,
 * a database, or any other logging destination.
 */
public interface AlertLogger {
    /**
     * Records a log message.
     *
     * @param message the message to record
     */
    void log(String message);
}