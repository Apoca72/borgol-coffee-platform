package mn.edu.num.cafe.core.application;

import mn.edu.num.cafe.core.domain.MenuCategory;
import mn.edu.num.cafe.core.domain.MenuItem;
import mn.edu.num.cafe.core.ports.IMenuRepository;
import mn.edu.num.cafe.core.ports.MenuChangeObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Цэсний бизнес логикийн үйлчилгээ (Application Service).
 *
 * Загвар: Observer — observer-уудыг бүртгэж, CRUD үйлдэл бүрт мэдэгдэл илгээнэ.
 *
 * Архитектурын дүрэм:
 *   - Зөвхөн IMenuRepository портоор репозитортой харилцана.
 *   - java.sql.*, javax.swing.* импорт хориглоно.
 */
public class MenuService {

    private final IMenuRepository            repository;
    private final List<MenuChangeObserver>   observers = new ArrayList<>();

    public MenuService(IMenuRepository repository) {
        this.repository = repository;
    }

    // ── Observer бүртгэл ─────────────────────────────────────────────────────

    public void addObserver(MenuChangeObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(MenuChangeObserver observer) {
        observers.remove(observer);
    }

    // ── CRUD үйлдлүүд ────────────────────────────────────────────────────────

    /**
     * Шинэ цэсний зүйл нэмнэ (available горимыг тодорхойлж болно).
     * @throws IllegalArgumentException  нэр хоосон эсвэл үнэ сөрөг бол
     */
    public MenuDto addItem(String name, MenuCategory category, double price, boolean available) {
        validateInput(name, price);
        int nextId = repository.findAll().stream()
                .mapToInt(MenuItem::getId).max().orElse(0) + 1;
        MenuItem item = new MenuItem(nextId, name, category, price, available);
        repository.save(item);
        observers.forEach(o -> o.onItemAdded(item));
        return toDto(item);
    }

    /** Backward-compatible: available default нь true. */
    public MenuDto addItem(String name, MenuCategory category, double price) {
        return addItem(name, category, price, true);
    }

    /** Ангилалаар шүүж буцаана (server-side filtering). */
    public List<MenuDto> getItemsByCategory(MenuCategory category) {
        return repository.findAll().stream()
                .filter(item -> item.getCategory() == category)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Одоо байгаа цэсний зүйлийг шинэчилнэ.
     * @throws IllegalArgumentException  нэр хоосон, үнэ сөрөг, эсвэл ID олдохгүй бол
     */
    public MenuDto updateItem(int id, String name, MenuCategory category,
                              double price, boolean available) {
        validateInput(name, price);
        MenuItem item = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found: id=" + id));
        item.setName(name);
        item.setCategory(category);
        item.setPrice(price);
        item.setAvailable(available);
        repository.save(item);
        observers.forEach(o -> o.onItemUpdated(item));
        return toDto(item);
    }

    /** Цэсний зүйлийг устгана. */
    public void removeItem(int id) {
        repository.deleteById(id);
        observers.forEach(o -> o.onItemDeleted(id));
    }

    /** Бүх цэсний зүйлийг DTO хэлбэрээр буцаана. */
    public List<MenuDto> getAllItems() {
        return repository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /** ID-аар хайж DTO буцаана. */
    public Optional<MenuDto> getItemById(int id) {
        return repository.findById(id).map(this::toDto);
    }

    // ── Туслах методууд ───────────────────────────────────────────────────────

    private void validateInput(String name, double price) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Item name cannot be empty.");
        }
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }
    }

    private MenuDto toDto(MenuItem item) {
        return new MenuDto(item.getId(), item.getName(), item.getCategory(),
                item.getPrice(), item.isAvailable());
    }
}
