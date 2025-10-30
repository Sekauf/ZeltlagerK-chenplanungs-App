package de.zeltlager.kuechenplaner.data.model;

import java.util.Objects;

/**
 * Represents a meal that can be served at the camp.
 */
public final class Meal {
    private final String name;
    private final int servings;

    public Meal(String name, int servings) {
        this.name = Objects.requireNonNull(name, "name");
        this.servings = servings;
    }

    public String getName() {
        return name;
    }

    public int getServings() {
        return servings;
    }
}
