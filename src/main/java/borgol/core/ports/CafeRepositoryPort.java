package borgol.core.ports;

import borgol.core.domain.CafeListing;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CafeRepositoryPort {
    List<CafeListing> findAllCafes(int currentUserId, String search, String district);
    Optional<CafeListing> findCafeById(int id, int currentUserId);
    CafeListing createCafe(CafeListing c);
    void updateCafeCoordinates(int cafeId, double lat, double lng);
    List<CafeListing> findCafesNearby(int currentUserId, double minLat, double maxLat, double minLng, double maxLng);
    boolean rateCafe(int userId, int cafeId, int rating, String review);
    boolean isCafesSeeded();
    Map<String, Object> checkIn(int cafeId, int userId, String note);
    List<Map<String, Object>> getCheckins(int cafeId);
}
