package borgol.core.application;

import borgol.core.domain.BeanBag;
import borgol.core.domain.BrewJournalEntry;
import borgol.core.ports.JournalRepositoryPort;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JournalService {

    private final JournalRepositoryPort repo;
    private final AchievementService    achievements;

    public JournalService(JournalRepositoryPort repo, AchievementService achievements) {
        this.repo         = repo;
        this.achievements = achievements;
    }

    public List<BrewJournalEntry> getJournalEntries(int userId) {
        return repo.getJournalEntries(userId);
    }

    public Optional<BrewJournalEntry> findJournalEntry(int id, int userId) {
        return repo.findJournalEntry(id, userId);
    }

    public BrewJournalEntry createJournalEntry(
            int userId, String coffeeBean, String brewMethod,
            double doseGrams, double yieldGrams, int waterTempC,
            int rating, String notes, String weatherData) {

        BrewJournalEntry e = new BrewJournalEntry();
        e.setUserId(userId);
        e.setCoffeeBean(coffeeBean != null ? coffeeBean : "");
        e.setBrewMethod(brewMethod != null ? brewMethod : "");
        e.setDoseGrams(doseGrams);
        e.setYieldGrams(yieldGrams);
        e.setWaterTempC(waterTempC);
        e.setRatingAroma(rating);
        e.setRatingFlavor(rating);
        e.setRatingBody(rating);
        e.setRatingSweetness(rating);
        e.setRatingFinish(rating);
        e.setRatingAcidity(rating);
        e.setNotes(notes != null ? notes : "");
        e.setWeatherData(weatherData != null
                ? weatherData.substring(0, Math.min(weatherData.length(), 300))
                : "");

        BrewJournalEntry saved = repo.createJournalEntry(e);
        achievements.checkAndAwardAchievements(userId);
        return saved;
    }

    public BrewJournalEntry createJournalEntry(
            int userId, String coffeeBean, String origin,
            String roastLevel, String brewMethod, String grindSize, int waterTempC,
            double doseGrams, double yieldGrams, int brewTimeSec,
            int ratingAroma, int ratingFlavor, int ratingAcidity,
            int ratingBody, int ratingSweetness, int ratingFinish, String notes, String weatherData) {

        BrewJournalEntry e = new BrewJournalEntry();
        e.setUserId(userId);
        e.setCoffeeBean(coffeeBean != null ? coffeeBean : "");
        e.setOrigin(origin != null ? origin : "");
        e.setRoastLevel(roastLevel != null ? roastLevel : "");
        e.setBrewMethod(brewMethod != null ? brewMethod : "");
        e.setGrindSize(grindSize != null ? grindSize : "");
        e.setWaterTempC(waterTempC);
        e.setDoseGrams(doseGrams);
        e.setYieldGrams(yieldGrams);
        e.setBrewTimeSec(brewTimeSec);
        e.setRatingAroma(ratingAroma);
        e.setRatingFlavor(ratingFlavor);
        e.setRatingAcidity(ratingAcidity);
        e.setRatingBody(ratingBody);
        e.setRatingSweetness(ratingSweetness);
        e.setRatingFinish(ratingFinish);
        e.setNotes(notes != null ? notes : "");
        e.setWeatherData(weatherData != null
                ? weatherData.substring(0, Math.min(weatherData.length(), 300))
                : "");

        BrewJournalEntry saved = repo.createJournalEntry(e);
        achievements.checkAndAwardAchievements(userId);
        return saved;
    }

    public BrewJournalEntry updateJournalEntry(
            int id, int userId, String coffeeBean, String origin,
            String roastLevel, String brewMethod, String grindSize, int waterTempC,
            double doseGrams, double yieldGrams, int brewTimeSec,
            int ratingAroma, int ratingFlavor, int ratingAcidity,
            int ratingBody, int ratingSweetness, int ratingFinish, String notes, String weatherData) {

        BrewJournalEntry e = new BrewJournalEntry();
        e.setId(id);
        e.setUserId(userId);
        e.setCoffeeBean(coffeeBean != null ? coffeeBean : "");
        e.setOrigin(origin != null ? origin : "");
        e.setRoastLevel(roastLevel != null ? roastLevel : "");
        e.setBrewMethod(brewMethod != null ? brewMethod : "");
        e.setGrindSize(grindSize != null ? grindSize : "");
        e.setWaterTempC(waterTempC);
        e.setDoseGrams(doseGrams);
        e.setYieldGrams(yieldGrams);
        e.setBrewTimeSec(brewTimeSec);
        e.setRatingAroma(ratingAroma);
        e.setRatingFlavor(ratingFlavor);
        e.setRatingAcidity(ratingAcidity);
        e.setRatingBody(ratingBody);
        e.setRatingSweetness(ratingSweetness);
        e.setRatingFinish(ratingFinish);
        e.setNotes(notes != null ? notes : "");
        e.setWeatherData(weatherData != null
                ? weatherData.substring(0, Math.min(weatherData.length(), 300))
                : "");
        return repo.updateJournalEntry(e);
    }

    public boolean deleteJournalEntry(int id, int userId) {
        return repo.deleteJournalEntry(id, userId);
    }

    public List<BeanBag> getBeanBags(int userId)      { return repo.getBeanBags(userId); }
    public BeanBag createBeanBag(BeanBag b)           { achievements.checkAndAwardAchievements(b.getUserId()); return repo.createBeanBag(b); }
    public BeanBag updateBeanBag(BeanBag b)           { return repo.updateBeanBag(b); }
    public void deleteBeanBag(int id, int userId)     { repo.deleteBeanBag(id, userId); }
    public Map<String, Object> getJournalStats(int u) { return repo.getJournalStats(u); }
}
