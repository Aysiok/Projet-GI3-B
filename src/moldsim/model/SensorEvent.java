package moldsim.model;

public class SensorEvent {

    private final int week;
    private final AlertLevel alertLevel;
    private final double moldRate;
    private final Wall wall;

    public SensorEvent(int week, AlertLevel alertLevel, double moldRate, Wall wall) {
        this.week       = week;
        this.alertLevel = alertLevel;
        this.moldRate   = moldRate;
        this.wall       = wall;
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
}
