package mn.edu.num.cafe.app;

import mn.edu.num.cafe.core.application.BorgolService;
import mn.edu.num.cafe.core.application.MenuService;
import mn.edu.num.cafe.core.domain.MenuCategory;
import mn.edu.num.cafe.core.ports.IMenuRepository;
import mn.edu.num.cafe.infrastructure.config.DatabaseConnection;
import mn.edu.num.cafe.infrastructure.persistence.BorgolRepository;
import mn.edu.num.cafe.infrastructure.persistence.RepositoryFactory;
import mn.edu.num.cafe.ui.desktop.BorgolApp;

/**
 * Application entry point — Composition Root.
 *
 * Wires together:
 *   - DatabaseConnection (Singleton)
 *   - BorgolRepository   → BorgolService   → BorgolApp (JavaFX desktop)
 *   - IMenuRepository    → MenuService      (legacy, kept for backward compat)
 */
public class Main {

    public static void main(String[] args) {

        // ── 1. Database connection (shared singleton) ────────────────────────
        DatabaseConnection db = DatabaseConnection.getInstance();

        // ── 2. Borgol platform: repository + service ─────────────────────────
        BorgolRepository borgolRepo    = new BorgolRepository(db);
        BorgolService    borgolService = new BorgolService(borgolRepo);

        // ── 3. Legacy menu management ─────────────────────────────────────────
        IMenuRepository menuRepo    = RepositoryFactory.createMenuRepository();
        MenuService     menuService = new MenuService(menuRepo);
        menuService.addObserver(new ConsoleMenuObserver());

        // ── 4. Seed menu items if empty ───────────────────────────────────────
        if (menuService.getAllItems().isEmpty()) {
            menuService.addItem("Espresso",         MenuCategory.COFFEE,   3.50);
            menuService.addItem("Caffe Latte",       MenuCategory.COFFEE,   4.50);
            menuService.addItem("Cappuccino",        MenuCategory.COFFEE,   4.00);
            menuService.addItem("Cold Brew",         MenuCategory.COFFEE,   5.00);
            menuService.addItem("Matcha Latte",      MenuCategory.TEA,      4.00);
            menuService.addItem("Chamomile Tea",     MenuCategory.TEA,      3.00);
            menuService.addItem("Mango Smoothie",    MenuCategory.SMOOTHIE, 5.00);
            menuService.addItem("Butter Croissant",  MenuCategory.FOOD,     3.50);
            menuService.addItem("Avocado Toast",     MenuCategory.FOOD,     6.50);
            menuService.addItem("Tiramisu",          MenuCategory.DESSERT,  5.50);
        }

        // ── 5. Seed demo Borgol users + content ───────────────────────────────
        seedDemoData(borgolService);

        // ── 6. Seed static content (brew guides + learn articles) ─────────────
        borgolService.seedStaticContent();

        // ── 6b. Seed GPS coordinates for demo cafes (idempotent) ─────────────
        seedCafeCoordinates(borgolService);

        // ── 7. Launch JavaFX desktop app ─────────────────────────────────────
        BorgolApp.setService(borgolService);
        javafx.application.Application.launch(BorgolApp.class, args);
    }

    private static void seedDemoData(BorgolService svc) {
        // Seed base 3 users if not present
        boolean baseMissing = false;
        try { svc.getMe(1); } catch (Exception ignored) { baseMissing = true; }

        if (baseMissing) {
            System.out.println("  [SEED] Seeding Borgol demo data...");
            seedBaseUsers(svc);
            System.out.println("  [SEED] Demo data seeded successfully.");
        }

        // Seed extra 5 users if not present (id=4 would be first extra user)
        boolean extraMissing = false;
        try { svc.getMe(4); } catch (Exception ignored) { extraMissing = true; }
        if (extraMissing) {
            System.out.println("  [SEED] Seeding extra demo users...");
            seedExtraUsers(svc);
            System.out.println("  [SEED] Extra users seeded.");
        }
    }

