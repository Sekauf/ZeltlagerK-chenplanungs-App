package de.zeltlager.kuechenplaner.data.repository.sqlite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages the lifecycle of the SQLite database connection and schema.
 */
public final class SqliteDatabase implements AutoCloseable {

    private final Connection connection;

    public SqliteDatabase(Path databaseFile) {
        try {
            ensureParentDirectoryExists(databaseFile);
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.toAbsolutePath());
            initializeSchema();
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Failed to initialize SQLite database", e);
        }
    }

    private void ensureParentDirectoryExists(Path databaseFile) throws IOException {
        Path parent = databaseFile.toAbsolutePath().getParent();
        if (parent != null && Files.notExists(parent)) {
            Files.createDirectories(parent);
        }
    }

    private void initializeSchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS inventory (" +
                    "ingredient TEXT PRIMARY KEY, " +
                    "quantity INTEGER NOT NULL, " +
                    "unit TEXT NOT NULL" +
                    ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS menu_plan (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "date TEXT NOT NULL, " +
                    "meal_name TEXT NOT NULL, " +
                    "servings INTEGER NOT NULL" +
                    ")");
        }
    }

    public Connection getConnection() {
        return connection;
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to close SQLite connection", e);
        }
    }
}
