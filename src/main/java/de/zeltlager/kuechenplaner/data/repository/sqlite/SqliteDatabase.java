package de.zeltlager.kuechenplaner.data.repository.sqlite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages the lifecycle of the SQLite database connection and schema.
 */
public final class SqliteDatabase implements AutoCloseable {

    private final Connection connection;
    private final Path databaseFile;

    public SqliteDatabase(Path databaseFile) {
        try {
            this.databaseFile = databaseFile.toAbsolutePath();
            ensureParentDirectoryExists(this.databaseFile);
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.databaseFile);
            enableForeignKeyEnforcement();
            initializeSchema();
            populateSampleDataIfEmpty();
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

    private void enableForeignKeyEnforcement() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
    }

    private void populateSampleDataIfEmpty() throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM recipes")) {
            if (!resultSet.next() || resultSet.getInt(1) > 0) {
                return;
            }
        }

        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            long spaghettiId = insertSampleRecipe(
                    "Spaghetti Bolognese",
                    null,
                    20,
                    "Spaghetti nach Packungsanweisung kochen. Hackfleisch anbraten, Gemüse zufügen "
                            + "und mit Tomaten sowie Gewürzen abschmecken. Mit den Nudeln vermengen.");
            insertSampleIngredient(spaghettiId, "Spaghetti", "g", 90.0, "Vollkornnudeln bevorzugt");
            insertSampleIngredient(spaghettiId, "Rinderhackfleisch", "g", 55.0, null);
            insertSampleIngredient(spaghettiId, "Passierte Tomaten", "ml", 80.0, null);
            insertSampleIngredient(spaghettiId, "Zwiebeln", "Stück", 0.05, "fein würfeln");
            insertSampleIngredient(spaghettiId, "Knoblauchzehen", "Stück", 0.05, "gepresst");

            long curryId = insertSampleRecipe(
                    "Gemüse-Curry mit Reis",
                    null,
                    15,
                    "Gemüse in Würfeln anbraten, Currypaste zugeben und mit Kokosmilch ablöschen. "
                            + "Kichererbsen hinzufügen und köcheln lassen. Mit Reis servieren.");
            insertSampleIngredient(curryId, "Basmatireis", "g", 75.0, "separat kochen");
            insertSampleIngredient(curryId, "Kokosmilch", "ml", 70.0, null);
            insertSampleIngredient(curryId, "Kichererbsen", "g", 45.0, "vorgekocht");
            insertSampleIngredient(curryId, "Rote Paprika", "Stück", 0.1, null);
            insertSampleIngredient(curryId, "Brokkoli", "Stück", 0.08, "in Röschen teilen");
            insertSampleIngredient(curryId, "Rote Currypaste", "g", 12.0, null);

            long pancakeId = insertSampleRecipe(
                    "Pfannkuchen mit Apfelmus",
                    null,
                    12,
                    "Teig aus Mehl, Milch, Eiern und Zucker herstellen. Pfannkuchen ausbacken und warm mit Apfelmus servieren.");
            insertSampleIngredient(pancakeId, "Weizenmehl", "g", 65.0, null);
            insertSampleIngredient(pancakeId, "Milch", "ml", 120.0, null);
            insertSampleIngredient(pancakeId, "Eier", "Stück", 0.5, null);
            insertSampleIngredient(pancakeId, "Apfelmus", "g", 90.0, null);
            insertSampleIngredient(pancakeId, "Zucker", "g", 8.0, "optional Zimt hinzufügen");

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private long insertSampleRecipe(String name, Long categoryId, int baseServings, String instructions) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO recipes (name, category_id, base_servings, instructions, created_at, updated_at)"
                        + " VALUES (?, ?, ?, ?, strftime('%Y-%m-%dT%H:%M:%SZ', 'now'), strftime('%Y-%m-%dT%H:%M:%SZ', 'now'))",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            if (categoryId != null) {
                statement.setLong(2, categoryId);
            } else {
                statement.setNull(2, java.sql.Types.INTEGER);
            }
            statement.setInt(3, baseServings);
            statement.setString(4, instructions);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to obtain generated key for sample recipe");
    }

    private void insertSampleIngredient(long recipeId, String name, String unit, double amountPerServing, String notes)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO recipe_ingredients (recipe_id, name, unit, amount_per_serving, notes)"
                        + " VALUES (?, ?, ?, ?, ?)")) {
            statement.setLong(1, recipeId);
            statement.setString(2, name);
            statement.setString(3, unit);
            statement.setDouble(4, amountPerServing);
            if (notes != null) {
                statement.setString(5, notes);
            } else {
                statement.setNull(5, java.sql.Types.VARCHAR);
            }
            statement.executeUpdate();
        }
    }

    private void initializeSchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS categories (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL UNIQUE CHECK (trim(name) <> ''), " +
                    "description TEXT" +
                    ")");

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

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS recipes (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL CHECK (trim(name) <> ''), " +
                    "category_id INTEGER, " +
                    "base_servings INTEGER NOT NULL DEFAULT 1 CHECK (base_servings > 0), " +
                    "instructions TEXT NOT NULL CHECK (trim(instructions) <> ''), " +
                    "created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')) CHECK (created_at LIKE '%Z'), " +
                    "updated_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')) CHECK (updated_at LIKE '%Z'), " +
                    "FOREIGN KEY (category_id) REFERENCES categories(id) ON UPDATE CASCADE ON DELETE SET NULL" +
                    ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS recipe_ingredients (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "recipe_id INTEGER NOT NULL, " +
                    "name TEXT NOT NULL CHECK (trim(name) <> ''), " +
                    "unit TEXT NOT NULL CHECK (trim(unit) <> ''), " +
                    "amount_per_serving REAL NOT NULL CHECK (amount_per_serving > 0), " +
                    "notes TEXT, " +
                    "FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON UPDATE CASCADE ON DELETE CASCADE" +
                    ")");

            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_recipe_ingredients_recipe ON recipe_ingredients(recipe_id)");

            statement.executeUpdate("CREATE TRIGGER IF NOT EXISTS recipes_updated_at " +
                    "AFTER UPDATE ON recipes " +
                    "FOR EACH ROW " +
                    "WHEN NEW.updated_at = OLD.updated_at " +
                    "BEGIN " +
                    "UPDATE recipes SET updated_at = strftime('%Y-%m-%dT%H:%M:%SZ', 'now') " +
                    "WHERE id = NEW.id AND updated_at = OLD.updated_at; " +
                    "END");
        }
    }

    public Connection getConnection() {
        return connection;
    }

    /**
     * Returns the path to the active SQLite database file.
     */
    public Path getDatabaseFile() {
        return databaseFile;
    }

    /**
     * Creates a physical backup of the current database file using SQLite's VACUUM INTO command.
     *
     * @param targetFile the desired backup file location (either absolute or relative).
     * @return the absolute path to the written backup file.
     */
    public Path backupTo(Path targetFile) {
        Path absoluteTarget = targetFile.toAbsolutePath();
        try {
            ensureParentDirectoryExists(absoluteTarget);
            try (Statement statement = connection.createStatement()) {
                String escapedPath = absoluteTarget.toString().replace("'", "''");
                statement.execute("VACUUM INTO '" + escapedPath + "'");
            }
            return absoluteTarget;
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Failed to create database backup", e);
        }
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