    private static void seedBaseUsers(BorgolService svc) {
        // Demo users
        var u1 = svc.register("coffee_master", "coffee@borgol.mn", "password123");
        var u2 = svc.register("barista_sara",  "sara@borgol.mn",   "password123");
        var u3 = svc.register("tea_lover",     "tea@borgol.mn",    "password123");

        int id1 = u1.user().id();
        int id2 = u2.user().id();
        int id3 = u3.user().id();

        // Update profiles
        svc.updateProfile(id1, "Coffee enthusiast & home barista. Obsessed with pour-over.",
            "", "BARISTA", java.util.List.of("BITTER", "CHOCOLATEY", "EARTHY"));
        svc.updateProfile(id2, "Professional barista at Blue Sky Cafe, Ulaanbaatar.",
            "", "EXPERT", java.util.List.of("CARAMEL", "SWEET", "NUTTY"));
        svc.updateProfile(id3, "Tea & coffee crossover fan. Love exploring new flavors.",
            "", "ENTHUSIAST", java.util.List.of("FLORAL", "FRUITY", "SWEET"));

        // Follow relationships
        svc.followUser(id1, id2);
        svc.followUser(id2, id1);
        svc.followUser(id3, id1);
        svc.followUser(id3, id2);

        // Recipes
        svc.createRecipe(id1, "Perfect Pour-Over Coffee",
            "My go-to pour-over recipe for a clean, bright cup with floral and citrus notes.",
            "POUR_OVER",
            "20g medium-fine ground coffee\n300ml water at 93°C\nPour-over dripper (Hario V60)\nPaper filter",
            "1. Rinse filter with hot water\n2. Add 20g coffee\n3. Bloom: pour 40ml, wait 30s\n4. Continue pouring in circular motions\n5. Total brew time: 3-4 minutes",
            4, "MEDIUM",
            java.util.List.of("FLORAL", "FRUITY", "BITTER"),
            "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=600");

        svc.createRecipe(id2, "Silky Caramel Latte",
            "A rich, velvety latte with house-made caramel sauce. Perfect morning comfort drink.",
            "LATTE",
            "Double espresso (18g coffee)\n200ml whole milk\n30ml caramel sauce\nPinch of sea salt",
            "1. Pull double espresso into warmed cup\n2. Steam milk to 65°C with fine microfoam\n3. Add caramel sauce to espresso\n4. Pour steamed milk with latte art\n5. Drizzle caramel and add sea salt",
            5, "HARD",
            java.util.List.of("CARAMEL", "SWEET", "NUTTY"),
            "https://images.unsplash.com/photo-1570968915860-54d5c301fa9f?w=600");

        svc.createRecipe(id1, "Japanese Iced Coffee",
            "Flash-brewed hot coffee over ice preserves aromatic compounds lost in cold brew.",
            "COLD_BREW",
            "30g light roast coffee (medium-fine grind)\n150ml water at 93°C\n150g ice",
            "1. Place ice directly in serving glass\n2. Brew pour-over directly over ice\n3. Adjust grind finer to compensate for dilution\n4. Serve immediately",
            6, "MEDIUM",
            java.util.List.of("FRUITY", "FLORAL", "SOUR"),
            "https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=600");

        svc.createRecipe(id2, "Classic Cappuccino",
            "Equal parts espresso, steamed milk, and milk foam. The trinity of espresso drinks.",
            "CAPPUCCINO",
            "18g espresso blend\n60ml steamed milk\n60ml milk foam",
            "1. Pull 60ml double espresso\n2. Steam 120ml milk to 65°C\n3. Pour half as steamed milk\n4. Spoon remaining foam on top\n5. Optional: dust with cocoa powder",
            4, "MEDIUM",
            java.util.List.of("BITTER", "CHOCOLATEY", "SWEET"),
            "https://images.unsplash.com/photo-1517701604599-bb29b565090c?w=600");

        svc.createRecipe(id3, "Mongolian Butter Tea (Suutei Tsai)",
            "Traditional Mongolian salted milk tea. Warming and nourishing — a cultural staple.",
            "TEA",
            "2 tbsp brick tea or pu-erh\n1L water\n500ml milk\n2 tbsp butter\n1 tsp salt",
            "1. Boil water with brick tea for 5 minutes\n2. Strain and add milk\n3. Simmer together for 3 minutes\n4. Add butter and salt\n5. Pour back and forth between pot and ladle to mix",
            8, "EASY",
            java.util.List.of("EARTHY", "SMOKY"),
            "https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=600");

        // Likes
        svc.toggleLike(id2, 1);
        svc.toggleLike(id3, 1);
        svc.toggleLike(id1, 2);
        svc.toggleLike(id3, 2);
        svc.toggleLike(id2, 3);

        // Comments
        svc.addComment(1, id2, "Beautiful recipe! The 30-second bloom makes all the difference.");
        svc.addComment(1, id3, "Been trying this for weeks, finally getting consistent results!");
        svc.addComment(2, id1, "The sea salt is a game changer. Tried this at your cafe Sara, amazing.");

        // Cafes
        svc.createCafe(id2, "Blue Sky Cafe",
            "Sukhbaatar District, Seoul Street 15", "Sukhbaatar", "Ulaanbaatar",
            "+976 9911-2233",
            "Specialty coffee shop with direct-trade beans from Ethiopia, Colombia, and Guatemala. " +
            "Known for exceptional latte art and single-origin pour-overs.",
            "Mon-Fri 8:00-20:00, Sat-Sun 9:00-21:00",
            "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=600");

        svc.createCafe(id1, "Nomadic Beans",
            "Khan-Uul District, Zaisan 32", "Khan-Uul", "Ulaanbaatar",
            "+976 9988-4455",
            "Cozy specialty cafe inspired by nomadic culture. Serves traditional Mongolian tea alongside " +
            "specialty espresso drinks. Great workspace with fast WiFi.",
            "Daily 8:00-22:00",
            "https://images.unsplash.com/photo-1445116572660-236099ec97a0?w=600");

        svc.createCafe(id3, "The Yurt Coffee House",
            "Bayanzurkh District, Peace Avenue 44", "Bayanzurkh", "Ulaanbaatar",
            "+976 9977-6611",
            "Unique café inside a traditional ger (yurt) setup. Serves both Mongolian dairy tea " +
            "and modern espresso drinks. Must-visit for tourists and locals alike.",
            "Tue-Sun 10:00-20:00",
            "https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=600");

        // Cafe ratings
        svc.rateCafe(id1, 1, 5, "Sara's cappuccino is the best in UB. Incredible atmosphere.");
        svc.rateCafe(id3, 1, 5, "Amazing single origin pour-overs. Staff is so knowledgeable.");
        svc.rateCafe(id2, 2, 4, "Love the nomadic vibe. Coffee is great, great for working.");
        svc.rateCafe(id3, 2, 5, "My favorite place to work. Fast WiFi and excellent cold brew.");
        svc.rateCafe(id1, 3, 4, "Unique experience! Great for visitors wanting authentic Mongolia.");
        svc.rateCafe(id2, 3, 4, "The suutei tsai here is authentic and delicious.");
    }

