package de.zeltlager.kuechenplaner.logic;

import de.zeltlager.kuechenplaner.data.model.Meal;
import de.zeltlager.kuechenplaner.data.model.MenuPlanEntry;
import de.zeltlager.kuechenplaner.data.repository.MenuPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimpleMenuPlanServiceTest {

    @Mock
    private MenuPlanRepository menuPlanRepository;

    @Test
    void getMenuPlanForDateDelegatesToRepository() {
        LocalDate today = LocalDate.now();
        List<MenuPlanEntry> entries = List.of(new MenuPlanEntry(today, new Meal("Mittag", 10)));
        when(menuPlanRepository.findByDate(today)).thenReturn(entries);

        SimpleMenuPlanService service = new SimpleMenuPlanService(menuPlanRepository);
        assertThat(service.getMenuPlan(today)).isEqualTo(entries);
    }

    @Test
    void getMenuPlanRejectsNullDate() {
        SimpleMenuPlanService service = new SimpleMenuPlanService(menuPlanRepository);
        assertThrows(NullPointerException.class, () -> service.getMenuPlan(null));
    }

    @Test
    void addMenuPlanEntryDelegatesToRepository() {
        MenuPlanEntry entry = new MenuPlanEntry(LocalDate.now(), new Meal("Abend", 15));
        SimpleMenuPlanService service = new SimpleMenuPlanService(menuPlanRepository);

        service.addMenuPlanEntry(entry);

        verify(menuPlanRepository).save(entry);
    }

    @Test
    void deleteMenuPlanEntryDelegatesToRepository() {
        MenuPlanEntry entry = new MenuPlanEntry(LocalDate.now(), new Meal("Frühstück", 20));
        SimpleMenuPlanService service = new SimpleMenuPlanService(menuPlanRepository);

        service.deleteMenuPlanEntry(entry);

        verify(menuPlanRepository).delete(entry);
    }
}
