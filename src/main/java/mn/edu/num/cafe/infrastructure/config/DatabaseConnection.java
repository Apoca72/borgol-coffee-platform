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
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("database.properties")) {
            if (is == null) {
                throw new RuntimeException("database.properties not found in classpath");
            }
            props.load(is);
            Class.forName(props.getProperty("db.driver"));
            connection = DriverManager.getConnection(
                    props.getProperty("db.url"),
                    props.getProperty("db.user"),
                    props.getProperty("db.password"));
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
