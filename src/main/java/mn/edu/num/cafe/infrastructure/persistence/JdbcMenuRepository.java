package mn.edu.num.cafe.infrastructure.persistence;

import mn.edu.num.cafe.core.domain.MenuCategory;
import mn.edu.num.cafe.core.domain.MenuItem;
import mn.edu.num.cafe.core.ports.IMenuRepository;
import mn.edu.num.cafe.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * IMenuRepository-ийн JDBC хэрэгжүүлэлт — DAO + Outbound Adapter.
 *
 * Загвар: DAO (Data Access Object) + Adapter (Hexagonal Architecture)
 * Зорилго: H2 файл өгөгдлийн санд цэсний зүйлийг хадгалах, унших.
 *
 * Аюулгүй байдал: SQL Injection-оос сэргийлэхийн тулд
 * бүх query-д PreparedStatement ашиглана. String concatenation хатуу хориглоно.
 */
public class JdbcMenuRepository implements IMenuRepository {

    private final DatabaseConnection db;

    public JdbcMenuRepository(DatabaseConnection db) {
        this.db = db;
        initTable();
    }

    /** Хүснэгт байхгүй бол үүсгэнэ. */
    private void initTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS menu_item (
                    id        INT          PRIMARY KEY,
                    name      VARCHAR(255) NOT NULL,
                    category  VARCHAR(50)  NOT NULL,
                    price     DOUBLE       NOT NULL DEFAULT 0.0,
                    available BOOLEAN      NOT NULL DEFAULT TRUE
                )
                """;
        try (Statement stmt = db.getConnection().createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create menu_item table", e);
        }
    }

    /**
     * Цэсний зүйлийг хадгална (байвал шинэчлэх — MERGE INTO).
     * PreparedStatement: SQL Injection-оос хамгаална.
     */
    @Override
    public void save(MenuItem item) {
        String sql = """
                MERGE INTO menu_item (id, name, category, price, available)
                KEY(id)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1,     item.getId());
            ps.setString(2,  item.getName());
            ps.setString(3,  item.getCategory().name());
            ps.setDouble(4,  item.getPrice());
            ps.setBoolean(5, item.isAvailable());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save menu item: id=" + item.getId(), e);
        }
    }

    /** Бүх цэсний зүйлийг буцаана. */
    @Override
    public List<MenuItem> findAll() {
        String sql = "SELECT id, name, category, price, available FROM menu_item ORDER BY id";
        List<MenuItem> result = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch menu items", e);
        }
        return result;
    }

    /** ID-аар цэсний зүйл хайна. */
    @Override
    public Optional<MenuItem> findById(int id) {
        String sql = "SELECT id, name, category, price, available FROM menu_item WHERE id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find menu item: id=" + id, e);
        }
        return Optional.empty();
    }

    /** ID-аар цэсний зүйл устгана. */
    @Override
    public void deleteById(int id) {
        String sql = "DELETE FROM menu_item WHERE id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete menu item: id=" + id, e);
        }
    }

    /**
     * ResultSet-ийн нэг мөрийг MenuItem domain объект болгон хөрвүүлнэ.
     * SQL-ийн нарийн ширийнийг core domain-аас тусгаарлана.
     */
    private MenuItem mapRow(ResultSet rs) throws SQLException {
        return new MenuItem(
                rs.getInt("id"),
                rs.getString("name"),
                MenuCategory.valueOf(rs.getString("category")),
                rs.getDouble("price"),
                rs.getBoolean("available")
        );
    }
}
