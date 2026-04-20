package borgol.core.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * User domain entity — Borgol platform member.
 */
public class User {

    private final int    id;
    private String       username;
    private String       email;
    private String       passwordHash;
    private String       bio;
    private String       avatarUrl;
    private String       expertiseLevel; // BEGINNER, ENTHUSIAST, BARISTA, EXPERT
    private List<String> flavorPrefs;
    private int          followerCount;
    private int          followingCount;
    private int          recipeCount;
    private String       createdAt;

    public User(int id, String username, String email, String passwordHash) {
        this.id            = id;
        this.username      = username;
        this.email         = email;
        this.passwordHash  = passwordHash;
        this.bio           = "";
        this.avatarUrl     = "";
        this.expertiseLevel = "BEGINNER";
        this.flavorPrefs   = new ArrayList<>();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int          getId()             { return id; }
    public String       getUsername()       { return username; }
    public String       getEmail()          { return email; }
    public String       getPasswordHash()   { return passwordHash; }
    public String       getBio()            { return bio; }
    public String       getAvatarUrl()      { return avatarUrl; }
    public String       getExpertiseLevel() { return expertiseLevel; }
    public List<String> getFlavorPrefs()    { return flavorPrefs; }
    public int          getFollowerCount()  { return followerCount; }
    public int          getFollowingCount() { return followingCount; }
    public int          getRecipeCount()    { return recipeCount; }
    public String       getCreatedAt()      { return createdAt; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setBio(String bio)                       { this.bio = bio; }
    public void setAvatarUrl(String avatarUrl)           { this.avatarUrl = avatarUrl; }
    public void setExpertiseLevel(String level)          { this.expertiseLevel = level; }
    public void setFlavorPrefs(List<String> prefs)       { this.flavorPrefs = prefs; }
    public void setFollowerCount(int count)              { this.followerCount = count; }
    public void setFollowingCount(int count)             { this.followingCount = count; }
    public void setRecipeCount(int count)                { this.recipeCount = count; }
    public void setCreatedAt(String createdAt)           { this.createdAt = createdAt; }
}
