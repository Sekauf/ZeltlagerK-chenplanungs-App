package de.zeltlager.kuechenplaner.logic;

import de.zeltlager.kuechenplaner.data.model.InventoryItem;
import de.zeltlager.kuechenplaner.data.repository.InventoryRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation delegating to an {@link InventoryRepository}.
 */
public class SimpleInventoryService implements InventoryService {

    private final InventoryRepository inventoryRepository;

    public SimpleInventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository, "inventoryRepository");
    }

    @Override
    public List<InventoryItem> getInventory() {
        return inventoryRepository.findAll();
    }

    @Override
    public Optional<InventoryItem> getInventoryItem(String ingredient) {
        return inventoryRepository.findByIngredient(Objects.requireNonNull(ingredient, "ingredient"));
    }

    @Override
    public void upsertInventoryItem(InventoryItem item) {
        inventoryRepository.save(Objects.requireNonNull(item, "item"));
    }
}
