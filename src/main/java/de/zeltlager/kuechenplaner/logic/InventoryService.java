package de.zeltlager.kuechenplaner.logic;

import de.zeltlager.kuechenplaner.data.model.InventoryItem;

import java.util.List;
import java.util.Optional;

/**
 * Business logic related to managing the camp inventory.
 */
public interface InventoryService {

    List<InventoryItem> getInventory();

    Optional<InventoryItem> getInventoryItem(String ingredient);

    void upsertInventoryItem(InventoryItem item);
}
