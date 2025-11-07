package de.zeltlager.kuechenplaner;

import de.zeltlager.kuechenplaner.data.repository.InventoryRepository;
import de.zeltlager.kuechenplaner.data.repository.MenuPlanRepository;
import de.zeltlager.kuechenplaner.data.repository.RecipeRepository;
import de.zeltlager.kuechenplaner.data.repository.sqlite.SqliteDatabase;
import de.zeltlager.kuechenplaner.data.repository.sqlite.SqliteInventoryRepository;
import de.zeltlager.kuechenplaner.data.repository.sqlite.SqliteMenuPlanRepository;
import de.zeltlager.kuechenplaner.data.repository.sqlite.SqliteRecipeRepository;
import de.zeltlager.kuechenplaner.logic.InventoryService;
import de.zeltlager.kuechenplaner.logic.MenuPlanService;
import de.zeltlager.kuechenplaner.logic.RecipeService;
import de.zeltlager.kuechenplaner.logic.SimpleInventoryService;
import de.zeltlager.kuechenplaner.logic.SimpleMenuPlanService;
import de.zeltlager.kuechenplaner.logic.SimpleRecipeService;

import java.nio.file.Path;

import javax.swing.SwingUtilities;

import de.zeltlager.kuechenplaner.ui.UiTheme;

/**
 * Entry point for the kitchen planning application.
 */
public final class App {

    private App() {
        // utility class
    }

    public static void main(String[] args) {
        Path databaseFile = Path.of("rezepte.db");

        SqliteDatabase database;
        try {
            database = new SqliteDatabase(databaseFile);
        } catch (IllegalStateException e) {
            System.err.println("Konnte Datenbank nicht initialisieren: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        MenuPlanRepository menuPlanRepository = new SqliteMenuPlanRepository(database.getConnection());
        InventoryRepository inventoryRepository = new SqliteInventoryRepository(database.getConnection());
        RecipeRepository recipeRepository = new SqliteRecipeRepository(database.getConnection());

        MenuPlanService menuPlanService = new SimpleMenuPlanService(menuPlanRepository);
        InventoryService inventoryService = new SimpleInventoryService(inventoryRepository);
        RecipeService recipeService = new SimpleRecipeService(recipeRepository);

        UiTheme.apply();

        SwingUtilities.invokeLater(() -> {
            try {
                MainWindow window = new MainWindow(menuPlanService, inventoryService, recipeService, database);
                window.showWindow();
            } catch (Exception e) {
                System.err.println("Konnte das Hauptfenster nicht starten: " + e.getMessage());
                e.printStackTrace();
                try {
                    database.close();
                } catch (Exception closeException) {
                    System.err.println("Datenbank konnte nicht geschlossen werden: " + closeException.getMessage());
                    closeException.printStackTrace();
                }
            }
        });
    }
}
