package moldsim;

/**
 * Les espèces de moisissure qui poussent sur un mur :
 * humidité/température/vitesse de croissance
 * CLADOSPORIUM : généraliste, premier à coloniser.
 * ASPERGILLUS  : agressif, aime la chaleur, tolère la sécheresse.
 * STACHYBOTRYS : moisissure noire, exige une très forte humidité.
 */

public enum MoldSpecies{

    CLADOSPORIUM(
            "Cladosporium",
            40.0,  // minHumidity
            10.0,  // minTemperature
            30.0,  // maxTemperature
            0.30,  // infectionProbability
            2.0    // moldGrowthPerStep
    ),
    ASPERGILLUS(
            "Aspergillus",
            30.0,
            20.0,
            40.0,
            0.40,
            2.5
    ),
    STACHYBOTRYS(
            "Stachybotrys",
            70.0,
            15.0,
            28.0,
            0.20,
            1.5
    );

    private final String displayName;
    private final double minHumidity;
    private final double minTemperature;
    private final double maxTemperature;
    private final double infectionProbability;
    private final double moldGrowthPerStep;

    MoldSpecies(String displayName,
                double minHumidity,
                double minTemperature,
                double maxTemperature,
                double infectionProbability,
                double moldGrowthPerStep) {
        this.displayName = displayName;
        this.minHumidity = minHumidity;
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
        this.infectionProbability = infectionProbability;
        this.moldGrowthPerStep = moldGrowthPerStep;
    }

    /** l'espèce : survivre ? se propager ? */
    public boolean canSurvive(double humidity, double temperature) {
        return humidity >= minHumidity
                && temperature >= minTemperature
                && temperature <= maxTemperature;
    }

    public String getDisplayName()         { return displayName; }
    public double getMinHumidity()         { return minHumidity; }
    public double getMinTemperature()      { return minTemperature; }
    public double getMaxTemperature()      { return maxTemperature; }
    public double getInfectionProbability(){ return infectionProbability; }
    public double getMoldGrowthPerStep()   { return moldGrowthPerStep; }
}
