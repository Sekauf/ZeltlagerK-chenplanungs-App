package de.zeltlager.kuechenplaner.logic;

import de.zeltlager.kuechenplaner.data.model.Ingredient;
import de.zeltlager.kuechenplaner.data.model.RecipeWithIngredients;
import de.zeltlager.kuechenplaner.data.model.ShoppingListItem;

import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Optional;

/**
 * Service API for managing recipes and their ingredients.
 */
public interface RecipeService {

    List<RecipeWithIngredients> getAllRecipes();

    Optional<RecipeWithIngredients> getRecipe(long id);

    RecipeWithIngredients createRecipe(String name,
                                       Long categoryId,
                                       int baseServings,
                                       String instructions,
                                       List<Ingredient> ingredients);

    RecipeWithIngredients updateRecipe(long id,
                                       String name,
                                       Long categoryId,
                                       int baseServings,
                                       String instructions,
                                       List<Ingredient> ingredients);

    void deleteRecipe(long id);

    List<ShoppingListItem> generateShoppingList(List<RecipeSelection> selections);

    /**
     * Exports all recipes in the repository into the provided writer.
     *
     * @param writer the writer to write the exported data to
     * @param format the desired export format
     */
    void exportRecipes(Writer writer, ExportFormat format);

    /**
     * Imports recipes from an external source and persists them using the current repository.
     *
     * @param reader the reader providing the external recipe data
     * @param format the format of the external data source
     * @return the list of imported recipes as stored in the repository
     */
    List<RecipeWithIngredients> importRecipes(Reader reader, ImportFormat format);

    enum ExportFormat {
        CSV,
        PLAIN_TEXT
    }

    enum ImportFormat {
        CSV,
        MEAL_MASTER
    }

    record RecipeSelection(long recipeId, int servings) {
        public RecipeSelection {
            if (recipeId <= 0) {
                throw new IllegalArgumentException("recipeId must be greater than zero");
            }
            if (servings <= 0) {
                throw new IllegalArgumentException("servings must be greater than zero");
            }
        }
    }
}
