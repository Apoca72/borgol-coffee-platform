package mn.edu.num.cafe.app;

import mn.edu.num.cafe.core.application.MenuService;
import mn.edu.num.cafe.core.domain.MenuCategory;
import mn.edu.num.cafe.core.ports.IMenuRepository;
import mn.edu.num.cafe.infrastructure.persistence.RepositoryFactory;
import mn.edu.num.cafe.ui.web.CafeApiServer;

/**
 * Программын орох цэг — Composition Root.
 *
 * Архитектурын дүрмүүд:
 *   1. Бүх объектуудыг зөвхөн ЭНД холбоно (wiring / dependency injection).
 *   2. RepositoryFactory → IMenuRepository буцаана (бетон класс биш).
 *   3. ConsoleMenuObserver → Observer pattern demo (browser request бүрт ажиллана).
 *   4. CafeApiServer → Inbound Web Adapter (hexagonal architecture).
 *   5. Main класс нь ямар ч бизнес логик агуулахгүй.
 */
public class Main {

    private static final int PORT = 7000;

    public static void main(String[] args) {

        // ── 1. Repository: config-driven Factory ────────────────────────────
        IMenuRepository repository = RepositoryFactory.createMenuRepository();

        // ── 2. Service: Dependency Injection ─────────────────────────────────
        MenuService menuService = new MenuService(repository);

        // ── 3. Observer: console prints every API call made from browser ──────
        menuService.addObserver(new ConsoleMenuObserver());

        // ── 4. Seed: default items if DB is empty ────────────────────────────
        if (menuService.getAllItems().isEmpty()) {
            System.out.println("  [SEED] Empty database — seeding default menu...");
            menuService.addItem("Espresso",          MenuCategory.COFFEE,   3.50);
            menuService.addItem("Caffe Latte",        MenuCategory.COFFEE,   4.50);
            menuService.addItem("Cappuccino",         MenuCategory.COFFEE,   4.00);
            menuService.addItem("Matcha Latte",       MenuCategory.TEA,      4.00);
            menuService.addItem("Chamomile Tea",      MenuCategory.TEA,      3.00);
            menuService.addItem("Mango Smoothie",     MenuCategory.SMOOTHIE, 5.00);
            menuService.addItem("Butter Croissant",   MenuCategory.FOOD,     3.50);
            menuService.addItem("Avocado Toast",      MenuCategory.FOOD,     6.50);
            menuService.addItem("Tiramisu",           MenuCategory.DESSERT,  5.50);
            menuService.addItem("Chocolate Brownie",  MenuCategory.DESSERT,  4.00);
        }

        // ── 5. Web Server: Inbound Adapter (REST API + static UI) ────────────
        CafeApiServer server = new CafeApiServer(menuService);
        server.start(PORT);
    }
}
