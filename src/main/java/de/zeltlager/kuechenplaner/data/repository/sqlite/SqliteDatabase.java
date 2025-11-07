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
                        + " VALUES (?, ?, ?, ?, datetime('now'), datetime('now'))",
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
                    "name TEXT NOT NULL, " +
                    "category_id INTEGER, " +
                    "base_servings INTEGER NOT NULL, " +
                    "instructions TEXT NOT NULL, " +
                    "created_at TEXT, " +
                    "updated_at TEXT" +
                    ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS recipe_ingredients (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "recipe_id INTEGER NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "unit TEXT NOT NULL, " +
                    "amount_per_serving REAL NOT NULL, " +
                    "notes TEXT, " +
                    "FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE" +
                    ")");

            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_recipe_ingredients_recipe_id ON recipe_ingredients(recipe_id)");
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
