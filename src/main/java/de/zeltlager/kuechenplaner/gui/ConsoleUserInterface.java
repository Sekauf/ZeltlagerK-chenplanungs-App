package de.zeltlager.kuechenplaner.gui;

import de.zeltlager.kuechenplaner.data.model.Ingredient;
import de.zeltlager.kuechenplaner.data.model.InventoryItem;
import de.zeltlager.kuechenplaner.data.model.Meal;
import de.zeltlager.kuechenplaner.data.model.MenuPlanEntry;
import de.zeltlager.kuechenplaner.data.model.RecipeWithIngredients;
import de.zeltlager.kuechenplaner.logic.InventoryService;
import de.zeltlager.kuechenplaner.logic.MenuPlanService;
import de.zeltlager.kuechenplaner.logic.RecipeService;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Minimal console interface that showcases how the layers collaborate.
 */
public class ConsoleUserInterface implements UserInterface {

    private final MenuPlanService menuPlanService;
    private final InventoryService inventoryService;
    private final RecipeService recipeService;

    public ConsoleUserInterface(MenuPlanService menuPlanService,
                                InventoryService inventoryService,
                                RecipeService recipeService) {
        this.menuPlanService = Objects.requireNonNull(menuPlanService, "menuPlanService");
        this.inventoryService = Objects.requireNonNull(inventoryService, "inventoryService");
        this.recipeService = Objects.requireNonNull(recipeService, "recipeService");
    }

    @Override
    public void start() {
        System.out.println("Zeltlager Küchenplaner – Architekturprototyp");
        System.out.println("Aktueller Menüplan:");
        if (menuPlanService.getMenuPlan().isEmpty()) {
            System.out.println("  (noch keine Einträge)");
        }

        System.out.println("\nAktueller Lagerbestand:");
        if (inventoryService.getInventory().isEmpty()) {
            System.out.println("  (noch keine Einträge)");
        }

        System.out.println("\nAktuelle Rezepte:");
        List<RecipeWithIngredients> recipes = recipeService.getAllRecipes();
        if (recipes.isEmpty()) {
            System.out.println("  (noch keine Einträge)");
        }

        // Beispielhafte Dummy-Daten, um den Ablauf zu demonstrieren.
        menuPlanService.addMenuPlanEntry(new MenuPlanEntry(LocalDate.now(), new Meal("Spaghetti Bolognese", 50)));
        inventoryService.upsertInventoryItem(new InventoryItem("Tomatensauce", 20, "l"));

        RecipeWithIngredients exampleRecipe = recipes.isEmpty()
                ? recipeService.createRecipe(
                        "Vegane Gemüsesuppe",
                        null,
                        10,
                        "Gemüse schneiden, anbraten und mit Brühe köcheln lassen.",
                        List.of(
                                new Ingredient(null, null, "Karotten", "kg", 0.1, null),
                                new Ingredient(null, null, "Kartoffeln", "kg", 0.12, null),
                                new Ingredient(null, null, "Gemüsebrühe", "l", 0.25, "Hausgemacht oder Instant")))
                : recipes.get(0);
        if (recipes.isEmpty()) {
            recipes = recipeService.getAllRecipes();
        }

        System.out.println("\nBeispieldaten wurden hinzugefügt.");
        System.out.println("Menüplan-Einträge: " + menuPlanService.getMenuPlan().size());
        System.out.println("Lagerartikel: " + inventoryService.getInventory().size());
        System.out.println("Rezepte: " + recipes.size());
        System.out.println("Beispielrezept: " + exampleRecipe.getRecipe().getName() +
                " (" + exampleRecipe.getIngredients().size() + " Zutaten)");
    }
}
