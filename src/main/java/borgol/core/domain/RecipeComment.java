package borgol.core.domain;

/**
 * Comment on a recipe — social interaction entity.
 */
public class RecipeComment {

    private int    id;
    private int    recipeId;
    private int    authorId;
    private String authorUsername;
    private String content;
    private String createdAt;

    public RecipeComment() {}

    public RecipeComment(int recipeId, int authorId, String content) {
        this.recipeId = recipeId;
        this.authorId = authorId;
        this.content  = content;
    }

    public int    getId()             { return id; }
    public int    getRecipeId()       { return recipeId; }
    public int    getAuthorId()       { return authorId; }
    public String getAuthorUsername() { return authorUsername; }
    public String getContent()        { return content; }
    public String getCreatedAt()      { return createdAt; }

    public void setId(int id)                          { this.id = id; }
    public void setRecipeId(int recipeId)              { this.recipeId = recipeId; }
    public void setAuthorId(int authorId)              { this.authorId = authorId; }
    public void setAuthorUsername(String username)     { this.authorUsername = username; }
    public void setContent(String content)             { this.content = content; }
    public void setCreatedAt(String createdAt)         { this.createdAt = createdAt; }
}
