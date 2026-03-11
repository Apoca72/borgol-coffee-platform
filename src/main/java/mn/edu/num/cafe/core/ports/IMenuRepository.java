package mn.edu.num.cafe.core.ports;

import mn.edu.num.cafe.core.domain.MenuItem;

import java.util.List;
import java.util.Optional;

/**
 * Цэсний репозиторын port интерфейс (Secondary / Driven Port).
 *
 * Загвар: Hexagonal Architecture — Port & Adapter
 * Зорилго: core/ давхарга нь persistence технологиос (JDBC, InMemory г.м)
 *          тусгаарлагдана. Зөвхөн энэ интерфейсийг ашиглана.
 *
 * Архитектурын дүрэм: java.sql.* импорт хориглоно.
 */
public interface IMenuRepository {

    /** Цэсний зүйлийг хадгалах (байвал шинэчлэх, байхгүй бол нэмэх). */
    void save(MenuItem item);

    /** Бүх цэсний зүйлийг буцаах. */
    List<MenuItem> findAll();

    /** ID-аар хайх. */
    Optional<MenuItem> findById(int id);

    /** ID-аар устгах. */
    void deleteById(int id);
}
