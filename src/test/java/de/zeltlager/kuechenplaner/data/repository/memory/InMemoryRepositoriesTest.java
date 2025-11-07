package de.zeltlager.kuechenplaner.data.repository.memory;

import de.zeltlager.kuechenplaner.data.model.InventoryItem;
import de.zeltlager.kuechenplaner.data.model.Meal;
import de.zeltlager.kuechenplaner.data.model.MenuPlanEntry;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRepositoriesTest {

    @Test
    void inventoryRepositoryReplacesExistingItemsByIngredient() {
        InMemoryInventoryRepository repository = new InMemoryInventoryRepository();
        repository.save(new InventoryItem("Milch", 2, "l"));
        repository.save(new InventoryItem("milch", 5, "l"));

        List<InventoryItem> items = repository.findAll();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getQuantity()).isEqualTo(5);
        assertThat(repository.findByIngredient("MILCH")).isPresent();
    }

    @Test
    void menuPlanRepositorySupportsBasicCrudOperations() {
        InMemoryMenuPlanRepository repository = new InMemoryMenuPlanRepository();
        MenuPlanEntry monday = new MenuPlanEntry(LocalDate.of(2024, 7, 1), new Meal("Lunch", 40));
        MenuPlanEntry tuesday = new MenuPlanEntry(LocalDate.of(2024, 7, 2), new Meal("Dinner", 30));

        repository.save(monday);
        repository.save(tuesday);

        assertThat(repository.findAll()).containsExactly(monday, tuesday);
        assertThat(repository.findByDate(LocalDate.of(2024, 7, 1))).containsExactly(monday);

        repository.delete(new MenuPlanEntry(LocalDate.of(2024, 7, 2), new Meal("Dinner", 30)));
        assertThat(repository.findAll()).containsExactly(monday);
    }
}
