package de.zeltlager.kuechenplaner.logic;

import de.zeltlager.kuechenplaner.data.model.Ingredient;
import de.zeltlager.kuechenplaner.data.model.RecipeWithIngredients;

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
}
