package de.zeltlager.kuechenplaner.backup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.zeltlager.kuechenplaner.data.model.Ingredient;
import de.zeltlager.kuechenplaner.data.model.InventoryItem;
import de.zeltlager.kuechenplaner.data.model.MenuPlanEntry;
import de.zeltlager.kuechenplaner.data.model.Recipe;
import de.zeltlager.kuechenplaner.data.model.RecipeWithIngredients;
import de.zeltlager.kuechenplaner.logic.InventoryService;
import de.zeltlager.kuechenplaner.logic.MenuPlanService;
import de.zeltlager.kuechenplaner.logic.RecipeService;
import de.zeltlager.kuechenplaner.user.UserContext;

@Service
public class JsonBackupService implements BackupService {

    private final RecipeService recipeService;
    private final MenuPlanService menuPlanService;
    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final UserContext userContext;

    public JsonBackupService(RecipeService recipeService,
                             MenuPlanService menuPlanService,
                             InventoryService inventoryService,
                             ObjectMapper objectMapper,
                             UserContext userContext) {
        this.recipeService = Objects.requireNonNull(recipeService, "recipeService");
        this.menuPlanService = Objects.requireNonNull(menuPlanService, "menuPlanService");
        this.inventoryService = Objects.requireNonNull(inventoryService, "inventoryService");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.userContext = Objects.requireNonNull(userContext, "userContext");
    }

    @Override
    public Path createBackup(Path targetFile) {
        Objects.requireNonNull(targetFile, "targetFile");
        Path normalizedPath = targetFile.toAbsolutePath();
        try {
            Path parent = normalizedPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            BackupPayload payload = buildPayload();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(normalizedPath.toFile(), payload);
            return normalizedPath;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create database backup", e);
        }
    }

    private BackupPayload buildPayload() {
        List<RecipeWithIngredients> recipes = recipeService.getAllRecipes();
        List<MenuPlanEntry> menuPlanEntries = menuPlanService.getMenuPlan();
        List<InventoryItem> inventoryItems = inventoryService.getInventory();

        List<RecipeBackup> recipeBackups = recipes.stream()
                .map(recipe -> new RecipeBackup(
                        recipe.getRecipe(),
                        recipe.getIngredients().stream()
                                .map(IngredientBackup::from)
                                .collect(Collectors.toUnmodifiableList())))
                .collect(Collectors.toUnmodifiableList());

        List<MenuPlanEntryBackup> menuPlanBackups = menuPlanEntries.stream()
                .map(entry -> new MenuPlanEntryBackup(
                        entry.getDate().toString(),
                        entry.getMeal().getName(),
                        entry.getMeal().getServings()))
                .collect(Collectors.toUnmodifiableList());

        List<InventoryItemBackup> inventoryBackups = inventoryItems.stream()
                .map(item -> new InventoryItemBackup(item.getIngredient(), item.getQuantity(), item.getUnit()))
                .collect(Collectors.toUnmodifiableList());

        return new BackupPayload(
                "1.0",
                Instant.now().toString(),
                userContext.getCurrentUsername(),
                recipeBackups,
                menuPlanBackups,
                inventoryBackups);
    }

    private record BackupPayload(String version,
                                 String generatedAt,
                                 String username,
                                 List<RecipeBackup> recipes,
                                 List<MenuPlanEntryBackup> menuPlan,
                                 List<InventoryItemBackup> inventory) {
    }

    private record RecipeBackup(Long id,
                                String name,
                                Long categoryId,
                                int baseServings,
                                String instructions,
                                String createdAt,
                                String updatedAt,
                                List<IngredientBackup> ingredients) {
        private RecipeBackup(Recipe recipe, List<IngredientBackup> ingredients) {
            this(recipe.getId().orElse(null),
                    recipe.getName(),
                    recipe.getCategoryId().orElse(null),
                    recipe.getBaseServings(),
                    recipe.getInstructions(),
                    recipe.getCreatedAt().map(Instant::toString).orElse(null),
                    recipe.getUpdatedAt().map(Instant::toString).orElse(null),
                    ingredients);
        }
    }

    private record IngredientBackup(Long id,
                                    Long recipeId,
                                    String name,
                                    String unit,
                                    double amountPerServing,
                                    String notes) {
        private static IngredientBackup from(Ingredient ingredient) {
            return new IngredientBackup(
                    ingredient.getId().orElse(null),
                    ingredient.getRecipeId().orElse(null),
                    ingredient.getName(),
                    ingredient.getUnit(),
                    ingredient.getAmountPerServing(),
                    ingredient.getNotes().orElse(null));
        }
    }

    private record MenuPlanEntryBackup(String date,
                                       String meal,
                                       int servings) {
    }

    private record InventoryItemBackup(String ingredient,
                                       int quantity,
                                       String unit) {
    }
}
