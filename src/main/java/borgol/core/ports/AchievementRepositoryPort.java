package borgol.core.ports;

import java.util.List;
import java.util.Map;

public interface AchievementRepositoryPort {
    List<Map<String, Object>> getAchievements(int userId, Map<String, String[]> meta);
    List<String> checkAndAwardAchievements(int userId, Map<String, String[]> meta);
}
