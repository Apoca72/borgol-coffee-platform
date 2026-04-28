package borgol.core.application;

import borgol.core.domain.BrewGuide;
import borgol.core.domain.LearnArticle;
import borgol.core.ports.BrewGuideRepositoryPort;

import java.util.List;
import java.util.Optional;

public class BrewGuideService {

    private final BrewGuideRepositoryPort repo;

    public BrewGuideService(BrewGuideRepositoryPort repo) {
        this.repo = repo;
    }

    // ── Brew Guides ───────────────────────────────────────────────────────────

    public List<BrewGuide> getBrewGuides() {
        return repo.findAllBrewGuides();
    }

    public BrewGuide getBrewGuide(int id) {
        return repo.findBrewGuideById(id)
            .orElseThrow(() -> new IllegalArgumentException("Brew guide not found: id=" + id));
    }

    // ── Learn Articles ────────────────────────────────────────────────────────

    public List<LearnArticle> getLearnArticles() {
        return repo.findAllLearnArticles();
    }

    public LearnArticle getLearnArticle(int id) {
        return repo.findLearnArticleById(id)
            .orElseThrow(() -> new IllegalArgumentException("Article not found: id=" + id));
    }

    // ── Static content seeding (idempotent) ───────────────────────────────────

    public void seedStaticContent() {
        if (repo.isStaticContentSeeded()) return;

        seedGuide("Pour Over (V60)", "☕",
            "A classic manual brew producing a clean, bright cup that highlights delicate flavors.",
            "BEGINNER", 4,
            "Coffee:15g\nWater:250ml\nGrind:Medium-fine\nTemp:92°C\nRatio:1:16.5",
            "1. Rinse the V60 filter with hot water and discard rinse water\n" +
            "2. Add 15g of medium-fine ground coffee to the filter\n" +
            "3. Create a small well in the center of the grounds\n" +
            "4. Bloom: pour 30ml of water and wait 30 seconds\n" +
            "5. Pour in slow concentric circles to reach 130ml at 1:00\n" +
            "6. Continue pouring to 250ml total by 2:00\n" +
            "7. Allow to drain completely — total time ~3:30");

        seedGuide("French Press", "🫖",
            "A full-immersion brew with rich body and bold flavors from the metal filter.",
            "BEGINNER", 5,
            "Coffee:30g\nWater:500ml\nGrind:Coarse\nTemp:95°C\nRatio:1:16",
            "1. Preheat the French press with hot water, then discard\n" +
            "2. Add 30g of coarsely ground coffee\n" +
            "3. Pour 500ml of water at 95°C over the grounds\n" +
            "4. Stir gently to ensure all grounds are saturated\n" +
            "5. Place the lid on with the plunger pulled up\n" +
            "6. Steep for exactly 4 minutes\n" +
            "7. Press plunger slowly and steadily — pour immediately");

        seedGuide("AeroPress", "🔄",
            "Versatile and forgiving — produces a smooth, espresso-like concentrate.",
            "INTERMEDIATE", 2,
            "Coffee:17g\nWater:220ml\nGrind:Medium\nTemp:85°C\nRatio:1:13",
            "1. Insert a paper filter into the AeroPress cap and rinse\n" +
            "2. Assemble in inverted position (plunger down)\n" +
            "3. Add 17g of ground coffee\n" +
            "4. Pour 220ml of water at 85°C and stir for 10 seconds\n" +
            "5. Secure the cap with filter\n" +
            "6. At 1:30 flip onto your cup carefully\n" +
            "7. Press steadily for 30 seconds — stop at first hiss");

        seedGuide("Espresso", "⚡",
            "High-pressure extraction creating an intense, concentrated shot with crema.",
            "ADVANCED", 1,
            "Coffee:18g\nYield:36g\nGrind:Extra-fine\nTemp:93°C\nPressure:9 bar",
            "1. Flush the group head with hot water for 2 seconds\n" +
            "2. Dose 18g of finely ground coffee into the portafilter\n" +
            "3. Distribute evenly and tamp with 15kg of pressure\n" +
            "4. Lock portafilter into the group head\n" +
            "5. Start extraction — aim for first drops at 5-7 seconds\n" +
            "6. Target 36g yield in 25-30 seconds total\n" +
            "7. Adjust grind finer if fast, coarser if slow");

        seedGuide("Cold Brew", "🧊",
            "Slow, cold extraction over 12–24 hours produces a smooth, sweet concentrate.",
            "BEGINNER", 720,
            "Coffee:100g\nWater:800ml\nGrind:Extra-coarse\nTemp:Cold (4°C)\nRatio:1:8",
            "1. Coarsely grind 100g of coffee beans\n" +
            "2. Combine coffee and 800ml of cold filtered water in a jar\n" +
            "3. Stir to ensure all grounds are saturated\n" +
            "4. Cover and refrigerate for 12–24 hours\n" +
            "5. Strain through a fine mesh strainer twice\n" +
            "6. Optionally pass through a paper filter for clarity\n" +
            "7. Store in fridge up to 2 weeks — dilute 1:1 to serve");

        seedGuide("Moka Pot", "🏠",
            "Stovetop espresso-style brew with rich, bittersweet flavors and heavy body.",
            "BEGINNER", 8,
            "Coffee:22g\nWater:200ml\nGrind:Fine-medium\nTemp:Stovetop\nRatio:1:9",
            "1. Fill the bottom chamber with hot water to the valve\n" +
            "2. Insert the filter basket and fill with 22g of ground coffee\n" +
            "3. Level the grounds without tamping\n" +
            "4. Screw the top chamber on tightly\n" +
            "5. Place on medium-low heat\n" +
            "6. Keep lid open and watch for coffee to emerge slowly\n" +
            "7. Remove from heat when sputtering — serve immediately");

        seedArticle("Understanding Roast Levels", "🔥", "Roasting",
            "## Light Roast\n" +
            "Light roasts are roasted to an internal temperature of 180–205°C. " +
            "The beans are light brown and have no surface oils. " +
            "They preserve the most origin character — you'll taste the terroir, " +
            "the altitude, and the variety of the bean itself. " +
            "Expect floral, fruity, and tea-like notes with high acidity.\n\n" +
            "## Medium Roast\n" +
            "Roasted to 210–220°C, medium roasts balance origin flavor with roast character. " +
            "The beans are medium brown with little oil. You get sweetness, " +
            "caramel notes, and a balanced acidity. This is the most popular roast level " +
            "and works well for drip coffee and pour overs.\n\n" +
            "## Dark Roast\n" +
            "Dark roasts reach 225–245°C. The beans are dark brown to almost black " +
            "with oily surfaces. Roast flavors dominate — chocolate, bittersweet, smoky. " +
            "Origin character is mostly lost. Lower acidity, fuller body. " +
            "Classic for espresso and French press.", 4);

        seedArticle("The Science of Coffee Extraction", "⚗️", "Brewing Science",
            "## What Is Extraction?\n" +
            "Extraction is the process of dissolving soluble compounds from coffee grounds " +
            "into water. About 30% of a coffee bean is water-soluble, but you only want " +
            "to extract 18–22% for the best flavor.\n\n" +
            "## Under-Extraction\n" +
            "Under-extracted coffee (below 18%) tastes sour, salty, and lacking sweetness. " +
            "This happens when water is too cool, grind is too coarse, " +
            "brew time is too short, or the dose is too low.\n\n" +
            "## Over-Extraction\n" +
            "Over-extracted coffee (above 22%) tastes bitter, harsh, and dry. " +
            "Fix by using a coarser grind, lower water temperature, " +
            "shorter contact time, or less coffee.\n\n" +
            "## The Golden Ratio\n" +
            "The Specialty Coffee Association recommends a brew ratio of 1:15 to 1:17 " +
            "(coffee to water by weight). Start at 1:15 and adjust to taste.", 5);

        seedArticle("Water Quality for Coffee", "💧", "Brewing Science",
            "## Why Water Matters\n" +
            "Coffee is 98% water. The minerals dissolved in water dramatically affect " +
            "extraction and taste. Distilled water produces flat, lifeless coffee " +
            "because minerals help extract compounds from grounds.\n\n" +
            "## Ideal Mineral Content\n" +
            "The SCA recommends Total Dissolved Solids (TDS) of 75–250 ppm, " +
            "with a target of about 150 ppm. Magnesium ions enhance flavor extraction. " +
            "Calcium contributes to body. Too much sodium makes coffee taste salty.\n\n" +
            "## Temperature\n" +
            "Brew temperature should be 90–96°C (195–205°F). Below 88°C leads to " +
            "under-extraction. Above 96°C increases bitterness. " +
            "For lighter roasts, use higher temperatures (94–96°C). " +
            "For darker roasts, go lower (88–92°C).\n\n" +
            "## Practical Tips\n" +
            "Filtered tap water is usually ideal. Avoid softened water — " +
            "it replaces calcium and magnesium with sodium.", 4);

        seedArticle("Coffee Tasting & the Flavor Wheel", "🎨", "Tasting",
            "## What Is the Coffee Flavor Wheel?\n" +
            "The SCA Flavor Wheel maps the spectrum of coffee flavors into categories: " +
            "fruity, floral, sweet, nutty/cocoa, spicy, roasted, and savory. " +
            "It was created to give baristas and enthusiasts a shared vocabulary.\n\n" +
            "## How to Taste Coffee\n" +
            "Start by smelling the dry grounds (fragrance). Then smell the wet coffee (aroma). " +
            "Slurp the coffee to spray it across your palate. " +
            "Notice the flavors, the mouthfeel (body), acidity, and how it finishes.\n\n" +
            "## Key Attributes\n" +
            "**Aroma** — fragrances you smell before and during drinking.\n" +
            "**Acidity** — brightness or liveliness; citric, malic, or phosphoric.\n" +
            "**Body** — mouthfeel; thin, medium, or full/syrupy.\n" +
            "**Sweetness** — natural sugars that balance acidity and bitterness.\n" +
            "**Finish** — how flavors linger after swallowing.", 5);

        seedArticle("Arabica vs Robusta", "🌿", "Coffee Origins",
            "## Arabica (Coffea arabica)\n" +
            "Arabica makes up ~60% of global coffee production. " +
            "It grows at high altitudes (600–2000m) in tropical climates. " +
            "Arabica has lower caffeine (~1.5%), higher sugars and lipids, " +
            "and a wider flavor spectrum. Expect floral, fruity, chocolatey, or " +
            "caramel notes with pleasant acidity.\n\n" +
            "## Robusta (Coffea canephora)\n" +
            "Robusta grows at lower altitudes and is more disease-resistant. " +
            "It has nearly twice the caffeine of Arabica (~2.7%), " +
            "which acts as a natural pest deterrent. " +
            "Robusta is cheaper to produce and has a harsher, more bitter taste. " +
            "It's commonly used in instant coffee and espresso blends for crema.\n\n" +
            "## Which Is Better?\n" +
            "For specialty coffee, Arabica is the standard. " +
            "But high-quality Robusta from Uganda or Vietnam can be surprisingly complex " +
            "and is excellent for espresso blends that need more body and crema.", 4);

        seedArticle("Grind Size Guide", "⚙️", "Brewing Science",
            "## Why Grind Size Matters\n" +
            "Grind size determines the surface area exposed to water. " +
            "Finer grinds extract faster; coarser grinds extract slower. " +
            "Matching grind to brew method is essential for balanced extraction.\n\n" +
            "## Grind Size Chart\n" +
            "**Extra Fine** — Turkish coffee; powder-like consistency\n" +
            "**Fine** — Espresso; fine sand texture\n" +
            "**Medium-Fine** — Pour over (V60, Kalita); between sand and sea salt\n" +
            "**Medium** — Drip coffee, AeroPress; sea salt\n" +
            "**Medium-Coarse** — Chemex, Clever Dripper; rough sand\n" +
            "**Coarse** — French Press; coarse sea salt\n" +
            "**Extra Coarse** — Cold brew; peppercorn\n\n" +
            "## The Grinder Matters\n" +
            "Blade grinders produce inconsistent particles causing uneven extraction. " +
            "Burr grinders (conical or flat) create uniform grinds. " +
            "For specialty coffee, a quality burr grinder is the single best investment.", 5);
    }

