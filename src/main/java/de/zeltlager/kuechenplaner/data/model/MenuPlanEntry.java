package de.zeltlager.kuechenplaner.data.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Describes which meal should be served on a specific date.
 */
public final class MenuPlanEntry {
    private final LocalDate date;
    private final Meal meal;

    public MenuPlanEntry(LocalDate date, Meal meal) {
        this.date = Objects.requireNonNull(date, "date");
        this.meal = Objects.requireNonNull(meal, "meal");
    }

    public LocalDate getDate() {
        return date;
    }

    public Meal getMeal() {
        return meal;
    }
}
