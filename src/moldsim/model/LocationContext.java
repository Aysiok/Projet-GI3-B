package moldsim.model;

/**
 * Represents the currently selected room and wall in the simulation UI.
 * <p>
 * This context is used to determine which part of the simulation grid is
 * displayed and interacted with by the user.
 */
public class LocationContext {

    /** Name of the current room. */
    private String roomName;
    /** Name of the current wall. */
    private String wallName;

    /**
     * Creates a location context for a given room and wall.
     *
     * @param roomName name of the room
     * @param wallName name of the wall
     */
    public LocationContext(String roomName, String wallName) {
        this.roomName = roomName;
        this.wallName = wallName;
    }

    /**
     * Returns the current room name.
     *
     * @return room name
     */
    public String getRoomName() {
        return roomName;
    }

    /**
     * Returns the current wall name.
     *
     * @return wall name
     */
    public String getWallName() {
        return wallName;
    }

    /**
     * Sets the current room name if valid.
     *
     * @param roomName new room name
     */
    public void setRoomName(String roomName) {
        if (roomName != null && !roomName.isBlank()) {
            this.roomName = roomName;
        }
    }

    /**
     * Sets the current wall name if valid.
     *
     * @param wallName new wall name
     */
    public void setWallName(String wallName) {
        if (wallName != null && !wallName.isBlank()) {
            this.wallName = wallName;
        }
    }

    /**
     * Returns a formatted display name combining room and wall.
     *
     * @return formatted display string
     */
    public String getDisplayName() {
        return roomName + " — " + wallName;
    }
}