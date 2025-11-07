package de.zeltlager.kuechenplaner.logic;

import de.zeltlager.kuechenplaner.data.model.Ingredient;
import de.zeltlager.kuechenplaner.data.model.Recipe;
import de.zeltlager.kuechenplaner.data.model.RecipeWithIngredients;
import de.zeltlager.kuechenplaner.data.model.ShoppingListItem;
import de.zeltlager.kuechenplaner.data.repository.RecipeRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
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

    @Override
    public List<RecipeWithIngredients> importRecipes(Reader reader, ImportFormat format) {
        Objects.requireNonNull(reader, "reader");
        Objects.requireNonNull(format, "format");

        List<ImportedRecipe> importedRecipes;
        try {
            importedRecipes = switch (format) {
                case CSV -> parseCsv(reader);
                case MEAL_MASTER -> parseMealMaster(reader);
            };
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read recipe import data", e);
        }

        if (importedRecipes.isEmpty()) {
            return List.of();
        }

        List<RecipeWithIngredients> persisted = new ArrayList<>(importedRecipes.size());
        for (ImportedRecipe imported : importedRecipes) {
            List<Ingredient> ingredients = imported.ingredients().stream()
                    .map(ingredient -> new Ingredient(
                            null,
                            null,
                            ingredient.name(),
                            ingredient.unit(),
                            ingredient.amountPerServing(),
                            ingredient.notes()))
                    .toList();
            RecipeWithIngredients created = createRecipe(
                    imported.name(),
                    imported.categoryId().orElse(null),
                    imported.baseServings(),
                    imported.instructions(),
                    ingredients);
            persisted.add(created);
        }
        return List.copyOf(persisted);
    }

    private List<ImportedRecipe> parseCsv(Reader reader) throws IOException {
        BufferedReader bufferedReader = reader instanceof BufferedReader br ? br : new BufferedReader(reader);
        String headerLine;
        while ((headerLine = bufferedReader.readLine()) != null) {
            if (!headerLine.trim().isEmpty()) {
                break;
            }
        }

        if (headerLine == null) {
            return List.of();
        }

        List<String> headers = parseCsvLine(headerLine);
        if (headers.isEmpty()) {
            throw new IllegalArgumentException("CSV import requires at least one header column");
        }

        Map<String, Integer> headerIndex = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            headerIndex.put(headers.get(i).trim().toLowerCase(Locale.ROOT), i);
        }

        CsvColumn column = CsvColumn.fromHeaderIndex(headerIndex);

        Map<String, CsvRecipeBuilder> recipes = new LinkedHashMap<>();
        String line;
        int lineNumber = 1;
        while ((line = bufferedReader.readLine()) != null) {
            lineNumber++;
            if (line.trim().isEmpty()) {
                continue;
            }

            List<String> values = parseCsvLine(line);
            while (values.size() < headers.size()) {
                values.add("");
            }

            String name = value(values, column.nameIndex()).trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Missing recipe name in CSV at line " + lineNumber);
            }

            Long categoryId = null;
            if (column.categoryIndex() != null) {
                String rawCategory = value(values, column.categoryIndex());
                if (!rawCategory.trim().isEmpty()) {
                    try {
                        categoryId = Long.parseLong(rawCategory.trim());
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid category id '" + rawCategory + "' in CSV at line " + lineNumber, e);
                    }
                }
            }

            int baseServings = column.baseServingsIndex() != null
                    ? parsePositiveInt(value(values, column.baseServingsIndex()), lineNumber, "base servings")
                    : 1;
            String instructions = value(values, column.instructionsIndex()).trim();

            double amountPerServing = parseDouble(value(values, column.ingredientAmountIndex()), lineNumber, "ingredient amount");
            if (amountPerServing < 0) {
                throw new IllegalArgumentException("Ingredient amount must not be negative in CSV at line " + lineNumber);
            }

            String ingredientName = value(values, column.ingredientNameIndex()).trim();
            if (ingredientName.isEmpty()) {
                throw new IllegalArgumentException("Missing ingredient name in CSV at line " + lineNumber);
            }

            String ingredientUnit = value(values, column.ingredientUnitIndex()).trim();
            if (ingredientUnit.isEmpty()) {
                ingredientUnit = "";
            }

            String notes = column.ingredientNotesIndex() != null
                    ? value(values, column.ingredientNotesIndex()).trim()
                    : "";

            String key = name.toLowerCase(Locale.ROOT) + '\u0000' + instructions;
            Long categoryIdFinal = categoryId;
            int baseServingsFinal = baseServings;
            String instructionsFinal = instructions;
            CsvRecipeBuilder builder = recipes.computeIfAbsent(key,
                    unused -> new CsvRecipeBuilder(name, categoryIdFinal, baseServingsFinal, instructionsFinal));
            builder.ensureCompatibility(categoryId, baseServings, instructions, lineNumber);
            builder.addIngredient(new ImportedIngredient(ingredientName, ingredientUnit, amountPerServing, notes.isEmpty() ? null : notes));
        }

        return recipes.values().stream()
                .map(CsvRecipeBuilder::build)
                .collect(Collectors.toList());
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(ch);
                }
            } else {
                if (ch == '"') {
                    inQuotes = true;
                } else if (ch == ';' || ch == ',') {
                    values.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(ch);
                }
            }
        }
        values.add(current.toString());
        return values;
    }

    private String value(List<String> values, int index) {
        return index < values.size() ? values.get(index) : "";
    }

    private int parsePositiveInt(String raw, int lineNumber, String description) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return 1;
        }
        try {
            int value = Integer.parseInt(trimmed);
            if (value <= 0) {
                throw new IllegalArgumentException(description + " must be positive in CSV at line " + lineNumber);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number '" + raw + "' for " + description + " in CSV at line " + lineNumber, e);
        }
    }

    private double parseDouble(String raw, int lineNumber, String description) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(trimmed.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number '" + raw + "' for " + description + " in CSV at line " + lineNumber, e);
        }
    }

    private List<ImportedRecipe> parseMealMaster(Reader reader) throws IOException {
        BufferedReader bufferedReader = reader instanceof BufferedReader br ? br : new BufferedReader(reader);
        List<ImportedRecipe> recipes = new ArrayList<>();
        MealMasterBuilder builder = null;
        MealMasterSection section = MealMasterSection.HEADER;
        boolean headerHasContent = false;
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if (line.startsWith("MMMMM")) {
                if (builder != null) {
                    recipes.add(builder.build());
                }
                builder = new MealMasterBuilder();
                section = MealMasterSection.HEADER;
                headerHasContent = false;
                continue;
            }

            if (builder == null) {
                continue;
            }

            String trimmed = line.trim();
            if (section == MealMasterSection.HEADER) {
                if (trimmed.isEmpty()) {
                    if (headerHasContent) {
                        section = MealMasterSection.INGREDIENTS;
                    }
                    continue;
                }
                String lower = trimmed.toLowerCase(Locale.ROOT);
                if (lower.startsWith("title:")) {
                    builder.setName(trimmed.substring(6).trim());
                    headerHasContent = true;
                } else if (lower.startsWith("yield:")) {
                    builder.setBaseServings(parseYield(trimmed.substring(6).trim()));
                    headerHasContent = true;
                } else if (lower.startsWith("categories:")) {
                    headerHasContent = true;
                }
                continue;
            }

            if (trimmed.isEmpty()) {
                if (section == MealMasterSection.INGREDIENTS) {
                    section = MealMasterSection.INSTRUCTIONS;
                } else {
                    builder.appendInstructionLine("");
                }
                continue;
            }

            if (section == MealMasterSection.INGREDIENTS) {
                MealMasterIngredient ingredient = parseMealMasterIngredient(line);
                if (ingredient == null) {
                    section = MealMasterSection.INSTRUCTIONS;
                    builder.appendInstructionLine(trimmed);
                } else {
                    builder.addIngredient(ingredient);
                }
            } else {
                builder.appendInstructionLine(trimmed);
            }
        }

        if (builder != null) {
            recipes.add(builder.build());
        }
        return List.copyOf(recipes);
    }

    private int parseYield(String rawYield) {
        String normalized = rawYield.toLowerCase(Locale.ROOT);
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (Character.isDigit(ch) || ch == '/' || ch == ' ' || ch == ',' || ch == '.') {
                digits.append(ch);
            } else if (digits.length() > 0) {
                break;
            }
        }
        String value = digits.toString().trim();
        if (value.isEmpty()) {
            return 1;
        }
        double servings = parseMealMasterAmount(value);
        int rounded = (int) Math.round(servings);
        return Math.max(rounded, 1);
    }

    private MealMasterIngredient parseMealMasterIngredient(String line) {
        String amountPart = substring(line, 0, 7).trim();
        String unitPart = substring(line, 7, 14).trim();
        String namePart = line.length() > 14 ? line.substring(14).trim() : "";

        if (amountPart.isEmpty() && unitPart.isEmpty()) {
            return null;
        }

        if (namePart.isEmpty()) {
            return null;
        }

        Double amount = parseMealMasterAmount(amountPart);
        if (amount == null) {
            return null;
        }

        return new MealMasterIngredient(namePart, unitPart, amount);
    }

    private String substring(String line, int start, int end) {
        if (line.length() <= start) {
            return "";
        }
        return line.substring(start, Math.min(end, line.length()));
    }

    private Double parseMealMasterAmount(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;
        String[] parts = trimmed.split("\\s+");
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            String normalized = part.replace(',', '.');
            if (normalized.equalsIgnoreCase("-")) {
                return null;
            }
            if (normalized.contains("/")) {
                String[] fraction = normalized.split("/");
                if (fraction.length != 2) {
                    return null;
                }
                try {
                    double numerator = Double.parseDouble(fraction[0]);
                    double denominator = Double.parseDouble(fraction[1]);
                    if (denominator == 0.0) {
                        return null;
                    }
                    total += numerator / denominator;
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                try {
                    total += Double.parseDouble(normalized);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return total;
    }

    private enum MealMasterSection {
        HEADER,
        INGREDIENTS,
        INSTRUCTIONS
    }

    private record ImportedRecipe(String name,
                                   Optional<Long> categoryId,
                                   int baseServings,
                                   String instructions,
                                   List<ImportedIngredient> ingredients) {
    }

    private record ImportedIngredient(String name, String unit, double amountPerServing, String notes) {
    }

    private record MealMasterIngredient(String name, String unit, double amount) {
    }

    private static final class CsvRecipeBuilder {
        private final String name;
        private final Long categoryId;
        private final int baseServings;
        private final String instructions;
        private final List<ImportedIngredient> ingredients = new ArrayList<>();

        private CsvRecipeBuilder(String name, Long categoryId, int baseServings, String instructions) {
            this.name = name;
            this.categoryId = categoryId;
            this.baseServings = baseServings > 0 ? baseServings : 1;
            this.instructions = instructions;
        }

        private void ensureCompatibility(Long categoryId, int baseServings, String instructions, int lineNumber) {
            if (!Objects.equals(this.categoryId, categoryId)) {
                throw new IllegalArgumentException("Conflicting category for recipe '" + name + "' in CSV at line " + lineNumber);
            }
            if (this.baseServings != baseServings) {
                throw new IllegalArgumentException("Conflicting base servings for recipe '" + name + "' in CSV at line " + lineNumber);
            }
            if (!Objects.equals(this.instructions, instructions)) {
                throw new IllegalArgumentException("Conflicting instructions for recipe '" + name + "' in CSV at line " + lineNumber);
            }
        }

        private void addIngredient(ImportedIngredient ingredient) {
            ingredients.add(ingredient);
        }

        private ImportedRecipe build() {
            if (ingredients.isEmpty()) {
                throw new IllegalArgumentException("Recipe '" + name + "' must contain at least one ingredient");
            }
            return new ImportedRecipe(name, Optional.ofNullable(categoryId), baseServings, instructions, List.copyOf(ingredients));
        }
    }

    private static final class MealMasterBuilder {
        private String name;
        private int baseServings = 1;
        private final List<MealMasterIngredient> ingredients = new ArrayList<>();
        private final StringJoiner instructions = new StringJoiner("\n");

        private void setName(String name) {
            if (!name.isBlank()) {
                this.name = name;
            }
        }

        private void setBaseServings(int baseServings) {
            if (baseServings > 0) {
                this.baseServings = baseServings;
            }
        }

        private void addIngredient(MealMasterIngredient ingredient) {
            ingredients.add(ingredient);
        }

        private void appendInstructionLine(String line) {
            instructions.add(line);
        }

        private ImportedRecipe build() {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("MealMaster recipe is missing a title");
            }

            List<ImportedIngredient> converted = new ArrayList<>();
            int servings = Math.max(baseServings, 1);
            for (MealMasterIngredient ingredient : ingredients) {
                double amountPerServing = ingredient.amount() / servings;
                converted.add(new ImportedIngredient(
                        ingredient.name(),
                        ingredient.unit().isEmpty() ? "" : ingredient.unit(),
                        amountPerServing,
                        null));
            }

            String instructionText = instructions.toString().trim();
            return new ImportedRecipe(name, Optional.empty(), servings, instructionText, List.copyOf(converted));
        }
    }

    private record CsvColumn(int nameIndex,
                             Integer categoryIndex,
                             Integer baseServingsIndex,
                             int instructionsIndex,
                             int ingredientNameIndex,
                             int ingredientUnitIndex,
                             int ingredientAmountIndex,
                             Integer ingredientNotesIndex) {

        static CsvColumn fromHeaderIndex(Map<String, Integer> headerIndex) {
            Map<CsvField, Integer> indices = new EnumMap<>(CsvField.class);
            for (Map.Entry<String, Integer> entry : headerIndex.entrySet()) {
                CsvField field = CsvField.fromHeader(entry.getKey());
                if (field != null) {
                    indices.putIfAbsent(field, entry.getValue());
                }
            }

            Integer name = indices.get(CsvField.NAME);
            Integer instructions = indices.get(CsvField.INSTRUCTIONS);
            Integer ingredientName = indices.get(CsvField.INGREDIENT_NAME);
            Integer ingredientUnit = indices.get(CsvField.INGREDIENT_UNIT);
            Integer ingredientAmount = indices.get(CsvField.INGREDIENT_AMOUNT_PER_SERVING);

            if (name == null || instructions == null || ingredientName == null || ingredientUnit == null || ingredientAmount == null) {
                throw new IllegalArgumentException("CSV header is missing required columns. Required: name, instructions, ingredient_name, ingredient_unit, ingredient_amount_per_serving");
            }

            return new CsvColumn(
                    name,
                    indices.get(CsvField.CATEGORY_ID),
                    indices.get(CsvField.BASE_SERVINGS),
                    instructions,
                    ingredientName,
                    ingredientUnit,
                    ingredientAmount,
                    indices.get(CsvField.INGREDIENT_NOTES));
        }
    }

    private enum CsvField {
        NAME("name"),
        CATEGORY_ID("category_id"),
        BASE_SERVINGS("base_servings"),
        INSTRUCTIONS("instructions"),
        INGREDIENT_NAME("ingredient_name"),
        INGREDIENT_UNIT("ingredient_unit"),
        INGREDIENT_AMOUNT_PER_SERVING("ingredient_amount_per_serving"),
        INGREDIENT_NOTES("ingredient_notes");

        private final String key;

        CsvField(String key) {
            this.key = key;
        }

        static CsvField fromHeader(String header) {
            String normalized = header.trim().toLowerCase(Locale.ROOT);
            for (CsvField field : values()) {
                if (field.key.equals(normalized)) {
                    return field;
                }
            }
            return null;
        }
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
