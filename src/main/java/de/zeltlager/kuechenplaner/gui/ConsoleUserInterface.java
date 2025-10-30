package de.zeltlager.kuechenplaner.gui;

import de.zeltlager.kuechenplaner.data.model.InventoryItem;
import de.zeltlager.kuechenplaner.data.model.Meal;
import de.zeltlager.kuechenplaner.data.model.MenuPlanEntry;
import de.zeltlager.kuechenplaner.logic.InventoryService;
import de.zeltlager.kuechenplaner.logic.MenuPlanService;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Minimal console interface that showcases how the layers collaborate.
 */
public class ConsoleUserInterface implements UserInterface {

    private final MenuPlanService menuPlanService;
    private final InventoryService inventoryService;

    public ConsoleUserInterface(MenuPlanService menuPlanService, InventoryService inventoryService) {
        this.menuPlanService = Objects.requireNonNull(menuPlanService, "menuPlanService");
        this.inventoryService = Objects.requireNonNull(inventoryService, "inventoryService");
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

        // Beispielhafte Dummy-Daten, um den Ablauf zu demonstrieren.
        menuPlanService.addMenuPlanEntry(new MenuPlanEntry(LocalDate.now(), new Meal("Spaghetti Bolognese", 50)));
        inventoryService.upsertInventoryItem(new InventoryItem("Tomatensauce", 20, "l"));

        System.out.println("\nBeispieldaten wurden hinzugefügt.");
        System.out.println("Menüplan-Einträge: " + menuPlanService.getMenuPlan().size());
        System.out.println("Lagerartikel: " + inventoryService.getInventory().size());
    }
}
