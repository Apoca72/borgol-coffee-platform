package mn.edu.num.cafe.infrastructure.persistence;

import mn.edu.num.cafe.core.domain.MenuItem;
import mn.edu.num.cafe.core.ports.IMenuRepository;

import java.util.*;

/**
 * IMenuRepository-ийн санах ойн хэрэгжүүлэлт.
 *
 * Зорилго: Тестийн орчинд эсвэл DB тохиргоогүй үед ашиглана.
 * database.properties дахь app.persistence.mode=MEM тохиргооноос идэвхжинэ.
 */
public class InMemoryMenuRepository implements IMenuRepository {

    private final Map<Integer, MenuItem> store = new LinkedHashMap<>();

    @Override
    public void save(MenuItem item) {
        store.put(item.getId(), item);
    }

    @Override
    public List<MenuItem> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<MenuItem> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public void deleteById(int id) {
        store.remove(id);
    }
}
