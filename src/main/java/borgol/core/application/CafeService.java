package borgol.core.application;

import borgol.core.domain.CafeListing;
import borgol.core.ports.CafeRepositoryPort;
import borgol.infrastructure.cache.CacheKeyBuilder;
import borgol.infrastructure.cache.RedisClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class CafeService {

    private static final Logger log = LoggerFactory.getLogger(CafeService.class);

    private final CafeRepositoryPort repo;
    private final AchievementService achievements;
    private final Gson               gson = new Gson();

    public CafeService(CafeRepositoryPort repo, AchievementService achievements) {
        this.repo         = repo;
        this.achievements = achievements;
    }

    // ── Cache helpers ─────────────────────────────────────────────────────────

    private <T> T cacheGet(String key, Type type) {
        try {
            String json = RedisClient.get().get(key);
            if (json != null) {
                log.debug("[Cache HIT] {}", key);
                return gson.fromJson(json, type);
            }
        } catch (Exception e) {
            log.debug("[Cache] Redis уншихад алдаа: {} — {}", key, e.getMessage());
        }
        log.debug("[Cache MISS] {}", key);
        return null;
    }

    private void cachePut(String key, Object value, int ttlSeconds) {
        try {
            RedisClient.get().setex(key, ttlSeconds, gson.toJson(value));
        } catch (Exception e) {
            log.debug("[Cache] Redis бичихэд алдаа: {} — {}", key, e.getMessage());
        }
    }

    // ── Cafes ─────────────────────────────────────────────────────────────────

    public List<CafeListing> getCafes(int currentUserId, String search, String district) {
        return repo.findAllCafes(currentUserId, search, district);
    }

    public CafeListing getCafe(int id, int currentUserId) {
        return repo.findCafeById(id, currentUserId)
            .orElseThrow(() -> new IllegalArgumentException("Cafe not found: id=" + id));
    }

    public CafeListing createCafe(int submittedBy, String name, String address, String district,
                                   String city, String phone, String description, String hours,
                                   String imageUrl) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Cafe name is required");

        CafeListing c = new CafeListing();
        c.setName(name.trim());
        c.setAddress(address);
        c.setDistrict(district);
        c.setCity(city != null && !city.isBlank() ? city : "Ulaanbaatar");
        c.setPhone(phone);
        c.setDescription(description);
        c.setHours(hours);
        c.setImageUrl(imageUrl != null ? imageUrl : "");
        c.setSubmittedBy(submittedBy);
        return repo.createCafe(c);
    }

    public CafeListing rateCafe(int userId, int cafeId, int rating, String review) {
        repo.rateCafe(userId, cafeId, rating, review);
        achievements.checkAndAwardAchievements(userId);
        return getCafe(cafeId, userId);
    }

    public void updateCafeCoordinates(int cafeId, double lat, double lng) {
        repo.updateCafeCoordinates(cafeId, lat, lng);
    }

    public List<CafeListing> getCafesNearby(int currentUserId, double lat, double lng, double radiusKm) {
        String key = CacheKeyBuilder.forCafesNearby(lat, lng);
        Type listType = new TypeToken<List<CafeListing>>(){}.getType();
        List<CafeListing> cached = cacheGet(key, listType);
        if (cached != null) return cached;

        double latDelta = radiusKm / 111.0;
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));
        List<CafeListing> result = repo.findCafesNearby(currentUserId,
            lat - latDelta, lat + latDelta,
            lng - lngDelta, lng + lngDelta);
        cachePut(key, result, 120);
        return result;
    }

    // ── Check-ins ─────────────────────────────────────────────────────────────

    public Map<String, Object> checkIn(int cafeId, int userId, String note) {
        Map<String, Object> result = repo.checkIn(cafeId, userId, note != null ? note : "");
        achievements.checkAndAwardAchievements(userId);
        return result;
    }

    public List<Map<String, Object>> getCheckins(int cafeId) {
        return repo.getCheckins(cafeId);
    }

    // ── Seeding ───────────────────────────────────────────────────────────────

    public void seedCafes() {
        if (repo.isCafesSeeded()) return;

        seedCafe("Luna Blanca", "Peace Ave 15, Sukhbaatar District", "Sukhbaatar",
            "A beloved specialty coffee and brunch spot near the State Department Store. Known for single-origin pour overs and all-day breakfast.",
            "08:00–22:00", 47.9184, 106.9177);
        seedCafe("Nomads Coffee", "Olympic Street 12, Sukhbaatar District", "Sukhbaatar",
            "Local specialty roastery and café. Roasts their own beans in-house, with a rotating menu of Mongolian and imported single-origins.",
            "09:00–21:00", 47.9161, 106.9203);
        seedCafe("Café Amsterdam", "Baga Toiruu 6, Sukhbaatar District", "Sukhbaatar",
            "European-style café with fresh pastries, strong espresso, and a relaxed atmosphere. Popular with expats and students.",
            "08:30–21:30", 47.9175, 106.9155);
        seedCafe("Coffee Lab UB", "Seoul Street 34, Sukhbaatar District", "Sukhbaatar",
            "Specialty coffee training lab and tasting bar. Offers cupping sessions, barista courses, and a retail bean selection.",
            "10:00–20:00", 47.9143, 106.9220);
        seedCafe("Rocky Mountain Coffee", "Ulaanbaatar Hotel, Sukhbaatar District", "Sukhbaatar",
            "International specialty chain with consistent quality. Full espresso bar, seasonal drinks, and free Wi-Fi.",
            "07:00–22:00", 47.9203, 106.9244);
        seedCafe("Merkuri Coffee", "Tsagdaagiin Gudamj 8, Bayangol District", "Bayangol",
            "Minimalist pour-over focused café. Clean design, focused menu, and some of the best manual brew coffee in the city.",
            "09:00–20:00", 47.9098, 106.8901);
        seedCafe("Grand Coffee", "Narnii Road 22, Bayanzurkh District", "Bayanzurkh",
            "Large, comfortable café ideal for work and meetings. Extensive food menu alongside a full espresso bar.",
            "08:00–23:00", 47.9226, 106.9498);
        seedCafe("Espresso Yourself", "Zaisan Area, Khan-Uul District", "Khan-Uul",
            "Cozy neighborhood café in the Zaisan hills. Specialty roasts, homemade cakes, and a stunning view of the city.",
            "09:00–21:00", 47.8912, 106.9063);
    }

    private void seedCafe(String name, String address, String district,
                          String description, String hours, double lat, double lng) {
        CafeListing c = new CafeListing();
        c.setName(name);
        c.setAddress(address);
        c.setDistrict(district);
        c.setCity("Ulaanbaatar");
        c.setDescription(description);
        c.setHours(hours);
        c.setLat(lat);
        c.setLng(lng);
        c.setSubmittedBy(0);
        repo.createCafe(c);
    }
}
