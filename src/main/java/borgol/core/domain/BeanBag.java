package borgol.core.domain;

public class BeanBag {
    private int id;
    private int userId;
    private String name;
    private String roaster;
    private String origin;
    private String roastLevel;   // LIGHT, MEDIUM, MEDIUM-DARK, DARK
    private String roastDate;    // ISO date string, nullable
    private double remainingGrams;
    private int rating;          // 1-5, 0 = unrated
    private String notes;
    private String createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRoaster() { return roaster; }
    public void setRoaster(String roaster) { this.roaster = roaster; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getRoastLevel() { return roastLevel; }
    public void setRoastLevel(String roastLevel) { this.roastLevel = roastLevel; }
    public String getRoastDate() { return roastDate; }
    public void setRoastDate(String roastDate) { this.roastDate = roastDate; }
    public double getRemainingGrams() { return remainingGrams; }
    public void setRemainingGrams(double g) { this.remainingGrams = g; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
