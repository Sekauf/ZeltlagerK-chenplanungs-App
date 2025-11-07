package de.zeltlager.kuechenplaner.data.repository;

import de.zeltlager.kuechenplaner.data.model.MenuPlanEntry;

import java.time.LocalDate;
import java.util.List;

/**
 * Defines access to persistent menu plan data.
 */
public interface MenuPlanRepository {

    List<MenuPlanEntry> findAll();

    List<MenuPlanEntry> findByDate(LocalDate date);

    void save(MenuPlanEntry entry);

    void delete(MenuPlanEntry entry);
}
