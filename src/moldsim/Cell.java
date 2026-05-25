package moldsim;
import java.io.Serializable;

/**
 * partie d'asma
Une cellule du mur correspond à un simple conteneur de données
**/
public class Cell implements Serializable {
    private static final long serialVersionUID = 1L;

    // Position (fixe)
    private final int x;
    private final int y;

    // Environnement
    private double humidity;      // 0..100 (%)
    private double temperature;   // en °C

    // Biologie
    private double moldLevel;     // 0..100
    private int age;              // nombre de pas survécus
    private CellState state;
    private MoldSpecies species;  // null si HEALTHY ou DEAD

    /** Crée  cellule saine aux coordonnées qu'on donne*/
    public Cell(int x, int y, double humidity, double temperature) {
        this.x = x;
        this.y = y;
        this.humidity = humidity;
        this.temperature = temperature;
        this.moldLevel = 0.0;
        this.age = 0;
        this.state = CellState.HEALTHY;
        this.species = null;
    }


    public boolean isInfected() {
        return state == CellState.INFECTED;
    }

    public boolean isAlive() {
        return state != CellState.DEAD;
    }

    /** Infecte la cellule même si elle est morte */
    public void infect(MoldSpecies species) {
        if (species == null) {
            throw new IllegalArgumentException("Species cannot be null");
        }
        if (state == CellState.DEAD) return;
        this.state = CellState.INFECTED;
        this.species = species;
        if (this.moldLevel < 1.0) {
            this.moldLevel = 1.0;
        }
    }

    /** Tue la cellule */
    public void kill() {
        this.state = CellState.DEAD;
        this.species = null;
    }

    /** etat sain */
    public void cure() {
        if (state == CellState.DEAD) return;
        this.state = CellState.HEALTHY;
        this.species = null;
        this.moldLevel = 0.0;
    }

    public void incrementAge() {
        this.age++;
    }


    public int getX() { return x; }
    public int getY() { return y; }

    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) {
        this.humidity = clamp(humidity);  
    }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getMoldLevel() { return moldLevel; }
    public void setMoldLevel(double moldLevel) {
        this.moldLevel = clamp(moldLevel);
    }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public CellState getState() { return state; }
    public void setState(CellState state) {
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null");
        }
        this.state = state;
    }

    public MoldSpecies getSpecies() { return species; }
    public void setSpecies(MoldSpecies species) { this.species = species; }

    /** Borne une valeur dans [0, 100]. */
    private static double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }

    @Override
    public String toString() {
        return "Cell(" + x + "," + y + ", " + state
                + (species != null ? " [" + species.name() + "]" : "")
                + ", hum=" + String.format("%.1f", humidity)
                + ", temp=" + String.format("%.1f", temperature)
                + ", mold=" + String.format("%.1f", moldLevel)
                + ", age=" + age + ")";
    }
}
