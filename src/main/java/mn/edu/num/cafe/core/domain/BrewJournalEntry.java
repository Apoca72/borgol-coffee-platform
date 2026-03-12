package mn.edu.num.cafe.core.domain;

public class BrewJournalEntry {
    private int id;
    private int userId;
    private String coffeeBean;
    private String origin;
    private String roastLevel;
    private String brewMethod;
    private String grindSize;
    private int waterTempC;
    private double doseGrams;
    private double yieldGrams;
    private int brewTimeSec;
    private int ratingAroma;
    private int ratingFlavor;
    private int ratingAcidity;
    private int ratingBody;
    private int ratingSweetness;
    private int ratingFinish;
    private String notes;
    private String createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getCoffeeBean() { return coffeeBean; }
    public void setCoffeeBean(String coffeeBean) { this.coffeeBean = coffeeBean; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getRoastLevel() { return roastLevel; }
    public void setRoastLevel(String roastLevel) { this.roastLevel = roastLevel; }

    public String getBrewMethod() { return brewMethod; }
    public void setBrewMethod(String brewMethod) { this.brewMethod = brewMethod; }

    public String getGrindSize() { return grindSize; }
    public void setGrindSize(String grindSize) { this.grindSize = grindSize; }

    public int getWaterTempC() { return waterTempC; }
    public void setWaterTempC(int waterTempC) { this.waterTempC = waterTempC; }

    public double getDoseGrams() { return doseGrams; }
    public void setDoseGrams(double doseGrams) { this.doseGrams = doseGrams; }

    public double getYieldGrams() { return yieldGrams; }
    public void setYieldGrams(double yieldGrams) { this.yieldGrams = yieldGrams; }

    public int getBrewTimeSec() { return brewTimeSec; }
    public void setBrewTimeSec(int brewTimeSec) { this.brewTimeSec = brewTimeSec; }

    public int getRatingAroma() { return ratingAroma; }
    public void setRatingAroma(int ratingAroma) { this.ratingAroma = ratingAroma; }

    public int getRatingFlavor() { return ratingFlavor; }
    public void setRatingFlavor(int ratingFlavor) { this.ratingFlavor = ratingFlavor; }

    public int getRatingAcidity() { return ratingAcidity; }
    public void setRatingAcidity(int ratingAcidity) { this.ratingAcidity = ratingAcidity; }

    public int getRatingBody() { return ratingBody; }
    public void setRatingBody(int ratingBody) { this.ratingBody = ratingBody; }

    public int getRatingSweetness() { return ratingSweetness; }
    public void setRatingSweetness(int ratingSweetness) { this.ratingSweetness = ratingSweetness; }

    public int getRatingFinish() { return ratingFinish; }
    public void setRatingFinish(int ratingFinish) { this.ratingFinish = ratingFinish; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
