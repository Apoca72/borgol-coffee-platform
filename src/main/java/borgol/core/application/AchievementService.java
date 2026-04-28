package borgol.core.application;

import borgol.core.ports.AchievementRepositoryPort;

import java.util.List;
import java.util.Map;

/**
 * Badge logic. BADGE_META нь энд хадгалагдана — DB schema биш.
 */
public class AchievementService {

    public static final Map<String, String[]> BADGE_META = Map.of(
        "first_brew",       new String[]{"☕", "First Sip",        "Logged your first brew"},
        "brew_10",          new String[]{"🔥", "Regular",          "Logged 10 brews"},
        "brew_50",          new String[]{"💪", "Dedicated",        "Logged 50 brews"},
        "recipe_author",    new String[]{"📝", "Recipe Author",    "Created your first recipe"},
        "cafe_explorer",    new String[]{"🗺️", "Cafe Explorer",    "Rated 3 different cafes"},
        "social_butterfly", new String[]{"🦋", "Social Butterfly", "Followed 5 users"},
        "bean_collector",   new String[]{"🫘", "Bean Collector",   "Added 5 beans"},
        "pour_over_pro",    new String[]{"⏱️", "Pour Over Pro",    "Logged 5 pour-over entries"}
    );

    private final AchievementRepositoryPort repo;

    public AchievementService(AchievementRepositoryPort repo) {
        this.repo = repo;
    }

    public List<Map<String, Object>> getAchievements(int userId) {
        return repo.getAchievements(userId, BADGE_META);
    }

    public List<String> checkAndAwardAchievements(int userId) {
        return repo.checkAndAwardAchievements(userId, BADGE_META);
    }
}
