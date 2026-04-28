package borgol.core.ports;

import borgol.core.domain.BeanBag;
import borgol.core.domain.BrewJournalEntry;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface JournalRepositoryPort {
    List<BrewJournalEntry> getJournalEntries(int userId);
    Optional<BrewJournalEntry> findJournalEntry(int id, int userId);
    BrewJournalEntry createJournalEntry(BrewJournalEntry e);
    BrewJournalEntry updateJournalEntry(BrewJournalEntry e);
    boolean deleteJournalEntry(int id, int userId);
    List<BeanBag> getBeanBags(int userId);
    BeanBag createBeanBag(BeanBag b);
    BeanBag updateBeanBag(BeanBag b);
    void deleteBeanBag(int id, int userId);
    Map<String, Object> getJournalStats(int userId);
}
