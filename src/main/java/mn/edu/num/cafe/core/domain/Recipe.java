package mn.edu.num.cafe.core.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe domain entity — coffee/drink recipe shared by users.
 */
public class Recipe {

    private int          id;
    private int          authorId;
    private String       authorUsername;
    private String       title;
    private String       description;
    private String       drinkType;    // ESPRESSO, LATTE, CAPPUCCINO, AMERICANO, COLD_BREW, POUR_OVER, FRENCH_PRESS, TEA, SMOOTHIE, OTHER
    private String       ingredients;  // newline-separated list
    private String       instructions; // step-by-step
    private int          brewTime;     // minutes
    private String       difficulty;   // EASY, MEDIUM, HARD
    private List<String> flavorTags;   // BITTER, SWEET, FRUITY, etc.
    private int          likesCount;
    private int          commentCount;
    private boolean      likedByCurrentUser;
    private String       imageUrl;
    private String       createdAt;

    public Recipe() {
        this.flavorTags = new ArrayList<>();
    }

    public Recipe(int authorId, String title, String drinkType) {
        this.authorId  = authorId;
        this.title     = title;
        this.drinkType = drinkType;
        this.flavorTags = new ArrayList<>();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int          getId()                  { return id; }
    public int          getAuthorId()            { return authorId; }
    public String       getAuthorUsername()      { return authorUsername; }
    public String       getTitle()               { return title; }
    public String       getDescription()         { return description; }
    public String       getDrinkType()           { return drinkType; }
    public String       getIngredients()         { return ingredients; }
    public String       getInstructions()        { return instructions; }
    public int          getBrewTime()            { return brewTime; }
    public String       getDifficulty()          { return difficulty; }
    public List<String> getFlavorTags()          { return flavorTags; }
    public int          getLikesCount()          { return likesCount; }
    public int          getCommentCount()        { return commentCount; }
    public boolean      isLikedByCurrentUser()   { return likedByCurrentUser; }
    public String       getImageUrl()            { return imageUrl; }
    public String       getCreatedAt()           { return createdAt; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setId(int id)                                 { this.id = id; }
    public void setAuthorId(int authorId)                     { this.authorId = authorId; }
    public void setAuthorUsername(String username)            { this.authorUsername = username; }
    public void setTitle(String title)                        { this.title = title; }
    public void setDescription(String description)            { this.description = description; }
    public void setDrinkType(String drinkType)                { this.drinkType = drinkType; }
    public void setIngredients(String ingredients)            { this.ingredients = ingredients; }
    public void setInstructions(String instructions)          { this.instructions = instructions; }
    public void setBrewTime(int brewTime)                     { this.brewTime = brewTime; }
    public void setDifficulty(String difficulty)              { this.difficulty = difficulty; }
    public void setFlavorTags(List<String> flavorTags)        { this.flavorTags = flavorTags; }
    public void setLikesCount(int count)                      { this.likesCount = count; }
    public void setCommentCount(int count)                    { this.commentCount = count; }
    public void setLikedByCurrentUser(boolean liked)          { this.likedByCurrentUser = liked; }
    public void setImageUrl(String imageUrl)                  { this.imageUrl = imageUrl; }
    public void setCreatedAt(String createdAt)                { this.createdAt = createdAt; }
}
