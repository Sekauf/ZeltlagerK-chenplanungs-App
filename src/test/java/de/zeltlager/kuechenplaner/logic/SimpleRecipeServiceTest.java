package de.zeltlager.kuechenplaner.logic;

import de.zeltlager.kuechenplaner.data.model.Ingredient;
import de.zeltlager.kuechenplaner.data.model.Recipe;
import de.zeltlager.kuechenplaner.data.model.RecipeWithIngredients;
import de.zeltlager.kuechenplaner.data.model.ShoppingListItem;
import de.zeltlager.kuechenplaner.data.repository.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimpleRecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    private SimpleRecipeService service;

    @BeforeEach
    void setUp() {
        service = new SimpleRecipeService(recipeRepository);
    }

    @Test
    void createRecipeDelegatesToRepositoryWithNormalizedIngredients() {
        when(recipeRepository.create(any(), any())).thenAnswer(invocation -> {
            Recipe recipe = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            List<Ingredient> ingredients = (List<Ingredient>) invocation.getArgument(1);
            return new RecipeWithIngredients(recipe, ingredients);
        });

        Ingredient ingredient = new Ingredient(5L, null, "Tomate", "g", 50.0, " reif ");
        RecipeWithIngredients created = service.createRecipe(
                "Salat",
                3L,
                4,
                "Alles mischen",
                List.of(ingredient));

        assertThat(created.getRecipe().getName()).isEqualTo("Salat");
        ArgumentCaptor<Recipe> recipeCaptor = ArgumentCaptor.forClass(Recipe.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Ingredient>> ingredientCaptor = ArgumentCaptor.forClass(List.class);
        verify(recipeRepository).create(recipeCaptor.capture(), ingredientCaptor.capture());

        Recipe persistedRecipe = recipeCaptor.getValue();
        assertThat(persistedRecipe.getId()).isEmpty();
        assertThat(persistedRecipe.getCategoryId()).contains(3L);
        assertThat(persistedRecipe.getBaseServings()).isEqualTo(4);
        assertThat(persistedRecipe.getInstructions()).isEqualTo("Alles mischen");
        assertThat(persistedRecipe.getCreatedAt()).isPresent();
        assertThat(persistedRecipe.getUpdatedAt()).isPresent();

        List<Ingredient> normalizedIngredients = ingredientCaptor.getValue();
        assertThat(normalizedIngredients).hasSize(1);
        Ingredient normalized = normalizedIngredients.get(0);
        assertThat(normalized.getRecipeId()).isEmpty();
        assertThat(normalized.getName()).isEqualTo("Tomate");
        assertThat(normalized.getNotes()).contains(" reif ");
    }

    @Test
    void createRecipeRejectsInvalidBaseServings() {
        assertThrows(IllegalArgumentException.class, () -> service.createRecipe(
                "Test",
                null,
                0,
                "",
                List.of()));
    }

    @Test
    void updateRecipeNormalizesIngredientsAndPreservesCreationTimestamp() {
        Instant createdAt = Instant.parse("2023-05-01T12:30:00Z");
        Recipe existingRecipe = new Recipe(10L, "Eintopf", 5L, 6, "Kochen", createdAt, createdAt);
        Ingredient existingIngredient = new Ingredient(100L, 10L, "Kartoffel", "g", 100.0, null);
        RecipeWithIngredients existing = new RecipeWithIngredients(existingRecipe, List.of(existingIngredient));
        when(recipeRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(recipeRepository.update(any(), any())).thenAnswer(invocation -> {
            Recipe recipe = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            List<Ingredient> ingredients = (List<Ingredient>) invocation.getArgument(1);
            return new RecipeWithIngredients(recipe, ingredients);
        });

        Ingredient updatedIngredient = new Ingredient(100L, 10L, "Kartoffel", "kg", 0.2, "mehlig");
        RecipeWithIngredients updated = service.updateRecipe(
                10L,
                "Gemüse-Eintopf",
                7L,
                8,
                "Neu kochen",
                List.of(updatedIngredient));

        assertThat(updated.getRecipe().getName()).isEqualTo("Gemüse-Eintopf");
        assertThat(updated.getRecipe().getCategoryId()).contains(7L);
        assertThat(updated.getRecipe().getCreatedAt()).contains(createdAt);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Ingredient>> ingredientsCaptor = ArgumentCaptor.forClass(List.class);
        verify(recipeRepository).update(any(), ingredientsCaptor.capture());
        Ingredient normalized = ingredientsCaptor.getValue().get(0);
        assertThat(normalized.getRecipeId()).contains(10L);
        assertThat(normalized.getUnit()).isEqualTo("kg");
        assertThat(normalized.getNotes()).contains("mehlig");
    }

    @Test
    void updateRecipeThrowsIfRecipeDoesNotExist() {
        when(recipeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.updateRecipe(
                99L,
                "Nicht vorhanden",
                null,
                2,
                "",
                List.of()));
    }

    @Test
    void updateRecipeRejectsIngredientFromDifferentRecipe() {
        Ingredient invalid = new Ingredient(3L, 77L, "Salz", "g", 1.0, null);
        Recipe existingRecipe = new Recipe(5L, "Suppe", null, 2, "", Instant.now(), Instant.now());
        RecipeWithIngredients existing = new RecipeWithIngredients(existingRecipe, List.of());
        when(recipeRepository.findById(5L)).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () -> service.updateRecipe(
                5L,
                "Suppe",
                null,
                2,
                "",
                List.of(invalid)));
    }

    @Test
    void deleteRecipeDelegatesToRepository() {
        service.deleteRecipe(42L);
        verify(recipeRepository).delete(42L);
    }

    @Test
    void generateShoppingListAggregatesQuantitiesAndCategories() {
        Recipe recipeOne = new Recipe(1L, "Kartoffelsuppe", null, 4, "Kochen", Instant.now(), Instant.now());
        Ingredient potato = new Ingredient(11L, 1L, "Kartoffel", "kg", 0.25, "mehlig");
        Ingredient milk = new Ingredient(12L, 1L, "Milch", "l", 0.05, "3,5% Fett");
        RecipeWithIngredients recipeWithIngredientsOne = new RecipeWithIngredients(recipeOne, List.of(potato, milk));

        Recipe recipeTwo = new Recipe(2L, "Kartoffelsalat", null, 2, "", Instant.now(), Instant.now());
        Ingredient potatoTwo = new Ingredient(21L, 2L, "Kartoffel", "g", 30.0, "frisch");
        RecipeWithIngredients recipeWithIngredientsTwo = new RecipeWithIngredients(recipeTwo, List.of(potatoTwo));

        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipeWithIngredientsOne));
        when(recipeRepository.findById(2L)).thenReturn(Optional.of(recipeWithIngredientsTwo));

        List<RecipeService.RecipeSelection> selections = List.of(
                new RecipeService.RecipeSelection(1L, 8),
                new RecipeService.RecipeSelection(2L, 4));

        List<ShoppingListItem> items = service.generateShoppingList(selections);

        assertThat(items).hasSize(2);

        ShoppingListItem milkItem = items.get(0);
        assertThat(milkItem.getName()).isEqualTo("Milch");
        assertThat(milkItem.getUnit()).isEqualTo("ml");
        assertThat(milkItem.getTotalAmount()).isEqualTo(400.0);
        assertThat(milkItem.getCategory()).contains("Milchprodukte");
        assertThat(milkItem.getNotes()).containsExactly("3,5% Fett");

        ShoppingListItem potatoItem = items.get(1);
        assertThat(potatoItem.getName()).isEqualTo("Kartoffel");
        assertThat(potatoItem.getUnit()).isEqualTo("g");
        assertThat(potatoItem.getTotalAmount()).isEqualTo(2120.0);
        assertThat(potatoItem.getCategory()).contains("Obst & Gemüse");
        assertThat(potatoItem.getNotes()).containsExactly("mehlig", "frisch");
    }

    @Test
    void generateShoppingListThrowsIfRecipeMissing() {
        when(recipeRepository.findById(7L)).thenReturn(Optional.empty());
        List<RecipeService.RecipeSelection> selections = List.of(new RecipeService.RecipeSelection(7L, 2));
        assertThrows(IllegalArgumentException.class, () -> service.generateShoppingList(selections));
    }

    @Test
    void generateShoppingListReturnsEmptyListForNoSelections() {
        assertThat(service.generateShoppingList(List.of())).isEmpty();
    }

    @Test
    void exportRecipesWrapsIoExceptions() throws IOException {
        when(recipeRepository.findAll()).thenReturn(List.of());
        Writer writer = new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                throw new IOException("kaputt");
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };

        assertThrows(IllegalArgumentException.class, () -> service.exportRecipes(writer, RecipeService.ExportFormat.CSV));
    }

    @Test
    void importRecipesReturnsEmptyListWhenInputEmpty() {
        assertThat(service.importRecipes(new java.io.StringReader(""), RecipeService.ImportFormat.CSV)).isEmpty();
    }

    @Test
    void importRecipesFailsForCsvWithMissingIngredientName() {
        String csv = String.join("\n",
                "name;instructions;ingredient_name;ingredient_unit;ingredient_amount_per_serving",
                "Reisauflauf;Kochen;;;100");
        assertThrows(IllegalArgumentException.class, () -> service.importRecipes(
                new java.io.StringReader(csv),
                RecipeService.ImportFormat.CSV));
    }
}
