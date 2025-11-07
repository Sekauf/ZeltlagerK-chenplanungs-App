package de.zeltlager.kuechenplaner.data.repository.jpa;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.zeltlager.kuechenplaner.data.model.Meal;
import de.zeltlager.kuechenplaner.data.model.MenuPlanEntry;
import de.zeltlager.kuechenplaner.data.persistence.entity.MenuPlanEntryEntity;
import de.zeltlager.kuechenplaner.data.persistence.entity.UserEntity;
import de.zeltlager.kuechenplaner.data.persistence.repository.MenuPlanEntryEntityRepository;
import de.zeltlager.kuechenplaner.data.repository.MenuPlanRepository;
import de.zeltlager.kuechenplaner.user.UserAccountService;

@Repository
@Transactional
public class JpaMenuPlanRepository implements MenuPlanRepository {

    private final MenuPlanEntryEntityRepository menuPlanEntryEntityRepository;
    private final UserAccountService userAccountService;

    public JpaMenuPlanRepository(MenuPlanEntryEntityRepository menuPlanEntryEntityRepository,
                                 UserAccountService userAccountService) {
        this.menuPlanEntryEntityRepository = menuPlanEntryEntityRepository;
        this.userAccountService = userAccountService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuPlanEntry> findAll() {
        UserEntity user = userAccountService.ensureCurrentUserEntity();
        return menuPlanEntryEntityRepository.findAllByUser_IdOrderByDateAsc(user.getId()).stream()
                .map(this::mapToDomain)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuPlanEntry> findByDate(LocalDate date) {
        UserEntity user = userAccountService.ensureCurrentUserEntity();
        return menuPlanEntryEntityRepository.findByUser_IdAndDateOrderByDateAsc(user.getId(), date).stream()
                .map(this::mapToDomain)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public void save(MenuPlanEntry entry) {
        UserEntity user = userAccountService.ensureCurrentUserEntity();
        MenuPlanEntryEntity entity = new MenuPlanEntryEntity();
        entity.setUser(user);
        entity.setDate(entry.getDate());
        entity.setMealName(entry.getMeal().getName());
        entity.setServings(entry.getMeal().getServings());
        menuPlanEntryEntityRepository.save(entity);
    }

    @Override
    public void delete(MenuPlanEntry entry) {
        UserEntity user = userAccountService.ensureCurrentUserEntity();
        menuPlanEntryEntityRepository.deleteByUser_IdAndDateAndMealNameIgnoreCaseAndServings(
                user.getId(), entry.getDate(), entry.getMeal().getName(), entry.getMeal().getServings());
    }

    private MenuPlanEntry mapToDomain(MenuPlanEntryEntity entity) {
        Meal meal = new Meal(entity.getMealName(), entity.getServings());
        return new MenuPlanEntry(entity.getDate(), meal);
    }
}
