package borgol.core.domain;

public class UserBean {
    private int id;
    private int userId;
    private String name;
    private String origin;
    private String roastLevel;
    private double weightG;
    private String purchaseDate;
    private String roastedDate;
    private String notes;
    private String createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getRoastLevel() { return roastLevel; }
    public void setRoastLevel(String roastLevel) { this.roastLevel = roastLevel; }

    public double getWeightG() { return weightG; }
    public void setWeightG(double weightG) { this.weightG = weightG; }

    public String getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }

    public String getRoastedDate() { return roastedDate; }
    public void setRoastedDate(String roastedDate) { this.roastedDate = roastedDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
