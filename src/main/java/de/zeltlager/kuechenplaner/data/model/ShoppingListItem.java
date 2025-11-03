package de.zeltlager.kuechenplaner.data.model;

import java.util.List;
import java.util.Objects;

/**
 * Represents an aggregated ingredient entry for a shopping list.
 */
public final class ShoppingListItem {

    private final String name;
    private final String unit;
    private final double totalAmount;
    private final List<String> notes;

    public ShoppingListItem(String name, String unit, double totalAmount, List<String> notes) {
        this.name = Objects.requireNonNull(name, "name");
        this.unit = Objects.requireNonNull(unit, "unit");
        this.totalAmount = totalAmount;
        this.notes = List.copyOf(Objects.requireNonNull(notes, "notes"));
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public List<String> getNotes() {
        return notes;
    }
}
