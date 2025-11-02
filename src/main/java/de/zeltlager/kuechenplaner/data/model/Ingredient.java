package de.zeltlager.kuechenplaner.data.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a single ingredient that belongs to a recipe.
 */
public final class Ingredient {
    private final Long id;
    private final Long recipeId;
    private final String name;
    private final String unit;
    private final double amountPerServing;
    private final String notes;

    public Ingredient(Long id,
                      Long recipeId,
                      String name,
                      String unit,
                      double amountPerServing,
                      String notes) {
        this.id = id;
        this.recipeId = recipeId;
        this.name = Objects.requireNonNull(name, "name");
        this.unit = Objects.requireNonNull(unit, "unit");
        this.amountPerServing = amountPerServing;
        this.notes = notes;
    }

    public Optional<Long> getId() {
        return Optional.ofNullable(id);
    }

    public Optional<Long> getRecipeId() {
        return Optional.ofNullable(recipeId);
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public double getAmountPerServing() {
        return amountPerServing;
    }

    public Optional<String> getNotes() {
        return Optional.ofNullable(notes);
    }
}
