package moldsim.model;

import java.io.Serializable;

public class ArchiveRoom implements Serializable {

    private static final long serialVersionUID = 1L;

    private String roomName;
    private Environment environment;

    private Wall northWall;
    private Wall southWall;
    private Wall eastWall;
    private Wall westWall;

    public ArchiveRoom(String roomName, Environment environment) {
        this.roomName = roomName;
        this.environment = environment;
    }

    public Wall getNorthWall() {
        return northWall;
    }

    public void setNorthWall(Wall northWall) {
        this.northWall = northWall;
    }

    public Wall getSouthWall() {
        return southWall;
    }

    public void setSouthWall(Wall southWall) {
        this.southWall = southWall;
    }

    public Wall getEastWall() {
        return eastWall;
    }

    public void setEastWall(Wall eastWall) {
        this.eastWall = eastWall;
    }

    public Wall getWestWall() {
        return westWall;
    }

    public void setWestWall(Wall westWall) {
        this.westWall = westWall;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }
}