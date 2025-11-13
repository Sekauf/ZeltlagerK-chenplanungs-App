package de.zeltlager.kuechenplaner.data.repository.jpa;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.zeltlager.kuechenplaner.data.model.InventoryItem;
import de.zeltlager.kuechenplaner.data.persistence.entity.InventoryItemEntity;
import de.zeltlager.kuechenplaner.data.persistence.entity.UserEntity;
import de.zeltlager.kuechenplaner.data.persistence.repository.InventoryItemEntityRepository;
import de.zeltlager.kuechenplaner.data.repository.InventoryRepository;
import de.zeltlager.kuechenplaner.user.UserAccountService;

@Repository
@Profile("!memory")
@Transactional
public class JpaInventoryRepository implements InventoryRepository {

    private final InventoryItemEntityRepository inventoryItemEntityRepository;
    private final UserAccountService userAccountService;

    public JpaInventoryRepository(InventoryItemEntityRepository inventoryItemEntityRepository,
                                  UserAccountService userAccountService) {
        this.inventoryItemEntityRepository = inventoryItemEntityRepository;
        this.userAccountService = userAccountService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> findAll() {
        UserEntity user = userAccountService.ensureCurrentUserEntity();
        return inventoryItemEntityRepository.findAllByUser_IdOrderByIngredientAsc(user.getId()).stream()
                .map(this::mapToDomain)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InventoryItem> findByIngredient(String ingredient) {
        UserEntity user = userAccountService.ensureCurrentUserEntity();
        return inventoryItemEntityRepository
                .findByUser_IdAndIngredientIgnoreCase(user.getId(), ingredient)
                .map(this::mapToDomain);
    }

    @Override
    public void save(InventoryItem item) {
        UserEntity user = userAccountService.ensureCurrentUserEntity();
        String sanitizedIngredient = sanitize(item.getIngredient());

        InventoryItemEntity entity = inventoryItemEntityRepository
                .findByUser_IdAndIngredientIgnoreCase(user.getId(), sanitizedIngredient)
                .orElseGet(InventoryItemEntity::new);

        entity.setUser(user);
        entity.setIngredient(sanitizedIngredient);
        entity.setQuantity(item.getQuantity());
        entity.setUnit(item.getUnit());

        inventoryItemEntityRepository.save(entity);
    }

    private InventoryItem mapToDomain(InventoryItemEntity entity) {
        return new InventoryItem(entity.getIngredient(), entity.getQuantity(), entity.getUnit());
    }

    private String sanitize(String ingredient) {
        if (ingredient == null) {
            throw new IllegalArgumentException("Ingredient name must not be null");
        }
        String trimmed = ingredient.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Ingredient name must not be blank");
        }
        return trimmed;
    }
}
