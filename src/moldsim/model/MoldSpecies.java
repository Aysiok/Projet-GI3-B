package moldsim.model;

/**
 * Represents mold species used in the simulation.
 * <p>
 * Each species defines environmental requirements and growth characteristics
 * such as humidity tolerance, temperature range, infection probability,
 * and growth speed.
 */
public enum MoldSpecies{

    /** Generalist species, first colonizer in most conditions. */
    CLADOSPORIUM(
            "Cladosporium",
            40.0,  // minHumidity
            10.0,  // minTemperature
            30.0,  // maxTemperature
            0.30,  // infectionProbability
            2.0    // moldGrowthPerStep
    ),

    /** Aggressive species favoring warm and relatively dry environments. */
    ASPERGILLUS(
            "Aspergillus",
            30.0,
            20.0,
            40.0,
            0.40,
            2.5
    ),

    /** Black mold species requiring high humidity conditions. */
    STACHYBOTRYS(
            "Stachybotrys",
            70.0,
            15.0,
            28.0,
            0.20,
            1.5
    );

    /** Human-readable name of the species. */
    private final String displayName;
    /** Minimum humidity required for survival. */
    private final double minHumidity;
    /** Minimum temperature for growth. */
    private final double minTemperature;
    /** Maximum temperature for growth. */
    private final double maxTemperature;
    /** Probability of infecting a cell under suitable conditions. */
    private final double infectionProbability;
    /** Growth rate per simulation step. */
    private final double moldGrowthPerStep;

    /**
     * Creates a mold species with its environmental parameters.
     *
     * @param displayName species name
     * @param minHumidity minimum humidity
     * @param minTemperature minimum temperature
     * @param maxTemperature maximum temperature
     * @param infectionProbability infection probability
     * @param moldGrowthPerStep growth per simulation step
     */
    MoldSpecies(String displayName, double minHumidity, double minTemperature, double maxTemperature, double infectionProbability, double moldGrowthPerStep) {
        this.displayName = displayName;
        this.minHumidity = minHumidity;
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
        this.infectionProbability = infectionProbability;
        this.moldGrowthPerStep = moldGrowthPerStep;
    }

    /**
     * Determines whether the species can survive under given conditions.
     *
     * @param humidity current humidity
     * @param temperature current temperature
     * @return true if conditions are suitable
     */
    public boolean canSurvive(double humidity, double temperature) {
        return humidity >= minHumidity && temperature >= minTemperature && temperature <= maxTemperature;
    }

    /**
     * Returns the human-readable name of the species.
     *
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the minimum humidity required for survival.
     *
     * @return minimum humidity
     */
    public double getMinHumidity() {
        return minHumidity;
    }

    /**
     * Returns the minimum temperature for growth.
     *
     * @return minimum temperature
     */
    public double getMinTemperature() {
        return minTemperature;
    }

    /**
     * Returns the maximum temperature for growth.
     *
     * @return maximum temperature
     */
    public double getMaxTemperature() {
        return maxTemperature;
    }

    /**
     * Returns the probability of infecting a cell under suitable conditions.
     *
     * @return infection probability
     */
    public double getInfectionProbability() {
        return infectionProbability;
    }

    /**
     * Returns the growth rate per simulation step.
     *
     * @return growth per step
     */
    public double getMoldGrowthPerStep() {
        return moldGrowthPerStep;
    }
}
