package mn.edu.num.cafe.core.domain;

/**
 * Цэсний зүйлийн домэйн объект (Entity).
 *
 * Архитектурын дүрэм: core/domain/ — java.sql.*, javax.swing.* импорт хатуу хориглоно.
 * Энэ класс нь цэвэр Java, ямар ч гадны хамаарал байхгүй.
 */
public class MenuItem {

    private final int    id;
    private String       name;
    private MenuCategory category;
    private double       price;
    private boolean      available;

    public MenuItem(int id, String name, MenuCategory category, double price, boolean available) {
        this.id        = id;
        this.name      = name;
        this.category  = category;
        this.price     = price;
        this.available = available;
    }

    public int          getId()       { return id; }
    public String       getName()     { return name; }
    public MenuCategory getCategory() { return category; }
    public double       getPrice()    { return price; }
    public boolean      isAvailable() { return available; }

    public void setName(String name)            { this.name      = name; }
    public void setCategory(MenuCategory cat)   { this.category  = cat; }
    public void setPrice(double price)          { this.price     = price; }
    public void setAvailable(boolean available) { this.available = available; }

    @Override
    public String toString() {
        return String.format("MenuItem{id=%d, name='%s', category=%s, price=%.2f, available=%b}",
                id, name, category, price, available);
    }
}
