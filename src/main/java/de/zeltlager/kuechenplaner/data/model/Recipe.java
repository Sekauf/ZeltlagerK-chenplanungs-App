package de.zeltlager.kuechenplaner.data.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a recipe that can be prepared for the camp kitchen.
 */
public final class Recipe {
    private final Long id;
    private final String name;
    private final Long categoryId;
    private final int baseServings;
    private final String instructions;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Recipe(Long id,
                  String name,
                  Long categoryId,
                  int baseServings,
                  String instructions,
                  Instant createdAt,
                  Instant updatedAt) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
        this.categoryId = categoryId;
        this.baseServings = baseServings;
        this.instructions = Objects.requireNonNull(instructions, "instructions");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Optional<Long> getId() {
        return Optional.ofNullable(id);
    }

    public String getName() {
        return name;
    }

    public Optional<Long> getCategoryId() {
        return Optional.ofNullable(categoryId);
    }

    public int getBaseServings() {
        return baseServings;
    }

    public String getInstructions() {
        return instructions;
    }

    public Optional<Instant> getCreatedAt() {
        return Optional.ofNullable(createdAt);
    }

    public Optional<Instant> getUpdatedAt() {
        return Optional.ofNullable(updatedAt);
    }
}
