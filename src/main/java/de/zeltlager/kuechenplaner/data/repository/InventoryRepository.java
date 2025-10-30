package de.zeltlager.kuechenplaner.data.repository;

import de.zeltlager.kuechenplaner.data.model.InventoryItem;

import java.util.List;
import java.util.Optional;

/**
 * Defines how inventory data can be accessed and updated.
 */
public interface InventoryRepository {

    List<InventoryItem> findAll();

    Optional<InventoryItem> findByIngredient(String ingredient);

    void save(InventoryItem item);
}
