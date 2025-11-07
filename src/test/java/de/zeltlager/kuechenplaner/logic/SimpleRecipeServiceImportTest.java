package de.zeltlager.kuechenplaner.logic;

import de.zeltlager.kuechenplaner.data.model.Ingredient;
import de.zeltlager.kuechenplaner.data.model.Recipe;
import de.zeltlager.kuechenplaner.data.model.RecipeWithIngredients;
import de.zeltlager.kuechenplaner.data.repository.RecipeRepository;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleRecipeServiceImportTest {

    @Test
    void importsRecipesFromCsv() {
        RecordingRecipeRepository repository = new RecordingRecipeRepository();
        SimpleRecipeService service = new SimpleRecipeService(repository);

        String csv = String.join("\n",
                "name;base_servings;instructions;ingredient_name;ingredient_unit;ingredient_amount_per_serving;ingredient_notes",
                "Kartoffelsalat;10;Alles vermengen;Kartoffeln;g;50;festkochend",
                "Kartoffelsalat;10;Alles vermengen;Essig;ml;5;Apfelessig");

        List<RecipeWithIngredients> imported = service.importRecipes(
                new StringReader(csv),
                RecipeService.ImportFormat.CSV);

        assertEquals(1, imported.size());
        RecipeWithIngredients recipe = imported.get(0);
        assertEquals("Kartoffelsalat", recipe.getRecipe().getName());
        assertEquals(10, recipe.getRecipe().getBaseServings());
        assertEquals("Alles vermengen", recipe.getRecipe().getInstructions());
        assertEquals(2, recipe.getIngredients().size());
        assertEquals("Kartoffeln", recipe.getIngredients().get(0).getName());
        assertEquals("Essig", recipe.getIngredients().get(1).getName());
        assertEquals(1, repository.createdRecipes.size());
    }

    @Test
    void importsRecipesFromMealMaster() {
        RecordingRecipeRepository repository = new RecordingRecipeRepository();
        SimpleRecipeService service = new SimpleRecipeService(repository);

        String mealMaster = String.join("\n",
                "MMMMM----- Recipe via Meal-Master (tm) v8.05",
                "",
                "      Title: Pfannkuchen",
                "      Yield: 8 servings",
                "",
                formatMealMasterIngredient("2", "c", "Flour"),
                formatMealMasterIngredient("1", "l", "Milk"),
                "",
                "Mix and bake.");

        List<RecipeWithIngredients> imported = service.importRecipes(
                new StringReader(mealMaster),
                RecipeService.ImportFormat.MEAL_MASTER);

        assertEquals(1, imported.size());
        RecipeWithIngredients recipe = imported.get(0);
        assertEquals("Pfannkuchen", recipe.getRecipe().getName());
        assertEquals(8, recipe.getRecipe().getBaseServings());
        assertEquals(2, recipe.getIngredients().size());
        assertEquals(0.25, recipe.getIngredients().get(0).getAmountPerServing());
        assertEquals(1, repository.createdRecipes.size());
    }

    private static String formatMealMasterIngredient(String amount, String unit, String name) {
        return String.format("%1$7s %2$-7s%3$s", amount, unit, name);
    }

    private static final class RecordingRecipeRepository implements RecipeRepository {
        private final List<RecipeWithIngredients> stored = new ArrayList<>();
        private long nextId = 1;
        private long nextIngredientId = 1;
        private final List<Recipe> createdRecipes = new ArrayList<>();

        @Override
        public RecipeWithIngredients create(Recipe recipe, List<Ingredient> ingredients) {
            Recipe persisted = new Recipe(nextId++,
                    recipe.getName(),
                    recipe.getCategoryId().orElse(null),
                    recipe.getBaseServings(),
                    recipe.getInstructions(),
                    recipe.getCreatedAt().orElse(Instant.now()),
                    recipe.getUpdatedAt().orElse(Instant.now()));
            List<Ingredient> persistedIngredients = new ArrayList<>();
            for (Ingredient ingredient : ingredients) {
                persistedIngredients.add(new Ingredient(nextIngredientId++,
                        persisted.getId().orElseThrow(),
                        ingredient.getName(),
                        ingredient.getUnit(),
                        ingredient.getAmountPerServing(),
                        ingredient.getNotes().orElse(null)));
            }
            RecipeWithIngredients result = new RecipeWithIngredients(persisted, persistedIngredients);
            stored.add(result);
            createdRecipes.add(persisted);
            return result;
        }

        @Override
        public Optional<RecipeWithIngredients> findById(long id) {
            return stored.stream().filter(recipe -> recipe.getRecipe().getId().orElseThrow() == id).findFirst();
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
