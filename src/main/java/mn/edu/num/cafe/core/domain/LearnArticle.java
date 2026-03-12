package mn.edu.num.cafe.core.domain;

public class LearnArticle {
    private int id;
    private String title;
    private String category;
    private String content;
    private String icon;
    private int readTimeMin;
    private String createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public int getReadTimeMin() { return readTimeMin; }
    public void setReadTimeMin(int readTimeMin) { this.readTimeMin = readTimeMin; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
