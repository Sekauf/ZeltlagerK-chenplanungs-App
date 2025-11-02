package de.zeltlager.kuechenplaner;

import de.zeltlager.kuechenplaner.data.model.InventoryItem;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * Table model for inventory items.
 */
public class InventoryTableModel extends AbstractTableModel {

    private static final String[] COLUMN_NAMES = {"Zutat", "Menge", "Einheit"};

    private final List<InventoryItem> items = new ArrayList<>();

    public void setItems(List<InventoryItem> newItems) {
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
        InventoryItem item = items.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> item.getIngredient();
            case 1 -> item.getQuantity();
            case 2 -> item.getUnit();
            default -> "";
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 1 -> Integer.class;
            default -> String.class;
        };
    }

    public InventoryItem getItem(int rowIndex) {
        return items.get(rowIndex);
    }
}
