package moldsim.controller;

import moldsim.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimulationController {

    private final ArchiveRoom room;
    private final List<MoldSensor> sensors;
    private final AlertController alertController;
    private int currentWeek;

    public SimulationController(ArchiveRoom room, Map<Wall, List<Shelf>> shelvesByWall) {
    this.room            = room;
    this.sensors         = new ArrayList<>();
    this.alertController = new AlertController();
    this.currentWeek     = 0;
    initSensors(shelvesByWall);
    alertController.setRecommendationEngine(new RecommendationEngine(room));
}

    private void initSensors(Map<Wall, List<Shelf>> shelvesByWall) {
        if (room.getNorthWall() != null) sensors.add(new MoldSensor(room.getNorthWall(),
                shelvesByWall.getOrDefault(room.getNorthWall(), List.of())));
        if (room.getSouthWall() != null) sensors.add(new MoldSensor(room.getSouthWall(),
                shelvesByWall.getOrDefault(room.getSouthWall(), List.of())));
        if (room.getEastWall()  != null) sensors.add(new MoldSensor(room.getEastWall(),
                shelvesByWall.getOrDefault(room.getEastWall(),  List.of())));
        if (room.getWestWall()  != null) sensors.add(new MoldSensor(room.getWestWall(),
                shelvesByWall.getOrDefault(room.getWestWall(),  List.of())));
    }

    public void tick() {
        currentWeek++;
        // propagation moisissure ici
        pollSensors();
    }

    private void pollSensors() {
        for (MoldSensor sensor : sensors) {
            sensor.poll(currentWeek).forEach(alertController::handle);
        }
    }

    public int getCurrentWeek()                   { return currentWeek; }
    public AlertController getAlertController()   { return alertController; }
    public List<SensorEvent> getHistory()         { return alertController.getHistory(); }
}