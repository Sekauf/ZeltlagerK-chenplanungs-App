package de.zeltlager.kuechenplaner.data.model;

import java.util.Objects;

/**
 * Represents the quantity of a specific ingredient in stock.
 */
public final class InventoryItem {
    private final String ingredient;
    private final int quantity;
    private final String unit;

    public InventoryItem(String ingredient, int quantity, String unit) {
        this.ingredient = Objects.requireNonNull(ingredient, "ingredient");
        this.quantity = quantity;
        this.unit = Objects.requireNonNull(unit, "unit");
    }

    public String getIngredient() {
        return ingredient;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }
}
