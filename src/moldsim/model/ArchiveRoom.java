
package moldsim.model;

import java.io.Serializable;

/**
 * Represents an archive room composed of four walls and an environmental context.
 * <p>
 * This class stores the room configuration and provides access to each wall
 * as well as shared environmental parameters used in the simulation.
 */
public class ArchiveRoom implements Serializable {

    /**
     * Serialization version identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Name of the archive room.
     */
    private String roomName;
    /**
     * Environmental conditions applied to the room.
     */
    private Environment environment;

    /** North wall of the room. */
    private Wall northWall;
    /** South wall of the room. */
    private Wall southWall;
    /** East wall of the room. */
    private Wall eastWall;
    /** West wall of the room. */
    private Wall westWall;

    /**
     * Creates an archive room with a name and environment.
     *
     * @param roomName name of the room
     * @param environment environmental conditions
     */
    public ArchiveRoom(String roomName, Environment environment) {
        this.roomName = roomName;
        this.environment = environment;
    }

    /**
     * Returns the north wall.
     *
     * @return north wall
     */
    public Wall getNorthWall() {
        return northWall;
    }

    /**
     * Sets the north wall.
     *
     * @param northWall wall to set
     */
    public void setNorthWall(Wall northWall) {
        this.northWall = northWall;
    }

    /**
     * Returns the south wall.
     *
     * @return south wall
     */
    public Wall getSouthWall() {
        return southWall;
    }

    /**
     * Sets the south wall.
     *
     * @param southWall wall to set
     */
    public void setSouthWall(Wall southWall) {
        this.southWall = southWall;
    }

    /**
     * Returns the east wall.
     *
     * @return east wall
     */
    public Wall getEastWall() {
        return eastWall;
    }

    /**
     * Sets the east wall.
     *
     * @param eastWall wall to set
     */
    public void setEastWall(Wall eastWall) {
        this.eastWall = eastWall;
    }

    /**
     * Returns the west wall.
     *
     * @return west wall
     */
    public Wall getWestWall() {
        return westWall;
    }

    /**
     * Sets the west wall.
     *
     * @param westWall wall to set
     */
    public void setWestWall(Wall westWall) {
        this.westWall = westWall;
    }

    /**
     * Returns the environment.
     *
     * @return environment
     */
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * Sets the environment.
     *
     * @param environment environmental conditions to set
     */
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    /**
     * Returns the room name.
     *
     * @return room name
     */
    public String getRoomName() {
        return roomName;
    }

    /**
     * Sets the room name.
     *
     * @param roomName name to set
     */
    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }
}