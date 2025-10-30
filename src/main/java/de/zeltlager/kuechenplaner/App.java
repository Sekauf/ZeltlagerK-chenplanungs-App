package de.zeltlager.kuechenplaner;

import de.zeltlager.kuechenplaner.data.repository.InventoryRepository;
import de.zeltlager.kuechenplaner.data.repository.MenuPlanRepository;
import de.zeltlager.kuechenplaner.data.repository.memory.InMemoryInventoryRepository;
import de.zeltlager.kuechenplaner.data.repository.memory.InMemoryMenuPlanRepository;
import de.zeltlager.kuechenplaner.gui.ConsoleUserInterface;
import de.zeltlager.kuechenplaner.gui.UserInterface;
import de.zeltlager.kuechenplaner.logic.InventoryService;
import de.zeltlager.kuechenplaner.logic.MenuPlanService;
import de.zeltlager.kuechenplaner.logic.SimpleInventoryService;
import de.zeltlager.kuechenplaner.logic.SimpleMenuPlanService;

/**
 * Application entry point that wires together the different architectural layers.
 */
public final class App {

    private App() {
        // utility class
    }

    public static void main(String[] args) {
        MenuPlanRepository menuPlanRepository = new InMemoryMenuPlanRepository();
        InventoryRepository inventoryRepository = new InMemoryInventoryRepository();

        MenuPlanService menuPlanService = new SimpleMenuPlanService(menuPlanRepository);
        InventoryService inventoryService = new SimpleInventoryService(inventoryRepository);

        UserInterface userInterface = new ConsoleUserInterface(menuPlanService, inventoryService);
        userInterface.start();
    }
}
