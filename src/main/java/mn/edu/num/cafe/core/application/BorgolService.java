package mn.edu.num.cafe.core.application;

import mn.edu.num.cafe.core.domain.*;
import mn.edu.num.cafe.infrastructure.persistence.BorgolRepository;
import mn.edu.num.cafe.infrastructure.security.JwtUtil;
import mn.edu.num.cafe.infrastructure.security.PasswordUtil;

import java.util.*;

/**
 * Main application service for the Borgol Coffee Enthusiast Platform.
 *
 * Handles: authentication, user management, recipes, cafes, social features.
 */
public class BorgolService {

    private final BorgolRepository repo;

    public BorgolService(BorgolRepository repo) {
        this.repo = repo;
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    public record AuthResult(String token, UserView user) {}

    public AuthResult register(String username, String email, String password) {
        if (username == null || username.isBlank())  throw new IllegalArgumentException("Username is required");
        if (email == null    || email.isBlank())     throw new IllegalArgumentException("Email is required");
        if (password == null || password.length() < 6) throw new IllegalArgumentException("Password must be ≥ 6 characters");
        if (!email.contains("@"))                    throw new IllegalArgumentException("Invalid email address");
        if (username.length() < 3 || username.length() > 50) throw new IllegalArgumentException("Username must be 3–50 characters");
        if (!username.matches("[a-zA-Z0-9_]+"))     throw new IllegalArgumentException("Username: letters, numbers, underscores only");

        if (repo.findUserByEmail(email).isPresent())      throw new IllegalArgumentException("Email already registered");
        if (repo.findUserByUsername(username).isPresent()) throw new IllegalArgumentException("Username already taken");

        String hash = PasswordUtil.hash(password);
        User user   = repo.createUser(username, email, hash);
        String token = JwtUtil.createToken(user.getId(), user.getUsername());
        return new AuthResult(token, toView(user, 0, false));
    }

    public AuthResult login(String email, String password) {
        if (email == null || email.isBlank())       throw new IllegalArgumentException("Email is required");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password is required");

        User user = repo.findUserByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!PasswordUtil.verify(password, user.getPasswordHash()))
            throw new IllegalArgumentException("Invalid email or password");

        String token = JwtUtil.createToken(user.getId(), user.getUsername());
        return new AuthResult(token, toView(user, 0, false));
    }

    public UserView getMe(int userId) {
        User user = repo.findUserById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toView(user, 0, false);
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    public UserView getUserProfile(int targetId, int currentUserId) {
        User user = repo.findUserById(targetId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean isFollowing = currentUserId > 0 && repo.isFollowing(currentUserId, targetId);
        return toView(user, currentUserId, isFollowing);
    }

    public UserView updateProfile(int userId, String bio, String avatarUrl, String expertiseLevel,
                                  List<String> flavorPrefs) {
        // Validate expertise level
        List<String> validLevels = List.of("BEGINNER", "ENTHUSIAST", "BARISTA", "EXPERT");
        if (expertiseLevel != null && !validLevels.contains(expertiseLevel.toUpperCase()))
            expertiseLevel = "BEGINNER";

        repo.updateUser(userId,
            bio != null ? bio : "",
            avatarUrl != null ? avatarUrl : "",
            expertiseLevel != null ? expertiseLevel.toUpperCase() : "BEGINNER");

        if (flavorPrefs != null) repo.setUserFlavorPrefs(userId, flavorPrefs);

        return getMe(userId);
    }

    public void followUser(int followerId, int followingId) {
        if (followerId == followingId) throw new IllegalArgumentException("Cannot follow yourself");
        repo.followUser(followerId, followingId);
    }

    public void unfollowUser(int followerId, int followingId) {
        repo.unfollowUser(followerId, followingId);
    }

    public List<UserView> searchUsers(String query, int currentUserId) {
        if (query == null || query.isBlank()) return List.of();
        return repo.searchUsers(query).stream()
            .map(u -> {
                boolean following = currentUserId > 0 && repo.isFollowing(currentUserId, u.getId());
                return toView(u, currentUserId, following);
            }).toList();
    }

    // ── Recipes ───────────────────────────────────────────────────────────────

    public List<Recipe> getRecipes(int currentUserId, String search, String drinkType, String sort) {
        return repo.findAllRecipes(currentUserId, search, drinkType, sort);
    }

    public List<Recipe> getFeed(int userId) {
        return repo.getFeedRecipes(userId, 30);
    }

    public List<Recipe> getUserRecipes(int authorId, int currentUserId) {
        return repo.getUserRecipes(authorId, currentUserId);
    }

    public Recipe getRecipe(int id, int currentUserId) {
        return repo.findRecipeById(id, currentUserId)
            .orElseThrow(() -> new IllegalArgumentException("Recipe not found: id=" + id));
    }

    public Recipe createRecipe(int authorId, String title, String description, String drinkType,
                                String ingredients, String instructions, int brewTime,
                                String difficulty, List<String> flavorTags, String imageUrl) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title is required");
        if (drinkType == null || drinkType.isBlank()) drinkType = "COFFEE";

        Recipe r = new Recipe();
        r.setAuthorId(authorId);
        r.setTitle(title.trim());
        r.setDescription(description);
        r.setDrinkType(drinkType.toUpperCase());
        r.setIngredients(ingredients);
        r.setInstructions(instructions);
        r.setBrewTime(Math.max(0, brewTime));
        r.setDifficulty(difficulty != null ? difficulty.toUpperCase() : "MEDIUM");
        r.setFlavorTags(flavorTags != null ? flavorTags : List.of());
        r.setImageUrl(imageUrl != null ? imageUrl : "");
        return repo.createRecipe(r);
    }

    public Recipe updateRecipe(int recipeId, int authorId, String title, String description,
                                String drinkType, String ingredients, String instructions,
                                int brewTime, String difficulty, List<String> flavorTags, String imageUrl) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title is required");

        Recipe r = new Recipe();
        r.setId(recipeId);
        r.setAuthorId(authorId);
        r.setTitle(title.trim());
        r.setDescription(description);
        r.setDrinkType(drinkType != null ? drinkType.toUpperCase() : "COFFEE");
        r.setIngredients(ingredients);
        r.setInstructions(instructions);
        r.setBrewTime(Math.max(0, brewTime));
        r.setDifficulty(difficulty != null ? difficulty.toUpperCase() : "MEDIUM");
        r.setFlavorTags(flavorTags != null ? flavorTags : List.of());
        r.setImageUrl(imageUrl != null ? imageUrl : "");
        return repo.updateRecipe(r);
    }

