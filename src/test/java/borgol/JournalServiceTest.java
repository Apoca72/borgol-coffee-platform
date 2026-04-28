package borgol;

import borgol.core.application.JournalService;
import borgol.core.application.AchievementService;
import borgol.core.domain.BeanBag;
import borgol.core.domain.BrewJournalEntry;
import borgol.core.ports.AchievementRepositoryPort;
import borgol.core.ports.JournalRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class JournalServiceTest {

    static class StubJournalRepo implements JournalRepositoryPort {
        BrewJournalEntry lastCreated;
        @Override public List<BrewJournalEntry> getJournalEntries(int uid) { return List.of(); }
        @Override public Optional<BrewJournalEntry> findJournalEntry(int id, int uid) { return Optional.empty(); }
        @Override public BrewJournalEntry createJournalEntry(BrewJournalEntry e) { lastCreated = e; return e; }
        @Override public BrewJournalEntry updateJournalEntry(BrewJournalEntry e) { return e; }
        @Override public boolean deleteJournalEntry(int id, int uid) { return true; }
        @Override public List<BeanBag> getBeanBags(int uid) { return List.of(); }
        @Override public BeanBag createBeanBag(BeanBag b) { return b; }
        @Override public BeanBag updateBeanBag(BeanBag b) { return b; }
        @Override public void deleteBeanBag(int id, int uid) {}
        @Override public Map<String, Object> getJournalStats(int uid) { return Map.of(); }
    }

    static class StubAchievementRepo implements AchievementRepositoryPort {
        @Override public List<Map<String,Object>> getAchievements(int uid, Map<String,String[]> meta) { return List.of(); }
        @Override public List<String> checkAndAwardAchievements(int uid, Map<String,String[]> meta) { return List.of(); }
    }

    private JournalService svc;
    private StubJournalRepo repo;

    @BeforeEach void setUp() {
        repo = new StubJournalRepo();
        svc  = new JournalService(repo, new AchievementService(new StubAchievementRepo()));
    }

    @Test void weatherData_truncatedAt300Chars() {
        String longWeather = "x".repeat(500);
        svc.createJournalEntry(1, "Ethiopia Yirgacheffe", "pour_over",
                18.0, 300.0, 93, 4, "notes", longWeather);
        assertNotNull(repo.lastCreated);
        assertTrue(repo.lastCreated.getWeatherData().length() <= 300,
                "weatherData must be capped at 300 chars");
    }

    @Test void weatherData_null_storedAsEmpty() {
        svc.createJournalEntry(1, "Sumatra", "espresso",
                18.0, 40.0, 92, 5, "bold", null);
        assertEquals("", repo.lastCreated.getWeatherData(),
                "null weatherData should be stored as empty string");
    }
}
