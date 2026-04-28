package borgol.core.ports;

import borgol.core.domain.User;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserRepositoryPort {
    Optional<User> findUserById(int id);
    Optional<User> findUserByEmail(String email);
    Optional<User> findUserByUsername(String username);
    User createUser(String username, String email, String passwordHash);
    void deleteUser(int id);
    void updateUser(int id, String bio, String avatarUrl, String expertiseLevel);
    List<String> getUserFlavorPrefs(int userId);
    void setUserFlavorPrefs(int userId, List<String> flavors);
    int getFollowerCount(int userId);
    int getFollowingCount(int userId);
    int getUserRecipeCount(int userId);
    boolean isFollowing(int followerId, int followingId);
    void followUser(int followerId, int followingId);
    void unfollowUser(int followerId, int followingId);
    List<User> searchUsers(String query);
    List<User> findAllUsers(int limit);
    List<User> getFollowingUsers(int userId);
    List<User> getFollowerUsers(int userId);
    void blockUser(int blockerId, int blockedId);
    void unblockUser(int blockerId, int blockedId);
    boolean isBlocked(int blockerId, int blockedId);
    void followHashtag(int userId, String tag);
    void unfollowHashtag(int userId, String tag);
    List<String> getUserHashtags(int userId);
    void createNotification(int userId, String type, int fromUserId, int contentId, String message);
    List<Map<String, Object>> getNotifications(int userId, int limit);
    void markNotificationsRead(int userId);
    int getUnreadNotificationCount(int userId);
}
