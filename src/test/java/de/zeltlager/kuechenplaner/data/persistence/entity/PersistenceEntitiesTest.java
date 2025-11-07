package de.zeltlager.kuechenplaner.data.persistence.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceEntitiesTest {

    @Test
    void inventoryItemEntityStoresValues() {
        UserEntity user = new UserEntity();
        user.setUsername("cook");

        InventoryItemEntity entity = new InventoryItemEntity();
        entity.setUser(user);
        entity.setIngredient("Kartoffel");
        entity.setQuantity(10);
        entity.setUnit("kg");

        assertThat(entity.getUser()).isEqualTo(user);
        assertThat(entity.getIngredient()).isEqualTo("Kartoffel");
        assertThat(entity.getQuantity()).isEqualTo(10);
        assertThat(entity.getUnit()).isEqualTo("kg");
    }

    @Test
    void menuPlanEntryEntityStoresValues() {
        UserEntity user = new UserEntity();
        user.setUsername("cook");

        MenuPlanEntryEntity entity = new MenuPlanEntryEntity();
        entity.setUser(user);
        entity.setDate(LocalDate.of(2024, 7, 1));
        entity.setMealName("Mittag");
        entity.setServings(50);

        assertThat(entity.getUser()).isEqualTo(user);
        assertThat(entity.getDate()).isEqualTo(LocalDate.of(2024, 7, 1));
        assertThat(entity.getMealName()).isEqualTo("Mittag");
        assertThat(entity.getServings()).isEqualTo(50);
    }

    @Test
    void recipeEntityMaintainsIngredientRelation() {
        UserEntity user = new UserEntity();
        user.setUsername("chef");

        RecipeEntity recipe = new RecipeEntity();
        recipe.setUser(user);
        recipe.setName("Suppe");
        recipe.setCategoryId(5L);
        recipe.setBaseServings(4);
        recipe.setInstructions("Kochen");

        RecipeIngredientEntity ingredient = new RecipeIngredientEntity();
        ingredient.setRecipe(recipe);
        ingredient.setName("Salz");
        ingredient.setUnit("g");
        ingredient.setAmountPerServing(1.5);
        ingredient.setNotes("fein");

        recipe.getIngredients().add(ingredient);

        assertThat(recipe.getUser()).isEqualTo(user);
        assertThat(recipe.getIngredients()).containsExactly(ingredient);
        assertThat(ingredient.getRecipe()).isEqualTo(recipe);
        assertThat(ingredient.getName()).isEqualTo("Salz");
        assertThat(ingredient.getUnit()).isEqualTo("g");
        assertThat(ingredient.getAmountPerServing()).isEqualTo(1.5);
        assertThat(ingredient.getNotes()).isEqualTo("fein");
    }

    @Test
    void auditableEntityTracksMetadata() {
        Instant created = Instant.parse("2024-07-01T10:00:00Z");
        Instant updated = Instant.parse("2024-07-01T12:00:00Z");

        class TestAuditable extends AuditableEntity {
        }

        TestAuditable entity = new TestAuditable();
        entity.setCreatedAt(created);
        entity.setUpdatedAt(updated);

        try {
            var createdByField = AuditableEntity.class.getDeclaredField("createdBy");
            createdByField.setAccessible(true);
            createdByField.set(entity, "creator");

            var updatedByField = AuditableEntity.class.getDeclaredField("updatedBy");
            updatedByField.setAccessible(true);
            updatedByField.set(entity, "updater");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to set audit fields via reflection", e);
        }

        assertThat(entity.getCreatedAt()).isEqualTo(created);
        assertThat(entity.getUpdatedAt()).isEqualTo(updated);
        assertThat(entity.getCreatedBy()).isEqualTo("creator");
        assertThat(entity.getUpdatedBy()).isEqualTo("updater");
    }
}
