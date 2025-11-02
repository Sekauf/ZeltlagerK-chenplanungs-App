package de.zeltlager.kuechenplaner;

import de.zeltlager.kuechenplaner.data.repository.InventoryRepository;
import de.zeltlager.kuechenplaner.data.repository.MenuPlanRepository;
import de.zeltlager.kuechenplaner.data.repository.RecipeRepository;
import de.zeltlager.kuechenplaner.data.repository.sqlite.SqliteDatabase;
import de.zeltlager.kuechenplaner.data.repository.sqlite.SqliteInventoryRepository;
import de.zeltlager.kuechenplaner.data.repository.sqlite.SqliteMenuPlanRepository;
import de.zeltlager.kuechenplaner.data.repository.sqlite.SqliteRecipeRepository;
import de.zeltlager.kuechenplaner.gui.ConsoleUserInterface;
import de.zeltlager.kuechenplaner.gui.UserInterface;
import de.zeltlager.kuechenplaner.logic.InventoryService;
import de.zeltlager.kuechenplaner.logic.MenuPlanService;
import de.zeltlager.kuechenplaner.logic.RecipeService;
import de.zeltlager.kuechenplaner.logic.SimpleInventoryService;
import de.zeltlager.kuechenplaner.logic.SimpleMenuPlanService;
import de.zeltlager.kuechenplaner.logic.SimpleRecipeService;

import java.nio.file.Path;

/**
 * Application entry point that wires together the different architectural layers.
 */
public final class App {

    private App() {
        // utility class
    }

    public static void main(String[] args) {
        Path databaseFile = Path.of("rezepte.db");

        try (SqliteDatabase database = new SqliteDatabase(databaseFile)) {
            MenuPlanRepository menuPlanRepository = new SqliteMenuPlanRepository(database.getConnection());
            InventoryRepository inventoryRepository = new SqliteInventoryRepository(database.getConnection());
            RecipeRepository recipeRepository = new SqliteRecipeRepository(database.getConnection());

            MenuPlanService menuPlanService = new SimpleMenuPlanService(menuPlanRepository);
            InventoryService inventoryService = new SimpleInventoryService(inventoryRepository);
            RecipeService recipeService = new SimpleRecipeService(recipeRepository);

            UserInterface userInterface = new ConsoleUserInterface(menuPlanService, inventoryService, recipeService);
            userInterface.start();
        } catch (IllegalStateException e) {
            System.err.println("Konnte die Anwendung nicht starten: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
