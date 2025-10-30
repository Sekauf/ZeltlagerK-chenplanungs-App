package de.zeltlager.kuechenplaner.data.repository.memory;

import de.zeltlager.kuechenplaner.data.model.MenuPlanEntry;
import de.zeltlager.kuechenplaner.data.repository.MenuPlanRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * In-memory implementation that keeps menu plan data inside the running application.
 */
public class InMemoryMenuPlanRepository implements MenuPlanRepository {

    private final List<MenuPlanEntry> entries = new ArrayList<>();

    @Override
    public List<MenuPlanEntry> findAll() {
        return Collections.unmodifiableList(entries);
    }

    @Override
    public List<MenuPlanEntry> findByDate(LocalDate date) {
        return entries.stream()
                .filter(entry -> entry.getDate().equals(date))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public void save(MenuPlanEntry entry) {
        entries.add(entry);
    }
}
