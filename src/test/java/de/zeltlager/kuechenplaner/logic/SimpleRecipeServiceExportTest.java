package de.zeltlager.kuechenplaner.logic;

import de.zeltlager.kuechenplaner.data.model.Ingredient;
import de.zeltlager.kuechenplaner.data.model.Recipe;
import de.zeltlager.kuechenplaner.data.model.RecipeWithIngredients;
import de.zeltlager.kuechenplaner.data.repository.RecipeRepository;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleRecipeServiceExportTest {

    @Test
    void exportsRecipesAsCsv() {
        InMemoryRecipeRepository repository = new InMemoryRecipeRepository();
        SimpleRecipeService service = new SimpleRecipeService(repository);

        service.createRecipe(
                "Kartoffelsalat",
                2L,
                10,
                "Alles vermengen",
                List.of(
                        new Ingredient(null, null, "Kartoffeln", "g", 5.0, null),
                        new Ingredient(null, null, "Salz", "g", 0.1, "Meeressalz")));

        StringWriter writer = new StringWriter();
        service.exportRecipes(writer, RecipeService.ExportFormat.CSV);
        String[] lines = writer.toString().split("\r?\n");

        assertTrue(lines.length >= 3);
        assertEquals(
                "recipe_id;name;category_id;base_servings;instructions;ingredient_name;ingredient_unit;ingredient_amount_per_serving;ingredient_amount_total;ingredient_notes",
                lines[0]);
        assertEquals(
                "1;Kartoffelsalat;2;10;Alles vermengen;Kartoffeln;g;5;50;",
                lines[1]);
        assertEquals(
                "1;Kartoffelsalat;2;10;Alles vermengen;Salz;g;0.1;1;Meeressalz",
                lines[2]);
    }

    @Test
    void exportsRecipesAsPlainText() {
        InMemoryRecipeRepository repository = new InMemoryRecipeRepository();
        SimpleRecipeService service = new SimpleRecipeService(repository);

        service.createRecipe(
                "Pfannkuchen",
                null,
                8,
                "Mixen und backen",
                List.of(
                        new Ingredient(null, null, "Mehl", "g", 12.5, null)));

        StringWriter writer = new StringWriter();
        service.exportRecipes(writer, RecipeService.ExportFormat.PLAIN_TEXT);
        String output = writer.toString();

        assertTrue(output.contains("Pfannkuchen"));
        assertTrue(output.contains("Kategorie-ID: -"));
        assertTrue(output.contains("Portionen: 8"));
        assertTrue(output.contains("Zutaten:"));
        assertTrue(output.contains("- 100 g Mehl"));
        assertTrue(output.contains("Anleitung:"));
        assertTrue(output.contains("Mixen und backen"));
    }

    private static final class InMemoryRecipeRepository implements RecipeRepository {
        private final List<RecipeWithIngredients> stored = new ArrayList<>();
        private long nextRecipeId = 1;
        private long nextIngredientId = 1;

        @Override
        public RecipeWithIngredients create(Recipe recipe, List<Ingredient> ingredients) {
            Recipe persisted = new Recipe(
                    nextRecipeId++,
                    recipe.getName(),
                    recipe.getCategoryId().orElse(null),
                    recipe.getBaseServings(),
                    recipe.getInstructions(),
                    recipe.getCreatedAt().orElse(Instant.now()),
                    recipe.getUpdatedAt().orElse(Instant.now()));
            List<Ingredient> persistedIngredients = new ArrayList<>();
            for (Ingredient ingredient : ingredients) {
                persistedIngredients.add(new Ingredient(
                        nextIngredientId++,
                        persisted.getId().orElseThrow(),
                        ingredient.getName(),
                        ingredient.getUnit(),
                        ingredient.getAmountPerServing(),
                        ingredient.getNotes().orElse(null)));
            }
            RecipeWithIngredients result = new RecipeWithIngredients(persisted, persistedIngredients);
            stored.add(result);
            return result;
        }

        @Override
        public Optional<RecipeWithIngredients> findById(long id) {
            return stored.stream()
                    .filter(recipe -> recipe.getRecipe().getId().orElseThrow() == id)
                    .findFirst();
        }

        @Override
        public List<RecipeWithIngredients> findAll() {
            return List.copyOf(stored);
        }

        @Override
        public RecipeWithIngredients update(Recipe recipe, List<Ingredient> ingredients) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(long id) {
            throw new UnsupportedOperationException();
        }
    }
}
