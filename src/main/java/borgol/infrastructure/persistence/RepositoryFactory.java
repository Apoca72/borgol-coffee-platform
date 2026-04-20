package borgol.infrastructure.persistence;

import borgol.core.ports.IMenuRepository;
import borgol.infrastructure.config.DatabaseConnection;

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
        String raw  = db.getProperty("app.persistence.mode");
        String mode = (raw != null) ? raw.trim().toUpperCase() : "DB";
        return switch (mode) {
            case "DB"  -> new JdbcMenuRepository(db);
            case "MEM" -> new InMemoryMenuRepository();
            default    -> throw new IllegalArgumentException(
                    "Unknown persistence mode: '" + mode + "'. Use DB or MEM.");
        };
    }
}
