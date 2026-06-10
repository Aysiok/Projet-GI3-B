package moldsim.model;

public class SensorEvent {

    private final int week;
    private final AlertLevel alertLevel;
    private final double moldRate;
    private final Wall wall;
    private final EventType type;
    private final Shelf shelf; // null si GLOBAL

    public SensorEvent(int week, AlertLevel alertLevel, double moldRate, Wall wall, EventType type, Shelf shelf) {
        this.week       = week;
        this.alertLevel = alertLevel;
        this.moldRate   = moldRate;
        this.wall       = wall;
        this.type       = type;
        this.shelf      = shelf;
    }

    public SensorEvent(int week, AlertLevel alertLevel, double moldRate, Wall wall) {
        this(week, alertLevel, moldRate, wall, EventType.GLOBAL, null);
    }

    public int getWeek(){ 
        return week;
    
    }
    public AlertLevel getAlertLevel(){
        return alertLevel;
    }
    
    public double getMoldRate(){
        return moldRate;
    }

    public Wall getWall(){
        return wall;
    }

    public EventType getType(){
        return type;
    }

    public Shelf getShelf(){
        return shelf;
    }

}