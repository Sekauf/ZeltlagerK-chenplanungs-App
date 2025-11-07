package de.zeltlager.kuechenplaner.data.persistence.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import de.zeltlager.kuechenplaner.data.persistence.entity.MenuPlanEntryEntity;

public interface MenuPlanEntryEntityRepository extends JpaRepository<MenuPlanEntryEntity, Long> {

    List<MenuPlanEntryEntity> findAllByUser_IdOrderByDateAsc(Long userId);

    List<MenuPlanEntryEntity> findByUser_IdAndDateOrderByDateAsc(Long userId, LocalDate date);

    long deleteByUser_IdAndDateAndMealNameIgnoreCaseAndServings(Long userId, LocalDate date, String mealName, int servings);
}
