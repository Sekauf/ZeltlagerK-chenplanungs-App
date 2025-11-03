package de.zeltlager.kuechenplaner.logic;

import de.zeltlager.kuechenplaner.data.model.Ingredient;
import de.zeltlager.kuechenplaner.data.model.Recipe;
import de.zeltlager.kuechenplaner.data.model.RecipeWithIngredients;
import de.zeltlager.kuechenplaner.data.model.ShoppingListItem;
import de.zeltlager.kuechenplaner.data.repository.RecipeRepository;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link RecipeService} that delegates to a {@link RecipeRepository}.
 */
public final class SimpleRecipeService implements RecipeService {

    private final RecipeRepository recipeRepository;
    private static final UnitConverter UNIT_CONVERTER = new UnitConverter();
    private static final IngredientCategorizer INGREDIENT_CATEGORIZER = new IngredientCategorizer();

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

    @Override
    public List<ShoppingListItem> generateShoppingList(List<RecipeSelection> selections) {
        Objects.requireNonNull(selections, "selections");
        if (selections.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> servingsByRecipe = new LinkedHashMap<>();
        for (RecipeSelection selection : selections) {
            Objects.requireNonNull(selection, "selection");
            servingsByRecipe.merge(selection.recipeId(), selection.servings(), Integer::sum);
        }

        Map<IngredientKey, IngredientAggregation> aggregations = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> entry : servingsByRecipe.entrySet()) {
            long recipeId = entry.getKey();
            RecipeWithIngredients recipe = recipeRepository.findById(recipeId)
                    .orElseThrow(() -> new IllegalArgumentException("Recipe with id " + recipeId + " does not exist"));

            int servings = entry.getValue();
            for (Ingredient ingredient : recipe.getIngredients()) {
                ConvertedAmount convertedAmount = UNIT_CONVERTER.convert(ingredient.getUnit(),
                        ingredient.getAmountPerServing() * servings);
                String displayUnit = convertedAmount.unit();
                IngredientKey key = IngredientKey.from(ingredient.getName(), displayUnit);
                IngredientAggregation aggregation = aggregations.computeIfAbsent(key,
                        unused -> new IngredientAggregation(
                                ingredient.getName(),
                                displayUnit,
                                INGREDIENT_CATEGORIZER.categorize(ingredient.getName()).orElse(null)));
                aggregation.addAmount(convertedAmount.amount());
                ingredient.getNotes().ifPresent(aggregation::addNote);
            }
        }

        return aggregations.values().stream()
                .map(IngredientAggregation::toShoppingListItem)
                .sorted(Comparator
                        .comparing((ShoppingListItem item) -> item.getCategory()
                                .map(value -> value.toLowerCase(Locale.ROOT))
                                .orElse("\uFFFF"))
                        .thenComparing(item -> item.getName().toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
    }

    private static String normalizeText(String value) {
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");
        normalized = normalized.toLowerCase(Locale.ROOT);
        normalized = normalized.replace("ß", "ss");
        return normalized;
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

    private static final class IngredientKey {
        private final String name;
        private final String unit;

        private IngredientKey(String name, String unit) {
            this.name = name;
            this.unit = unit;
        }

        static IngredientKey from(String name, String unit) {
            return new IngredientKey(normalizeText(name), normalizeText(unit));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof IngredientKey other)) {
                return false;
            }
            return name.equals(other.name) && unit.equals(other.unit);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, unit);
        }
    }

    private static final class IngredientAggregation {
        private final String name;
        private final String unit;
        private final String category;
        private double totalAmount;
        private final LinkedHashSet<String> notes = new LinkedHashSet<>();

        private IngredientAggregation(String name, String unit, String category) {
            this.name = name;
            this.unit = unit;
            this.category = category;
        }

        private void addAmount(double amount) {
            totalAmount += amount;
        }

        private void addNote(String note) {
            String trimmed = note.trim();
            if (!trimmed.isEmpty()) {
                notes.add(trimmed);
            }
        }

        private ShoppingListItem toShoppingListItem() {
            return new ShoppingListItem(name, unit, totalAmount, List.copyOf(notes), category);
        }
    }

    private static final class UnitConverter {
        private final Map<String, ConversionRule> conversions;

        private UnitConverter() {
            Map<String, ConversionRule> map = new LinkedHashMap<>();
            register(map, "g", "g", 1.0);
            register(map, "gramm", "g", 1.0);
            register(map, "kg", "g", 1_000.0);
            register(map, "kilogramm", "g", 1_000.0);
            register(map, "ml", "ml", 1.0);
            register(map, "milliliter", "ml", 1.0);
            register(map, "l", "ml", 1_000.0);
            register(map, "liter", "ml", 1_000.0);
            this.conversions = Map.copyOf(map);
        }

        private void register(Map<String, ConversionRule> map, String unit, String canonicalUnit, double factor) {
            map.put(normalizeText(unit), new ConversionRule(canonicalUnit, factor));
        }

        private ConvertedAmount convert(String unit, double amount) {
            String trimmedUnit = unit.trim();
            ConversionRule rule = conversions.get(normalizeText(unit));
            if (rule == null) {
                return new ConvertedAmount(amount, trimmedUnit);
            }
            return new ConvertedAmount(amount * rule.factor(), rule.canonicalUnit());
        }
    }

    private record ConversionRule(String canonicalUnit, double factor) {
    }

    private record ConvertedAmount(double amount, String unit) {
    }

    private static final class IngredientCategorizer {
        private final List<CategoryRule> rules;

        private IngredientCategorizer() {
            rules = List.of(
                    new CategoryRule("Obst & Gemüse", List.of("apfel", "banane", "birne", "karotte", "möhre", "paprika", "tomate", "gurke", "zwiebel", "salat", "kartoffel")),
                    new CategoryRule("Milchprodukte", List.of("milch", "käse", "quark", "joghurt", "butter", "sahne")),
                    new CategoryRule("Fleisch & Fisch", List.of("hähnchen", "rind", "schwein", "fleisch", "fisch", "lachs")),
                    new CategoryRule("Backwaren", List.of("brot", "brötchen", "toast", "croissant", "kuchen")),
                    new CategoryRule("Getränke", List.of("wasser", "saft", "tee", "kaffee"))
            );
        }

        private Optional<String> categorize(String ingredientName) {
            String normalized = normalizeText(ingredientName);
            for (CategoryRule rule : rules) {
                for (String keyword : rule.keywords()) {
                    if (normalized.contains(keyword)) {
                        return Optional.of(rule.category());
                    }
                }
            }
            return Optional.empty();
        }
    }

    private record CategoryRule(String category, List<String> keywords) {
    }
}
