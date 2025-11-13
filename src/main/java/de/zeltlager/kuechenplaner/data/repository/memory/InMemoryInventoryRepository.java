package de.zeltlager.kuechenplaner.data.repository.memory;

import de.zeltlager.kuechenplaner.data.model.InventoryItem;
import de.zeltlager.kuechenplaner.data.repository.InventoryRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * Simple in-memory inventory that can be replaced by a persistent implementation later.
 */
@Repository
@Profile("memory")
public class InMemoryInventoryRepository implements InventoryRepository {

    private final List<InventoryItem> items = new ArrayList<>();

    @Override
    public List<InventoryItem> findAll() {
        return Collections.unmodifiableList(items);
    }

    @Override
    public Optional<InventoryItem> findByIngredient(String ingredient) {
        return items.stream()
                .filter(item -> item.getIngredient().equalsIgnoreCase(ingredient))
                .findFirst();
    }

    @Override
    public void save(InventoryItem item) {
        findByIngredient(item.getIngredient())
                .ifPresentOrElse(existing -> {
                    items.remove(existing);
                    items.add(item);
                }, () -> items.add(item));
    }
}
