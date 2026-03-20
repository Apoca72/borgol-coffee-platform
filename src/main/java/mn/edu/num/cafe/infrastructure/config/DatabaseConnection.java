package mn.edu.num.cafe.infrastructure.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Өгөгдлийн сангийн холболт — Double-Checked Locking Singleton.
 *
 * Загвар: Singleton (GoF) — thread-safe, lazy initialization.
 *
 * Зорилго: Програмын туршид ганц нэг холболтын объект байна.
 * volatile keyword: JVM-ийн instruction reordering-ээс хамгаална.
 */
public class DatabaseConnection {

    private static volatile DatabaseConnection instance;
    private final Properties props;
    private Connection connection;

    private DatabaseConnection() {
        props = new Properties();

        // Load properties file if present (local / Eclipse)
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("database.properties")) {
            if (is != null) props.load(is);
        } catch (Exception ignored) {}

        // Environment variables override properties file (Railway / Docker / cloud)
        // DB_URL not set → use in-memory H2 so Railway starts without a volume
        String dbUrl    = System.getenv().getOrDefault("DB_URL",
                          props.getProperty("db.url",
                          "jdbc:h2:mem:cafe_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"));
        String dbUser   = System.getenv().getOrDefault("DB_USER",
                          props.getProperty("db.user", "sa"));
        String dbPass   = System.getenv().getOrDefault("DB_PASSWORD",
                          props.getProperty("db.password", ""));
        String dbDriver = props.getProperty("db.driver", "org.h2.Driver");

        // Write resolved values back so getProperty() works elsewhere
        props.setProperty("db.url",      dbUrl);
        props.setProperty("db.user",     dbUser);
        props.setProperty("db.password", dbPass);
        props.setProperty("db.driver",   dbDriver);

        System.out.println("  [DB] Connecting to: " + dbUrl);
        try {
            Class.forName(dbDriver);
            connection = DriverManager.getConnection(dbUrl, dbUser, dbPass);
        } catch (Exception e) {
            throw new RuntimeException("Database connection failed", e);
        }
    }

    /** Double-Checked Locking — thread-safe Singleton хандалт. */
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    /** Холболтыг буцаана; хаагдсан бол дахин нээнэ. */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(
                        props.getProperty("db.url"),
                        props.getProperty("db.user"),
                        props.getProperty("db.password"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to reconnect to database", e);
        }
        return connection;
    }

    /** database.properties-с утга уншина. */
    public String getProperty(String key) {
        return props.getProperty(key);
    }
}