    public void seedEnrichedContent() {
        if (!repo.isBeanArticlesSeeded()) {
            seedArticle("Ethiopian Yirgacheffe", "🌸", "Beans",
                "## Origin\nYirgacheffe is a town in the Gedeo Zone of southern Ethiopia, widely " +
                "regarded as the birthplace of coffee. Grown at 1,700–2,200 m altitude.\n\n" +
                "## Flavor Profile\nLight roast reveals **jasmine, blueberry, lemon zest, and bergamot**. " +
                "Bright, tea-like acidity with a delicate, clean finish.\n\n" +
                "## Roast\nBest enjoyed as a **light roast** (180–195°C) to preserve floral aromatics. " +
                "Medium roast brings out more caramel sweetness. Avoid dark — it destroys the character.\n\n" +
                "## Brew Pairings\nPour Over (V60), AeroPress, or filter drip. " +
                "High-temperature water (93–96°C) works well with light roasts.", 4);

            seedArticle("Colombian Huila", "🏔️", "Beans",
                "## Origin\nHuila is a mountainous department in southwest Colombia at 1,500–2,000 m. " +
                "Known for its volcanic soil, consistent rainfall, and family-run farms.\n\n" +
                "## Flavor Profile\n**Caramel sweetness, red apple, citrus, and milk chocolate** " +
                "with a smooth medium body and balanced acidity.\n\n" +
                "## Roast\nExcels as a **medium roast** (210–220°C). Retains sweetness without " +
                "becoming bitter. One of the most versatile origins for any brew method.\n\n" +
                "## Brew Pairings\nWorks beautifully for espresso, pour over, and French press. " +
                "An excellent daily driver for home baristas.", 4);

            seedArticle("Kenyan AA", "🦁", "Beans",
                "## Origin\nKenya's central highlands around Mt. Kenya and the Aberdare Range " +
                "(1,400–2,000 m). 'AA' is the largest screen size grade, indicating large, dense beans.\n\n" +
                "## Flavor Profile\n**Blackcurrant, tomato, grapefruit, and wine-like brightness**. " +
                "Complex, juicy acidity with a full body and lingering finish.\n\n" +
                "## Roast\n**Light to medium** (190–215°C). The SL28 and SL34 varietals express " +
                "maximum complexity when not over-roasted. A challenging but rewarding origin.\n\n" +
                "## Brew Pairings\nPour over showcases the brightness. French press enhances body. " +
                "Not ideal for espresso — acidity becomes sharp under pressure.", 4);

            seedArticle("Guatemalan Antigua", "🌋", "Beans",
                "## Origin\nGrown in the Antigua valley surrounded by three volcanoes — Agua, Fuego, " +
                "and Acatenango — at 1,500–1,700 m. Volcanic ash provides exceptional mineral richness.\n\n" +
                "## Flavor Profile\n**Dark chocolate, brown sugar, almond, and mild spice** with a " +
                "medium-heavy body and low, soft acidity.\n\n" +
                "## Roast\nShines at **medium-dark roast** (220–230°C). The chocolate notes deepen " +
                "without crossing into bitterness. One of the most consistent Central American origins.\n\n" +
                "## Brew Pairings\nExcellent for espresso and Moka Pot. Also exceptional as a " +
                "French press — the body holds up well to the metal filter.", 4);

            seedArticle("Indonesian Mandheling", "🌿", "Beans",
                "## Origin\nFrom the Batak highlands of North Sumatra, Indonesia, around Lake Toba " +
                "at 1,100–1,600 m. Processed using the unique 'wet-hulling' (Giling Basah) method.\n\n" +
                "## Flavor Profile\n**Earthy, cedar, dark chocolate, tobacco, and mushroom** with a " +
                "very full body and low acidity. Syrupy mouthfeel.\n\n" +
                "## Roast\nTraditionally roasted **dark** (230–245°C). The wet-hulling process " +
                "creates the characteristic earthy funk and heavy body. Not for the light-roast enthusiast.\n\n" +
                "## Brew Pairings\nFrench press and Moka Pot maximize the body. Also well-suited " +
                "to cold brew — the earthiness mellows beautifully over 18 hours.", 4);

            seedArticle("Brazilian Cerrado Mineiro", "🌾", "Beans",
                "## Origin\nThe Cerrado Mineiro region in Minas Gerais state, Brazil, at 800–1,300 m. " +
                "A designated Geographic Indication zone with dry climate and flat terrain — " +
                "suited to mechanized harvesting and consistent naturals.\n\n" +
                "## Flavor Profile\n**Milk chocolate, hazelnut, caramel, and soft sweetness** " +
                "with very low acidity and a smooth, round body.\n\n" +
                "## Roast\n**Medium roast** (210–220°C) highlights sweetness. The low acidity makes " +
                "it forgiving at medium-dark too. Brazil is the world's largest coffee producer " +
                "and Cerrado is its flagship specialty region.\n\n" +
                "## Brew Pairings\nThe go-to espresso base for blends. Excellent for cold brew " +
                "due to natural sweetness. Great entry-point origin for new specialty drinkers.", 4);
        }

        if (!repo.isDrinkArticlesSeeded()) {
            seedArticle("Coffee Drink Guide", "☕", "Drinks",
                "## Espresso\n" +
                "A 25–30 ml concentrated shot extracted at 9 bar in 25–30 seconds. The foundation of most " +
                "café drinks. Intense, syrupy, with a golden-red crema. **Best bean:** Medium-dark or dark " +
                "roast (Brazilian, Guatemalan, or Italian blend).\n\n" +
                "## Americano\n" +
                "Espresso diluted with hot water (1:2–1:4). Maintains espresso flavor without the " +
                "concentration. **Milk ratio:** None. Often ordered black. Larger serving for those who " +
                "want espresso flavor in a longer drink.\n\n" +
                "## Latte\n" +
                "Espresso + steamed milk (150–200 ml) + thin microfoam layer. Ratio ~1:4 coffee to milk. " +
                "Creamy, mild, crowd-pleasing. **Best bean:** Medium roast (Colombian, Brazilian). " +
                "Canvas for latte art.\n\n" +
                "## Cappuccino\n" +
                "Equal thirds: espresso + steamed milk + thick milk foam. 150–180 ml total. " +
                "More intense than a latte, less sweet. Traditionally dry (more foam) or wet (more milk). " +
                "**Best bean:** Dark or medium-dark.\n\n" +
                "## Flat White\n" +
                "Double ristretto (short espresso) + 120 ml velvety microfoam — smaller and stronger than a latte. " +
                "Australian origin. **Best bean:** A blend or single origin with chocolate/nut notes.\n\n" +
                "## Cold Brew\n" +
                "Coffee steeped in cold water for 12–24 hours, then filtered. Smooth, naturally sweet, " +
                "low acidity. Serve over ice diluted 1:1. **Best bean:** Medium or dark roast with " +
                "chocolate and nut character (Brazilian Cerrado, Colombian).\n\n" +
                "## Pour Over\n" +
                "Manual filter brew using gravity. Produces a clean, bright, tea-like cup that highlights " +
                "delicate origin flavors. Methods: V60, Chemex, Kalita Wave. " +
                "**Best bean:** Light roast with floral/fruity notes (Ethiopian, Kenyan).\n\n" +
                "## French Press\n" +
                "Full-immersion brew with a metal mesh filter. Rich body, heavy mouthfeel, " +
                "more oils and texture than filter. 4-minute steep. **Best bean:** Medium-dark to dark " +
                "roast (Guatemalan, Indonesian Mandheling).", 6);
        }
    }

    private void seedGuide(String name, String icon, String desc, String diff, int time,
                            String params, String steps) {
        BrewGuide g = new BrewGuide();
        g.setMethodName(name);
        g.setIcon(icon);
        g.setDescription(desc);
        g.setDifficulty(diff);
        g.setBrewTimeMin(time);
        g.setParameters(params);
        g.setSteps(steps);
        repo.seedBrewGuide(g);
    }

    private void seedArticle(String title, String icon, String category, String content, int readTime) {
        LearnArticle a = new LearnArticle();
        a.setTitle(title);
        a.setIcon(icon);
        a.setCategory(category);
        a.setContent(content);
        a.setReadTimeMin(readTime);
        repo.seedLearnArticle(a);
    }
}
