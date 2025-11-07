package de.zeltlager.kuechenplaner.backup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.zeltlager.kuechenplaner.data.model.Ingredient;
import de.zeltlager.kuechenplaner.data.model.InventoryItem;
import de.zeltlager.kuechenplaner.data.model.Meal;
import de.zeltlager.kuechenplaner.data.model.MenuPlanEntry;
import de.zeltlager.kuechenplaner.data.model.Recipe;
import de.zeltlager.kuechenplaner.data.model.RecipeWithIngredients;
import de.zeltlager.kuechenplaner.logic.InventoryService;
import de.zeltlager.kuechenplaner.logic.MenuPlanService;
import de.zeltlager.kuechenplaner.logic.RecipeService;
import de.zeltlager.kuechenplaner.user.UserContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JsonBackupServiceTest {

    @Mock
    private RecipeService recipeService;

    @Mock
    private MenuPlanService menuPlanService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private UserContext userContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void createBackupWritesAggregatedDataToFile() throws IOException {
        Recipe recipe = new Recipe(1L, "Suppe", null, 4, "Kochen", Instant.parse("2024-01-01T10:00:00Z"), Instant.parse("2024-01-02T10:00:00Z"));
        Ingredient ingredient = new Ingredient(2L, 1L, "Kartoffel", "g", 100.0, "mehlig");
        RecipeWithIngredients recipeWithIngredients = new RecipeWithIngredients(recipe, List.of(ingredient));
        when(recipeService.getAllRecipes()).thenReturn(List.of(recipeWithIngredients));

        MenuPlanEntry menuPlanEntry = new MenuPlanEntry(LocalDate.of(2024, 7, 1), new Meal("Mittag", 50));
        when(menuPlanService.getMenuPlan()).thenReturn(List.of(menuPlanEntry));

        InventoryItem inventoryItem = new InventoryItem("Milch", 5, "l");
        when(inventoryService.getInventory()).thenReturn(List.of(inventoryItem));

        when(userContext.getCurrentUsername()).thenReturn("koch");

        JsonBackupService service = new JsonBackupService(recipeService, menuPlanService, inventoryService, objectMapper, userContext);
        Path target = tempDir.resolve("backup.json");

        Path written = service.createBackup(target);

        assertThat(written).exists();
        JsonNode root = objectMapper.readTree(written.toFile());
        assertThat(root.get("version").asText()).isEqualTo("1.0");
        assertThat(root.get("username").asText()).isEqualTo("koch");
        assertThat(root.get("recipes")).hasSize(1);
        JsonNode recipeNode = root.get("recipes").get(0);
        assertThat(recipeNode.get("name").asText()).isEqualTo("Suppe");
        assertThat(recipeNode.get("ingredients")).hasSize(1);
        assertThat(root.get("menuPlan")).hasSize(1);
        assertThat(root.get("inventory")).hasSize(1);
    }

    @Test
    void createBackupCreatesParentDirectories() throws IOException {
        when(recipeService.getAllRecipes()).thenReturn(List.of());
        when(menuPlanService.getMenuPlan()).thenReturn(List.of());
        when(inventoryService.getInventory()).thenReturn(List.of());
        when(userContext.getCurrentUsername()).thenReturn("user");

        JsonBackupService service = new JsonBackupService(recipeService, menuPlanService, inventoryService, objectMapper, userContext);
        Path nested = tempDir.resolve(Path.of("backups", "daily", "backup.json"));

        Path written = service.createBackup(nested);

        assertThat(Files.exists(written)).isTrue();
    }

    @Test
    void createBackupRejectsNullTarget() {
        JsonBackupService service = new JsonBackupService(recipeService, menuPlanService, inventoryService, objectMapper, userContext);
        assertThrows(NullPointerException.class, () -> service.createBackup(null));
    }
}
