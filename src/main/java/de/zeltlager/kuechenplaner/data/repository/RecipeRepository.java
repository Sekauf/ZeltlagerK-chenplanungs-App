package de.zeltlager.kuechenplaner.data.repository;

import de.zeltlager.kuechenplaner.data.model.Ingredient;
import de.zeltlager.kuechenplaner.data.model.Recipe;
import de.zeltlager.kuechenplaner.data.model.RecipeWithIngredients;

import java.util.List;
import java.util.Optional;

/**
 * Repository abstraction for persisting recipes including their ingredients.
 */
public interface RecipeRepository {

    RecipeWithIngredients create(Recipe recipe, List<Ingredient> ingredients);

    Optional<RecipeWithIngredients> findById(long id);

    List<RecipeWithIngredients> findAll();

    RecipeWithIngredients update(Recipe recipe, List<Ingredient> ingredients);

    void delete(long id);
}
