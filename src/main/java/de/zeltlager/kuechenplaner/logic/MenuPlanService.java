package de.zeltlager.kuechenplaner.logic;

import de.zeltlager.kuechenplaner.data.model.MenuPlanEntry;

import java.time.LocalDate;
import java.util.List;

/**
 * Business logic related to menu planning.
 */
public interface MenuPlanService {

    List<MenuPlanEntry> getMenuPlan();

    List<MenuPlanEntry> getMenuPlan(LocalDate date);

    void addMenuPlanEntry(MenuPlanEntry entry);
}
