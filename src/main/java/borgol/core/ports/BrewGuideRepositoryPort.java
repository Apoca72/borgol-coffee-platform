package borgol.core.ports;

import borgol.core.domain.BrewGuide;
import borgol.core.domain.LearnArticle;
import java.util.List;
import java.util.Optional;

public interface BrewGuideRepositoryPort {
    List<BrewGuide> findAllBrewGuides();
    Optional<BrewGuide> findBrewGuideById(int id);
    List<LearnArticle> findAllLearnArticles();
    Optional<LearnArticle> findLearnArticleById(int id);
    boolean isStaticContentSeeded();
    void seedBrewGuide(BrewGuide g);
    void seedLearnArticle(LearnArticle a);
    boolean isBeanArticlesSeeded();
    boolean isDrinkArticlesSeeded();
}
