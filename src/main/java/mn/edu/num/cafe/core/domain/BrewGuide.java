package mn.edu.num.cafe.core.domain;

public class BrewGuide {
    private int id;
    private String methodName;
    private String description;
    private String difficulty;
    private int brewTimeMin;
    private String parameters;  // newline-separated "key:value" pairs
    private String steps;       // newline-separated step strings
    private String icon;
    private String createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public int getBrewTimeMin() { return brewTimeMin; }
    public void setBrewTimeMin(int brewTimeMin) { this.brewTimeMin = brewTimeMin; }

    public String getParameters() { return parameters; }
    public void setParameters(String parameters) { this.parameters = parameters; }

    public String getSteps() { return steps; }
    public void setSteps(String steps) { this.steps = steps; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
