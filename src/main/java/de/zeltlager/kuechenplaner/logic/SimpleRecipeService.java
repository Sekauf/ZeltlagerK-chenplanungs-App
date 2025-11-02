package de.zeltlager.kuechenplaner.logic;

import de.zeltlager.kuechenplaner.data.model.Ingredient;
import de.zeltlager.kuechenplaner.data.model.Recipe;
import de.zeltlager.kuechenplaner.data.model.RecipeWithIngredients;
import de.zeltlager.kuechenplaner.data.repository.RecipeRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of {@link RecipeService} that delegates to a {@link RecipeRepository}.
 */
public final class SimpleRecipeService implements RecipeService {

    private final RecipeRepository recipeRepository;

    public SimpleRecipeService(RecipeRepository recipeRepository) {
        this.recipeRepository = Objects.requireNonNull(recipeRepository, "recipeRepository");
    }

    @Override
    public List<RecipeWithIngredients> getAllRecipes() {
        return recipeRepository.findAll();
    }

    @Override
    public Optional<RecipeWithIngredients> getRecipe(long id) {
        return recipeRepository.findById(id);
    }

    @Override
    public RecipeWithIngredients createRecipe(String name,
                                              Long categoryId,
                                              int baseServings,
                                              String instructions,
                                              List<Ingredient> ingredients) {
        validateBaseServings(baseServings);
        List<Ingredient> normalizedIngredients = normalizeNewIngredients(ingredients);
        Instant now = Instant.now();
        Recipe recipe = new Recipe(null, name, categoryId, baseServings, instructions, now, now);
        return recipeRepository.create(recipe, normalizedIngredients);
    }

    @Override
    public RecipeWithIngredients updateRecipe(long id,
                                              String name,
                                              Long categoryId,
                                              int baseServings,
                                              String instructions,
                                              List<Ingredient> ingredients) {
        validateBaseServings(baseServings);
        RecipeWithIngredients existing = recipeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recipe with id " + id + " does not exist"));

        List<Ingredient> normalizedIngredients = normalizeUpdatedIngredients(id, ingredients);
        Recipe updatedRecipe = new Recipe(
                id,
                name,
                categoryId,
                baseServings,
                instructions,
                existing.getRecipe().getCreatedAt().orElse(null),
                Instant.now());
        return recipeRepository.update(updatedRecipe, normalizedIngredients);
    }

    @Override
    public void deleteRecipe(long id) {
        recipeRepository.delete(id);
    }

    private void validateBaseServings(int baseServings) {
        if (baseServings <= 0) {
            throw new IllegalArgumentException("Base servings must be greater than zero");
        }
    }

    private List<Ingredient> normalizeNewIngredients(List<Ingredient> ingredients) {
        Objects.requireNonNull(ingredients, "ingredients");
        List<Ingredient> result = new ArrayList<>(ingredients.size());
        for (Ingredient ingredient : ingredients) {
            Objects.requireNonNull(ingredient, "ingredient");
            if (ingredient.getRecipeId().isPresent()) {
                throw new IllegalArgumentException("New ingredient must not already be associated with a recipe");
            }
            result.add(new Ingredient(
                    ingredient.getId().orElse(null),
                    null,
                    ingredient.getName(),
                    ingredient.getUnit(),
                    ingredient.getAmountPerServing(),
                    ingredient.getNotes().orElse(null)));
        }
        return List.copyOf(result);
    }

    private List<Ingredient> normalizeUpdatedIngredients(long recipeId, List<Ingredient> ingredients) {
        Objects.requireNonNull(ingredients, "ingredients");
        List<Ingredient> result = new ArrayList<>(ingredients.size());
        for (Ingredient ingredient : ingredients) {
            Objects.requireNonNull(ingredient, "ingredient");
            if (ingredient.getRecipeId().isPresent() && ingredient.getRecipeId().get() != recipeId) {
                throw new IllegalArgumentException("Ingredient belongs to another recipe");
            }
            result.add(new Ingredient(
                    ingredient.getId().orElse(null),
                    recipeId,
                    ingredient.getName(),
                    ingredient.getUnit(),
                    ingredient.getAmountPerServing(),
                    ingredient.getNotes().orElse(null)));
        }
        return List.copyOf(result);
    }
}