    public void deleteRecipe(int recipeId, int userId) {
        boolean deleted = repo.deleteRecipe(recipeId, userId);
        if (!deleted) throw new IllegalArgumentException("Recipe not found or not authorized");
    }

    public Map<String, Object> toggleLike(int userId, int recipeId) {
        boolean liked;
        if (repo.findRecipeById(recipeId, userId)
                .map(Recipe::isLikedByCurrentUser).orElse(false)) {
            repo.unlikeRecipe(userId, recipeId);
            liked = false;
        } else {
            repo.likeRecipe(userId, recipeId);
            liked = true;
        }
        int count = repo.findRecipeById(recipeId, userId)
            .map(Recipe::getLikesCount).orElse(0);
        return Map.of("liked", liked, "likesCount", count);
    }

    // ── Comments ──────────────────────────────────────────────────────────────

    public List<RecipeComment> getComments(int recipeId) {
        return repo.findCommentsByRecipeId(recipeId);
    }

    public RecipeComment addComment(int recipeId, int authorId, String content) {
        if (content == null || content.isBlank()) throw new IllegalArgumentException("Comment cannot be empty");
        if (content.length() > 1000) throw new IllegalArgumentException("Comment too long (max 1000 chars)");
        return repo.addComment(recipeId, authorId, content);
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
        return getCafe(cafeId, userId);
    }

    // ── Extra queries ─────────────────────────────────────────────────────────

    public List<UserView> getAllUsers(int currentUserId) {
        return repo.findAllUsers(60).stream()
            .map(u -> {
                boolean following = currentUserId > 0 && repo.isFollowing(currentUserId, u.getId());
                return toView(u, currentUserId, following);
            }).toList();
    }

    public List<Recipe> getLikedRecipes(int userId, int currentUserId) {
        return repo.getLikedRecipes(userId, currentUserId);
    }

    public List<UserView> getFollowingUsers(int userId, int currentUserId) {
        return repo.getFollowingUsers(userId).stream()
            .map(u -> {
                boolean following = currentUserId > 0 && repo.isFollowing(currentUserId, u.getId());
                return toView(u, currentUserId, following);
            }).toList();
    }

    public List<UserView> getFollowerUsers(int userId, int currentUserId) {
        return repo.getFollowerUsers(userId).stream()
            .map(u -> {
                boolean following = currentUserId > 0 && repo.isFollowing(currentUserId, u.getId());
                return toView(u, currentUserId, following);
            }).toList();
    }

    // ── View mapping ──────────────────────────────────────────────────────────

    public record UserView(
        int id, String username, String email, String bio, String avatarUrl,
        String expertiseLevel, List<String> flavorPrefs,
        int followerCount, int followingCount, int recipeCount,
        boolean isFollowing, String createdAt
    ) {}

    private UserView toView(User u, int currentUserId, boolean isFollowing) {
        List<String> prefs = repo.getUserFlavorPrefs(u.getId());
        return new UserView(
            u.getId(), u.getUsername(), u.getEmail(),
            u.getBio(), u.getAvatarUrl(), u.getExpertiseLevel(),
            prefs,
            repo.getFollowerCount(u.getId()),
            repo.getFollowingCount(u.getId()),
            repo.getUserRecipeCount(u.getId()),
            isFollowing,
            u.getCreatedAt()
        );
    }
}
