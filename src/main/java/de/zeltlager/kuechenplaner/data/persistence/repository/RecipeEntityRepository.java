package de.zeltlager.kuechenplaner.data.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import de.zeltlager.kuechenplaner.data.persistence.entity.RecipeEntity;

public interface RecipeEntityRepository extends JpaRepository<RecipeEntity, Long> {

    @EntityGraph(attributePaths = "ingredients")
    List<RecipeEntity> findAllByUser_IdOrderByNameAsc(Long userId);

    @EntityGraph(attributePaths = "ingredients")
    Optional<RecipeEntity> findByIdAndUser_Id(Long id, Long userId);

    void deleteByIdAndUser_Id(Long id, Long userId);
}
