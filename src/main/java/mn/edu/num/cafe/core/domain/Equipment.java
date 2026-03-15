package mn.edu.num.cafe.core.domain;

/**
 * Equipment domain entity — a piece of brewing equipment tracked by a user.
 */
public class Equipment {

    private int    id;
    private int    userId;
    private String category;   // GRINDER, BREWER, KETTLE, SCALE, TAMPER, MACHINE, OTHER
    private String name;
    private String brand;
    private String notes;
    private String createdAt;

    public Equipment() {}

    public int    getId()        { return id; }
    public int    getUserId()    { return userId; }
    public String getCategory()  { return category; }
    public String getName()      { return name; }
    public String getBrand()     { return brand; }
    public String getNotes()     { return notes; }
    public String getCreatedAt() { return createdAt; }

    public void setId(int id)               { this.id = id; }
    public void setUserId(int userId)       { this.userId = userId; }
    public void setCategory(String category){ this.category = category; }
    public void setName(String name)        { this.name = name; }
    public void setBrand(String brand)      { this.brand = brand; }
    public void setNotes(String notes)      { this.notes = notes; }
    public void setCreatedAt(String c)      { this.createdAt = c; }
}
