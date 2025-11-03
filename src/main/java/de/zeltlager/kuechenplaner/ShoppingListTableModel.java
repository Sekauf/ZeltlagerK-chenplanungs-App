package de.zeltlager.kuechenplaner;

import de.zeltlager.kuechenplaner.data.model.ShoppingListItem;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * Table model representing aggregated shopping list items.
 */
public class ShoppingListTableModel extends AbstractTableModel {

    private static final String[] COLUMN_NAMES = {"Kategorie", "Zutat", "Menge", "Einheit", "Notizen"};

    private final List<ShoppingListItem> items = new ArrayList<>();

    public void setItems(List<ShoppingListItem> newItems) {
        items.clear();
        items.addAll(newItems);
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return items.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ShoppingListItem item = items.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> item.getCategory().orElse("-");
            case 1 -> item.getName();
            case 2 -> item.getTotalAmount();
            case 3 -> item.getUnit();
            case 4 -> item.getNotes().isEmpty() ? "" : String.join(", ", item.getNotes());
            default -> "";
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 2 -> Double.class;
            default -> String.class;
        };
    }

    public ShoppingListItem getItem(int rowIndex) {
        return items.get(rowIndex);
    }
}
