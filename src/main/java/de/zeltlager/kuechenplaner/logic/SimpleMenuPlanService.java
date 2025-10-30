package de.zeltlager.kuechenplaner.logic;

import de.zeltlager.kuechenplaner.data.model.MenuPlanEntry;
import de.zeltlager.kuechenplaner.data.repository.MenuPlanRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Default implementation delegating to a {@link MenuPlanRepository}.
 */
public class SimpleMenuPlanService implements MenuPlanService {

    private final MenuPlanRepository menuPlanRepository;

    public SimpleMenuPlanService(MenuPlanRepository menuPlanRepository) {
        this.menuPlanRepository = Objects.requireNonNull(menuPlanRepository, "menuPlanRepository");
    }

    @Override
    public List<MenuPlanEntry> getMenuPlan() {
        return menuPlanRepository.findAll();
    }

    @Override
    public List<MenuPlanEntry> getMenuPlan(LocalDate date) {
        return menuPlanRepository.findByDate(date);
    }

    @Override
    public void addMenuPlanEntry(MenuPlanEntry entry) {
        menuPlanRepository.save(entry);
    }
}
