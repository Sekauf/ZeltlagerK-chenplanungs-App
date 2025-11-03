package de.zeltlager.kuechenplaner.data.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents an aggregated ingredient entry for a shopping list.
 */
public final class ShoppingListItem {

    private final String name;
    private final String unit;
    private final double totalAmount;
    private final List<String> notes;
    private final String category;

    public ShoppingListItem(String name, String unit, double totalAmount, List<String> notes) {
        this(name, unit, totalAmount, notes, null);
    }

    public ShoppingListItem(String name,
                            String unit,
                            double totalAmount,
                            List<String> notes,
                            String category) {
        this.name = Objects.requireNonNull(name, "name");
        this.unit = Objects.requireNonNull(unit, "unit");
        this.totalAmount = totalAmount;
        this.notes = List.copyOf(Objects.requireNonNull(notes, "notes"));
        this.category = category;
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

    public Optional<String> getCategory() {
        return Optional.ofNullable(category);
    }
}
