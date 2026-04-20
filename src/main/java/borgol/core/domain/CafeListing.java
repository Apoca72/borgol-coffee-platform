package borgol.core.domain;

/**
 * CafeListing domain entity — physical café location in the Borgol platform.
 */
public class CafeListing {

    private int    id;
    private String name;
    private String address;
    private String district;
    private String city;
    private String phone;
    private String description;
    private String hours;
    private double avgRating;
    private int    ratingCount;
    private int    submittedBy;
    private String submittedByUsername;
    private int    currentUserRating;   // 0 = not rated
    private String currentUserReview;
    private String imageUrl;
    private String createdAt;
    private Double lat;   // GPS latitude  (nullable — null = no pin yet)
    private Double lng;   // GPS longitude (nullable)

    public CafeListing() {}

    // ── Getters ───────────────────────────────────────────────────────────────

    public int    getId()                  { return id; }
    public String getName()                { return name; }
    public String getAddress()             { return address; }
    public String getDistrict()            { return district; }
    public String getCity()                { return city; }
    public String getPhone()               { return phone; }
    public String getDescription()         { return description; }
    public String getHours()               { return hours; }
    public double getAvgRating()           { return avgRating; }
    public int    getRatingCount()         { return ratingCount; }
    public int    getSubmittedBy()         { return submittedBy; }
    public String getSubmittedByUsername() { return submittedByUsername; }
    public int    getCurrentUserRating()   { return currentUserRating; }
    public String getCurrentUserReview()   { return currentUserReview; }
    public String getImageUrl()            { return imageUrl; }
    public String getCreatedAt()           { return createdAt; }
    public Double getLat()                 { return lat; }
    public Double getLng()                 { return lng; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setId(int id)                              { this.id = id; }
    public void setName(String name)                       { this.name = name; }
    public void setAddress(String address)                 { this.address = address; }
    public void setDistrict(String district)               { this.district = district; }
    public void setCity(String city)                       { this.city = city; }
    public void setPhone(String phone)                     { this.phone = phone; }
    public void setDescription(String description)         { this.description = description; }
    public void setHours(String hours)                     { this.hours = hours; }
    public void setAvgRating(double avgRating)             { this.avgRating = avgRating; }
    public void setRatingCount(int ratingCount)            { this.ratingCount = ratingCount; }
    public void setSubmittedBy(int submittedBy)            { this.submittedBy = submittedBy; }
    public void setSubmittedByUsername(String username)    { this.submittedByUsername = username; }
    public void setCurrentUserRating(int rating)           { this.currentUserRating = rating; }
    public void setCurrentUserReview(String review)        { this.currentUserReview = review; }
    public void setImageUrl(String imageUrl)               { this.imageUrl = imageUrl; }
    public void setCreatedAt(String createdAt)             { this.createdAt = createdAt; }
    public void setLat(Double lat)                         { this.lat = lat; }
    public void setLng(Double lng)                         { this.lng = lng; }
}