    private static void seedExtraUsers(BorgolService svc) {
        var u4 = svc.register("latte_king",       "latte@borgol.mn",   "password123");
        var u5 = svc.register("espresso_pro",      "espresso@borgol.mn","password123");
        var u6 = svc.register("cold_brew_queen",   "coldbrew@borgol.mn","password123");
        var u7 = svc.register("matcha_monk",       "matcha@borgol.mn",  "password123");
        var u8 = svc.register("roast_master",      "roast@borgol.mn",   "password123");

        int id4 = u4.user().id();
        int id5 = u5.user().id();
        int id6 = u6.user().id();
        int id7 = u7.user().id();
        int id8 = u8.user().id();

        svc.updateProfile(id4, "Latte art enthusiast. Rosettes and tulips are my jam.",
            "", "BARISTA", java.util.List.of("CARAMEL", "SWEET", "MILKY"));
        svc.updateProfile(id5, "Espresso purist. Single origin, no shortcuts.",
            "", "EXPERT", java.util.List.of("BITTER", "EARTHY", "CHOCOLATEY"));
        svc.updateProfile(id6, "Cold brew devotee. 24-hour steeps only.",
            "", "ENTHUSIAST", java.util.List.of("SMOOTH", "FRUITY", "SWEET"));
        svc.updateProfile(id7, "Japanese matcha meets specialty coffee culture.",
            "", "ENTHUSIAST", java.util.List.of("FLORAL", "GRASSY", "UMAMI"));
        svc.updateProfile(id8, "Roast profiler and home roasting geek.",
            "", "EXPERT", java.util.List.of("SMOKY", "CHOCOLATEY", "EARTHY"));

        // Follow network — everyone follows coffee_master (id=1)
        svc.followUser(id4, 1); svc.followUser(id5, 1);
        svc.followUser(id6, 1); svc.followUser(id7, 2);
        svc.followUser(id8, 1); svc.followUser(id4, id5);
        svc.followUser(id6, id8); svc.followUser(id7, id6);

        // Recipes
        svc.createRecipe(id4, "Honey Oat Milk Latte",
            "Creamy oat milk latte sweetened with wildflower honey. Silky smooth.",
            "LATTE",
            "Double espresso (18g dark roast)\n220ml oat milk\n1 tbsp wildflower honey",
            "1. Pull double espresso into warmed glass\n2. Steam oat milk to 60°C with microfoam\n3. Stir honey into espresso\n4. Pour oat milk with slow-speed pour for layered effect",
            4, "MEDIUM",
            java.util.List.of("CARAMEL", "SWEET", "NUTTY"),
            "https://images.unsplash.com/photo-1545665277-5937489579f2?w=600");

        svc.createRecipe(id5, "Ristretto-Based Flat White",
            "Two ristretto shots with velvety microfoam. The Australian classic, done right.",
            "ESPRESSO",
            "18g espresso blend (ristretto pull: 1:1.5 ratio)\n100ml whole milk",
            "1. Dial in ristretto: 18g in, 27g out in 25s\n2. Steam 120ml milk to 60°C — aim for silky paint texture\n3. Pour from high to create contrast, then fold microfoam close",
            6, "HARD",
            java.util.List.of("BITTER", "CHOCOLATEY", "SWEET"),
            "https://images.unsplash.com/photo-1534040385115-33dcb3acba5b?w=600");

        svc.createRecipe(id6, "Kyoto-Style Slow Cold Brew",
            "Drip cold brew inspired by Kyoto drip towers. Delicate and complex.",
            "COLD_BREW",
            "40g light roast, coarse grind\n500ml filtered water (ice cold)\n500g ice",
            "1. Load coarse ground coffee into filter\n2. Place ice layer on top\n3. Drip at 1 drop/second (approximately)\n4. Collect over 8-12 hours in refrigerator\n5. Dilute 1:1 with water before serving",
            8, "EASY",
            java.util.List.of("FRUITY", "FLORAL", "SWEET"),
            "https://images.unsplash.com/photo-1497935586351-b67a49e012bf?w=600");

        svc.createRecipe(id7, "Matcha Espresso Fusion",
            "Ceremonial matcha meets espresso. Bold, earthy, energizing — and surprisingly harmonious.",
            "TEA",
            "2g ceremonial grade matcha\n60ml hot water (75°C)\n1 shot espresso\n120ml oat milk",
            "1. Sift matcha into bowl\n2. Whisk with 60ml water at 75°C until frothy\n3. Pull espresso shot\n4. Steam oat milk\n5. Layer: espresso → oat milk → matcha on top",
            5, "MEDIUM",
            java.util.List.of("FLORAL", "EARTHY", "BITTER"),
            "https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=600");

        svc.createRecipe(id8, "Ethiopian Natural Aeropress",
            "Showcasing the fruit-forward character of Ethiopian naturals via Aeropress.",
            "POUR_OVER",
            "15g Ethiopian natural, medium-coarse grind\n200ml water at 85°C",
            "1. Inverted Aeropress method — flip and add filter\n2. Add coffee, pour 200ml water, stir 10 times\n3. Steep for 90 seconds\n4. Flip and press slowly over 30 seconds",
            3, "MEDIUM",
            java.util.List.of("FRUITY", "SWEET", "FLORAL"),
            "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=600");

        // Likes on extra recipes
        svc.toggleLike(1, 6); svc.toggleLike(2, 6); svc.toggleLike(3, 6);
        svc.toggleLike(1, 7); svc.toggleLike(3, 7);
        svc.toggleLike(2, 8); svc.toggleLike(id7, 8);
        svc.toggleLike(1, 9); svc.toggleLike(id4, 9);
        svc.toggleLike(2, 10); svc.toggleLike(id5, 10);
    }

    /**
     * Sets real Ulaanbaatar GPS coordinates on the three demo cafes.
     * Idempotent: skips any cafe that already has a latitude set.
     *
     *   Blue Sky Cafe        → Sukhbaatar District, Seoul Street area
     *   Nomadic Beans        → Khan-Uul District, Zaisan
     *   The Yurt Coffee House → Bayanzurkh District, Peace Avenue
     */
    private static void seedCafeCoordinates(BorgolService svc) {
        var cafes = svc.getCafes(0, null, null);
        for (var cafe : cafes) {
            if (cafe.getLat() != null) continue; // already pinned
            switch (cafe.getName()) {
                case "Blue Sky Cafe"          -> svc.updateCafeCoordinates(cafe.getId(), 47.9176, 106.9176);
                case "Nomadic Beans"          -> svc.updateCafeCoordinates(cafe.getId(), 47.8740, 106.8638);
                case "The Yurt Coffee House"  -> svc.updateCafeCoordinates(cafe.getId(), 47.9135, 106.9487);
            }
        }
        System.out.println("  [SEED] Cafe GPS coordinates set.");
    }
}
