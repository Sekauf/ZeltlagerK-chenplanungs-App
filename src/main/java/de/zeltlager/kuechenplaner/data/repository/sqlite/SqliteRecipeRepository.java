package de.zeltlager.kuechenplaner.data.repository.sqlite;

import de.zeltlager.kuechenplaner.data.model.Ingredient;
import de.zeltlager.kuechenplaner.data.model.Recipe;
import de.zeltlager.kuechenplaner.data.model.RecipeWithIngredients;
import de.zeltlager.kuechenplaner.data.repository.RecipeRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * SQLite implementation for {@link RecipeRepository}.
 */
public final class SqliteRecipeRepository implements RecipeRepository {

    private final Connection connection;

    public SqliteRecipeRepository(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "connection");
    }

    @Override
    public RecipeWithIngredients create(Recipe recipe, List<Ingredient> ingredients) {
        Objects.requireNonNull(recipe, "recipe");
        Objects.requireNonNull(ingredients, "ingredients");

        try {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                long recipeId = insertRecipe(recipe);
                Recipe persistedRecipe = new Recipe(
                        recipeId,
                        recipe.getName(),
                        recipe.getCategoryId().orElse(null),
                        recipe.getBaseServings(),
                        recipe.getInstructions(),
                        recipe.getCreatedAt().orElse(null),
                        recipe.getUpdatedAt().orElse(null));

                List<Ingredient> persistedIngredients = insertIngredients(recipeId, ingredients);
                connection.commit();
                return new RecipeWithIngredients(persistedRecipe, persistedIngredients);
            } catch (SQLException e) {
                connection.rollback();
                throw new IllegalStateException("Failed to create recipe", e);
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create recipe", e);
        }
    }

    @Override
    public Optional<RecipeWithIngredients> findById(long id) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, name, category_id, base_servings, instructions, created_at, updated_at FROM recipes WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                Recipe recipe = mapRecipe(resultSet);
                List<Ingredient> ingredients = findIngredientsForRecipe(id);
                return Optional.of(new RecipeWithIngredients(recipe, ingredients));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load recipe", e);
        }
    }

    @Override
    public List<RecipeWithIngredients> findAll() {
        Map<Long, Recipe> recipes = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, name, category_id, base_servings, instructions, created_at, updated_at FROM recipes ORDER BY name")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Recipe recipe = mapRecipe(resultSet);
                    recipes.put(recipe.getId().orElseThrow(), recipe);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load recipes", e);
        }

        if (recipes.isEmpty()) {
            return List.of();
        }

        Map<Long, List<Ingredient>> ingredientsByRecipe = loadIngredientsForRecipes(recipes.keySet());

        List<RecipeWithIngredients> result = new ArrayList<>();
        for (Map.Entry<Long, Recipe> entry : recipes.entrySet()) {
            List<Ingredient> ingredients = ingredientsByRecipe.getOrDefault(entry.getKey(), List.of());
            result.add(new RecipeWithIngredients(entry.getValue(), ingredients));
        }
        return List.copyOf(result);
    }

    @Override
    public RecipeWithIngredients update(Recipe recipe, List<Ingredient> ingredients) {
        Objects.requireNonNull(recipe, "recipe");
        Objects.requireNonNull(ingredients, "ingredients");
        long recipeId = recipe.getId().orElseThrow(() -> new IllegalArgumentException("Recipe ID must be present for update"));

        try {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                updateRecipeRow(recipe);
                deleteIngredientsForRecipe(recipeId);
                List<Ingredient> persistedIngredients = insertIngredients(recipeId, ingredients);
                connection.commit();
                return new RecipeWithIngredients(recipe, persistedIngredients);
            } catch (SQLException e) {
                connection.rollback();
                throw new IllegalStateException("Failed to update recipe", e);
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update recipe", e);
        }
    }

    @Override
    public void delete(long id) {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM recipes WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete recipe", e);
        }
    }

    private long insertRecipe(Recipe recipe) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO recipes (name, category_id, base_servings, instructions, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, recipe.getName());
            if (recipe.getCategoryId().isPresent()) {
                statement.setLong(2, recipe.getCategoryId().get());
            } else {
                statement.setNull(2, Types.INTEGER);
            }
            statement.setInt(3, recipe.getBaseServings());
            statement.setString(4, recipe.getInstructions());
            statement.setString(5, recipe.getCreatedAt().map(Instant::toString).orElse(null));
            statement.setString(6, recipe.getUpdatedAt().map(Instant::toString).orElse(null));
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
                throw new SQLException("Failed to obtain generated recipe ID");
            }
        }
    }

    private void updateRecipeRow(Recipe recipe) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE recipes SET name = ?, category_id = ?, base_servings = ?, instructions = ?, created_at = ?, updated_at = ? WHERE id = ?")) {
            statement.setString(1, recipe.getName());
            if (recipe.getCategoryId().isPresent()) {
                statement.setLong(2, recipe.getCategoryId().get());
            } else {
                statement.setNull(2, Types.INTEGER);
            }
            statement.setInt(3, recipe.getBaseServings());
            statement.setString(4, recipe.getInstructions());
            statement.setString(5, recipe.getCreatedAt().map(Instant::toString).orElse(null));
            statement.setString(6, recipe.getUpdatedAt().map(Instant::toString).orElse(null));
            statement.setLong(7, recipe.getId().orElseThrow());
            statement.executeUpdate();
        }
    }

    private List<Ingredient> insertIngredients(long recipeId, List<Ingredient> ingredients) throws SQLException {
        if (ingredients.isEmpty()) {
            return List.of();
        }

        List<Ingredient> persisted = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO recipe_ingredients (recipe_id, name, unit, amount_per_serving, notes) VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            for (Ingredient ingredient : ingredients) {
                statement.setLong(1, recipeId);
                statement.setString(2, ingredient.getName());
                statement.setString(3, ingredient.getUnit());
                statement.setDouble(4, ingredient.getAmountPerServing());
                if (ingredient.getNotes().isPresent()) {
                    statement.setString(5, ingredient.getNotes().get());
                } else {
                    statement.setNull(5, Types.VARCHAR);
                }
                statement.executeUpdate();
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        long ingredientId = generatedKeys.getLong(1);
                        persisted.add(new Ingredient(
                                ingredientId,
                                recipeId,
                                ingredient.getName(),
                                ingredient.getUnit(),
                                ingredient.getAmountPerServing(),
                                ingredient.getNotes().orElse(null)));
                    } else {
                        throw new SQLException("Failed to obtain generated ingredient ID");
                    }
                }
            }
        }
        return List.copyOf(persisted);
    }

    private void deleteIngredientsForRecipe(long recipeId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM recipe_ingredients WHERE recipe_id = ?")) {
            statement.setLong(1, recipeId);
            statement.executeUpdate();
        }
    }

    private Map<Long, List<Ingredient>> loadIngredientsForRecipes(Iterable<Long> recipeIds) {
        String sql = "SELECT id, recipe_id, name, unit, amount_per_serving, notes FROM recipe_ingredients WHERE recipe_id IN (" +
                inPlaceholders(recipeIds) + ") ORDER BY recipe_id, id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Long recipeId : recipeIds) {
                statement.setLong(index++, recipeId);
            }

            Map<Long, List<Ingredient>> result = new LinkedHashMap<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Ingredient ingredient = mapIngredient(resultSet);
                    long recipeId = ingredient.getRecipeId().orElseThrow();
                    result.computeIfAbsent(recipeId, id -> new ArrayList<>()).add(ingredient);
                }
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load ingredients", e);
        }
    }

    private String inPlaceholders(Iterable<Long> ids) {
        StringBuilder builder = new StringBuilder();
        for (Long id : ids) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append('?');
        }
        return builder.toString();
    }

    private List<Ingredient> findIngredientsForRecipe(long recipeId) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, recipe_id, name, unit, amount_per_serving, notes FROM recipe_ingredients WHERE recipe_id = ? ORDER BY id")) {
            statement.setLong(1, recipeId);
            List<Ingredient> result = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(mapIngredient(resultSet));
                }
            }
            return List.copyOf(result);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load recipe ingredients", e);
        }
    }

    private Recipe mapRecipe(ResultSet resultSet) throws SQLException {
        long id = resultSet.getLong("id");
        String name = resultSet.getString("name");
        long categoryId = resultSet.getLong("category_id");
        Long category = resultSet.wasNull() ? null : categoryId;
        int baseServings = resultSet.getInt("base_servings");
        String instructions = resultSet.getString("instructions");
        String createdAtRaw = resultSet.getString("created_at");
        String updatedAtRaw = resultSet.getString("updated_at");
        Instant createdAt = createdAtRaw != null ? Instant.parse(createdAtRaw) : null;
        Instant updatedAt = updatedAtRaw != null ? Instant.parse(updatedAtRaw) : null;
        return new Recipe(id, name, category, baseServings, instructions, createdAt, updatedAt);
    }

    private Ingredient mapIngredient(ResultSet resultSet) throws SQLException {
        long id = resultSet.getLong("id");
        long recipeId = resultSet.getLong("recipe_id");
        String name = resultSet.getString("name");
        String unit = resultSet.getString("unit");
        double amountPerServing = resultSet.getDouble("amount_per_serving");
        String notes = resultSet.getString("notes");
        return new Ingredient(id, recipeId, name, unit, amountPerServing, notes);
    }
}
