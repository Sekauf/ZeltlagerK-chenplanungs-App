package de.zeltlager.kuechenplaner.logic;

import de.zeltlager.kuechenplaner.data.model.InventoryItem;
import de.zeltlager.kuechenplaner.data.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimpleInventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Test
    void getInventoryReturnsAllItemsFromRepository() {
        List<InventoryItem> items = List.of(new InventoryItem("Kartoffeln", 5, "kg"));
        when(inventoryRepository.findAll()).thenReturn(items);

        SimpleInventoryService service = new SimpleInventoryService(inventoryRepository);
        assertThat(service.getInventory()).isEqualTo(items);
    }

    @Test
    void getInventoryItemRejectsNullIngredient() {
        SimpleInventoryService service = new SimpleInventoryService(inventoryRepository);
        assertThrows(NullPointerException.class, () -> service.getInventoryItem(null));
    }

    @Test
    void upsertInventoryItemDelegatesToRepository() {
        InventoryItem item = new InventoryItem("Milch", 2, "l");
        SimpleInventoryService service = new SimpleInventoryService(inventoryRepository);

        service.upsertInventoryItem(item);

        verify(inventoryRepository).save(item);
    }
}
