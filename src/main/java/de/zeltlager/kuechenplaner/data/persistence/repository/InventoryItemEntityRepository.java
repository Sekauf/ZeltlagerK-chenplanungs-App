package de.zeltlager.kuechenplaner.data.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import de.zeltlager.kuechenplaner.data.persistence.entity.InventoryItemEntity;

public interface InventoryItemEntityRepository extends JpaRepository<InventoryItemEntity, Long> {

    List<InventoryItemEntity> findAllByUser_IdOrderByIngredientAsc(Long userId);

    Optional<InventoryItemEntity> findByUser_IdAndIngredientIgnoreCase(Long userId, String ingredient);
}
