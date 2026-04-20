package borgol;

import borgol.core.application.MenuDto;
import borgol.core.application.MenuService;
import borgol.core.domain.MenuCategory;
import borgol.core.domain.MenuItem;
import borgol.core.ports.MenuChangeObserver;
import borgol.infrastructure.persistence.InMemoryMenuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MenuServiceTest {

    private MenuService menuService;

    @BeforeEach
    void setUp() {
        menuService = new MenuService(new InMemoryMenuRepository());
    }

    @Test
    void addItem_savesAndReturnsDto() {
        MenuDto dto = menuService.addItem("Espresso", MenuCategory.COFFEE, 3.50);

        assertNotNull(dto);
        assertEquals("Espresso", dto.name());
        assertEquals(MenuCategory.COFFEE, dto.category());
        assertEquals(3.50, dto.price());
        assertTrue(dto.available());
    }

    @Test
    void addItem_emptyName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> menuService.addItem("", MenuCategory.COFFEE, 3.50));
    }

    @Test
    void addItem_negativePrice_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> menuService.addItem("Latte", MenuCategory.COFFEE, -1.0));
    }

    @Test
    void updateItem_changesFieldsCorrectly() {
        MenuDto original = menuService.addItem("Espresso", MenuCategory.COFFEE, 3.50);
        MenuDto updated  = menuService.updateItem(
                original.id(), "Espresso Shot", MenuCategory.COFFEE, 3.75, true);

        assertEquals("Espresso Shot", updated.name());
        assertEquals(3.75, updated.price());
    }

    @Test
    void removeItem_deletesFromRepository() {
        MenuDto dto = menuService.addItem("Croissant", MenuCategory.FOOD, 2.50);
        menuService.removeItem(dto.id());

        List<MenuDto> all = menuService.getAllItems();
        assertTrue(all.stream().noneMatch(d -> d.id() == dto.id()));
    }

    @Test
    void observer_notifiedOnAddUpdateDelete() {
        int[] counts = {0, 0, 0}; // [added, updated, deleted]

        menuService.addObserver(new MenuChangeObserver() {
            @Override public void onItemAdded(MenuItem item)   { counts[0]++; }
            @Override public void onItemUpdated(MenuItem item) { counts[1]++; }
            @Override public void onItemDeleted(int id)        { counts[2]++; }
        });

        MenuDto dto = menuService.addItem("Matcha", MenuCategory.TEA, 4.00);
        menuService.updateItem(dto.id(), "Matcha Latte", MenuCategory.TEA, 4.50, true);
        menuService.removeItem(dto.id());

        assertEquals(1, counts[0], "onItemAdded should fire once");
        assertEquals(1, counts[1], "onItemUpdated should fire once");
        assertEquals(1, counts[2], "onItemDeleted should fire once");
    }
}
