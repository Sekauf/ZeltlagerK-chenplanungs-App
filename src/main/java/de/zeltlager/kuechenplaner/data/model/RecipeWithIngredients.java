package de.zeltlager.kuechenplaner.data.model;

import java.util.List;
import java.util.Objects;

/**
 * Aggregates a recipe with its ingredient list.
 */
public final class RecipeWithIngredients {

    private final Recipe recipe;
    private final List<Ingredient> ingredients;

    public RecipeWithIngredients(Recipe recipe, List<Ingredient> ingredients) {
        this.recipe = Objects.requireNonNull(recipe, "recipe");
        this.ingredients = List.copyOf(Objects.requireNonNull(ingredients, "ingredients"));
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }
}
