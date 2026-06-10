package moldsim.model;

/**
 * Represents the current room and wall displayed by the simulation grid.
 */
public class LocationContext {

    private String roomName;
    private String wallName;

    public LocationContext(String roomName, String wallName) {
        this.roomName = roomName;
        this.wallName = wallName;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getWallName() {
        return wallName;
    }

    public void setRoomName(String roomName) {
        if (roomName != null && !roomName.isBlank()) {
            this.roomName = roomName;
        }
    }

    public void setWallName(String wallName) {
        if (wallName != null && !wallName.isBlank()) {
            this.wallName = wallName;
        }
    }

    public String getDisplayName() {
        return roomName + " — " + wallName;
    }
}