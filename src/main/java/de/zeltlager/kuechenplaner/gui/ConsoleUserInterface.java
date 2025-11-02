package de.zeltlager.kuechenplaner.gui;

import de.zeltlager.kuechenplaner.data.model.InventoryItem;
import de.zeltlager.kuechenplaner.data.model.Meal;
import de.zeltlager.kuechenplaner.data.model.MenuPlanEntry;
import de.zeltlager.kuechenplaner.logic.InventoryService;
import de.zeltlager.kuechenplaner.logic.MenuPlanService;

import java.time.LocalDate;
import java.util.List;
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
        ConsoleSnapshot snapshotBeforeSampleData = loadSnapshot();

        printHeader();
        printMenuPlanOverview(snapshotBeforeSampleData.menuPlanEntries());
        printInventoryOverview(snapshotBeforeSampleData.inventoryItems());

        addSampleData();

        ConsoleSnapshot snapshotAfterSampleData = loadSnapshot();
        printSummary(snapshotAfterSampleData);
    }

    private void printHeader() {
        System.out.println("Zeltlager Küchenplaner – Architekturprototyp");
    }

    private void printMenuPlanOverview(List<MenuPlanEntry> entries) {
        System.out.println("Aktueller Menüplan:");
        if (entries.isEmpty()) {
            printEmptyPlaceholder();
            return;
        }

        entries.stream()
                .map(this::formatMenuPlanEntry)
                .forEach(System.out::println);
    }

    private void printInventoryOverview(List<InventoryItem> items) {
        System.out.println("\nAktueller Lagerbestand:");
        if (items.isEmpty()) {
            printEmptyPlaceholder();
            return;
        }

        items.stream()
                .map(this::formatInventoryItem)
                .forEach(System.out::println);
    }

    private void printEmptyPlaceholder() {
        System.out.println("  (noch keine Einträge)");
    }

    private void addSampleData() {
        // Beispielhafte Dummy-Daten, um den Ablauf zu demonstrieren.
        menuPlanService.addMenuPlanEntry(new MenuPlanEntry(LocalDate.now(), new Meal("Spaghetti Bolognese", 50)));
        inventoryService.upsertInventoryItem(new InventoryItem("Tomatensauce", 20, "l"));
    }

    private void printSummary(ConsoleSnapshot snapshot) {
        System.out.println("\nBeispieldaten wurden hinzugefügt.");
        System.out.println("Menüplan-Einträge: " + snapshot.menuPlanEntries().size());
        System.out.println("Lagerartikel: " + snapshot.inventoryItems().size());
    }

    private ConsoleSnapshot loadSnapshot() {
        return new ConsoleSnapshot(
                List.copyOf(menuPlanService.getMenuPlan()),
                List.copyOf(inventoryService.getInventory())
        );
    }

    private String formatMenuPlanEntry(MenuPlanEntry entry) {
        Meal meal = entry.getMeal();
        return "  " + entry.getDate() + ": " + meal.getName() + " für " + meal.getServings() + " Personen";
    }

    private String formatInventoryItem(InventoryItem item) {
        return "  " + item.getIngredient() + ": " + item.getQuantity() + " " + item.getUnit();
    }

    private record ConsoleSnapshot(List<MenuPlanEntry> menuPlanEntries, List<InventoryItem> inventoryItems) {
    }
}
