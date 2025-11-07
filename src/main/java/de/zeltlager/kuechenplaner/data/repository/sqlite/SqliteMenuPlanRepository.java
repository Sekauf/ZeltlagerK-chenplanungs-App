package de.zeltlager.kuechenplaner.data.repository.sqlite;

import de.zeltlager.kuechenplaner.data.model.Meal;
import de.zeltlager.kuechenplaner.data.model.MenuPlanEntry;
import de.zeltlager.kuechenplaner.data.repository.MenuPlanRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SQLite implementation of {@link MenuPlanRepository}.
 */
public class SqliteMenuPlanRepository implements MenuPlanRepository {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter LEGACY_DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter[] SUPPORTED_DATE_TIME_FORMATTERS = {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_DATE_TIME,
            LEGACY_DATE_TIME_FORMATTER
    };

    private final Connection connection;

    public SqliteMenuPlanRepository(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "connection");
    }

    @Override
    public List<MenuPlanEntry> findAll() {
        return queryEntries("SELECT date, meal_name, servings FROM menu_plan ORDER BY date");
    }

    @Override
    public List<MenuPlanEntry> findByDate(LocalDate date) {
        return queryEntries("SELECT date, meal_name, servings FROM menu_plan WHERE date = ? ORDER BY date",
                DATE_FORMATTER.format(date));
    }

    @Override
    public void save(MenuPlanEntry entry) {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO menu_plan (date, meal_name, servings) VALUES (?, ?, ?)")) {
            statement.setString(1, DATE_FORMATTER.format(entry.getDate()));
            statement.setString(2, entry.getMeal().getName());
            statement.setInt(3, entry.getMeal().getServings());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save menu plan entry", e);
        }
    }

    private List<MenuPlanEntry> queryEntries(String sql, String... parameters) {
        List<MenuPlanEntry> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setString(i + 1, parameters[i]);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(mapRow(resultSet));
                }
            }
            return List.copyOf(result);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query menu plan entries", e);
        }
    }

    private MenuPlanEntry mapRow(ResultSet resultSet) throws SQLException {
        String dateValue = resultSet.getString("date");
        LocalDate date = parseDate(dateValue);
        String mealName = resultSet.getString("meal_name");
        int servings = resultSet.getInt("servings");
        return new MenuPlanEntry(date, new Meal(mealName, servings));
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException dateParseException) {
            for (DateTimeFormatter formatter : SUPPORTED_DATE_TIME_FORMATTERS) {
                try {
                    return LocalDateTime.parse(value, formatter).toLocalDate();
                } catch (DateTimeParseException ignored) {
                    // try next formatter
                }
            }
            throw new IllegalStateException("Unsupported date format: " + value, dateParseException);
        }
    }
}
