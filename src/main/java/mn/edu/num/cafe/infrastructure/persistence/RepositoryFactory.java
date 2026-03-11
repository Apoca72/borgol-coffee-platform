package mn.edu.num.cafe.infrastructure.persistence;

import mn.edu.num.cafe.core.ports.IMenuRepository;
import mn.edu.num.cafe.infrastructure.config.DatabaseConnection;

/**
 * Репозиторын Factory — database.properties тохиргооноос репозиторыг үүсгэнэ.
 *
 * Загвар: Factory (GoF) — concrete классыг нуун IMenuRepository порт буцаана.
 *
 * app.persistence.mode утгаас хамааран:
 *   DB  → JdbcMenuRepository  (H2 файл өгөгдлийн сан)
 *   MEM → InMemoryMenuRepository (санах ой, тест орчинд)
 */
public class RepositoryFactory {

    private RepositoryFactory() { /* utility class — instantiation хориглоно */ }

    public static IMenuRepository createMenuRepository() {
        DatabaseConnection db = DatabaseConnection.getInstance();
        String mode = db.getProperty("app.persistence.mode");
        return switch (mode.trim().toUpperCase()) {
            case "DB"  -> new JdbcMenuRepository(db);
            case "MEM" -> new InMemoryMenuRepository();
            default    -> throw new IllegalArgumentException(
                    "Unknown persistence mode: '" + mode + "'. Use DB or MEM.");
        };
    }
}
