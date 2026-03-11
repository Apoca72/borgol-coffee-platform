package mn.edu.num.cafe.app;

import mn.edu.num.cafe.core.application.MenuDto;
import mn.edu.num.cafe.core.application.MenuService;
import mn.edu.num.cafe.core.domain.MenuCategory;
import mn.edu.num.cafe.core.ports.IMenuRepository;
import mn.edu.num.cafe.infrastructure.persistence.RepositoryFactory;

/**
 * Программын орох цэг — Composition Root.
 *
 * Архитектурын дүрмүүд:
 *   1. Объектуудыг зөвхөн ЭНД холбоно (wiring / dependency injection).
 *   2. UI / console нь MenuService-г л дуудна — DAO/Repository шууд дуудахгүй.
 *   3. RepositoryFactory → IMenuRepository буцаана (бетон класс биш).
 *   4. Observer-ийг ЭНД бүртгэнэ (ConsoleMenuObserver).
 */
public class Main {

    public static void main(String[] args) {

        // ── 1. Repository: config-driven Factory ────────────────────────────
        IMenuRepository repository = RepositoryFactory.createMenuRepository();

        // ── 2. Service: Dependency Injection ─────────────────────────────────
        MenuService menuService = new MenuService(repository);

        // ── 3. Observer: register console listener ───────────────────────────
        menuService.addObserver(new ConsoleMenuObserver());

        // ── 4. Demo: CRUD operations ──────────────────────────────────────────
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   MyCafé — Coffee Shop Menu Management   ║");
        System.out.println("╚══════════════════════════════════════════╝\n");

        System.out.println(">>> Adding menu items...");
        MenuDto espresso  = menuService.addItem("Espresso",         MenuCategory.COFFEE,   3.50);
        MenuDto latte     = menuService.addItem("Caffe Latte",      MenuCategory.COFFEE,   4.50);
        MenuDto matcha    = menuService.addItem("Matcha Latte",     MenuCategory.TEA,      4.00);
        MenuDto croissant = menuService.addItem("Butter Croissant", MenuCategory.FOOD,     3.00);
        MenuDto tiramisu  = menuService.addItem("Tiramisu",         MenuCategory.DESSERT,  5.50);

        printMenu("--- Full Menu ---", menuService);

        System.out.println("\n>>> Updating Espresso price to $3.75...");
        menuService.updateItem(espresso.id(), "Espresso Shot", MenuCategory.COFFEE, 3.75, true);

        System.out.println("\n>>> Removing Butter Croissant (sold out)...");
        menuService.removeItem(croissant.id());

        printMenu("--- Final Menu ---", menuService);
    }

    private static void printMenu(String header, MenuService menuService) {
        System.out.println("\n" + header);
        System.out.printf("  %-4s %-22s %-10s %6s  %s%n",
                "ID", "Name", "Category", "Price", "Available");
        System.out.println("  " + "─".repeat(58));
        for (MenuDto dto : menuService.getAllItems()) {
            System.out.printf("  %-4d %-22s %-10s $%5.2f  %s%n",
                    dto.id(), dto.name(), dto.category(),
                    dto.price(), dto.available() ? "✓" : "✗");
        }
    }
}
