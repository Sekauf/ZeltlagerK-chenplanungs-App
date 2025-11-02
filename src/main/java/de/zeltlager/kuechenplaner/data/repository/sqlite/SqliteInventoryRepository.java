package de.zeltlager.kuechenplaner.data.repository.sqlite;

import de.zeltlager.kuechenplaner.data.model.InventoryItem;
import de.zeltlager.kuechenplaner.data.repository.InventoryRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * SQLite-based persistence for {@link InventoryItem} records.
 */
public class SqliteInventoryRepository implements InventoryRepository {

    private final Connection connection;

    public SqliteInventoryRepository(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "connection");
    }

    @Override
    public List<InventoryItem> findAll() {
        List<InventoryItem> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT ingredient, quantity, unit FROM inventory ORDER BY ingredient")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(mapRow(resultSet));
                }
            }
            return List.copyOf(result);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load inventory items", e);
        }
    }

    @Override
    public Optional<InventoryItem> findByIngredient(String ingredient) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT ingredient, quantity, unit FROM inventory WHERE LOWER(ingredient) = LOWER(?)")) {
            statement.setString(1, ingredient);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query inventory item", e);
        }
    }

    @Override
    public void save(InventoryItem item) {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO inventory (ingredient, quantity, unit) VALUES (?, ?, ?) " +
                        "ON CONFLICT(ingredient) DO UPDATE SET quantity = excluded.quantity, unit = excluded.unit")) {
            statement.setString(1, item.getIngredient());
            statement.setInt(2, item.getQuantity());
            statement.setString(3, item.getUnit());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save inventory item", e);
        }
    }

    private InventoryItem mapRow(ResultSet resultSet) throws SQLException {
        String ingredient = resultSet.getString("ingredient");
        int quantity = resultSet.getInt("quantity");
        String unit = resultSet.getString("unit");
        return new InventoryItem(ingredient, quantity, unit);
    }
}
