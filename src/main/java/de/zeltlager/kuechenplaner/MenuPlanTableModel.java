package de.zeltlager.kuechenplaner;

import de.zeltlager.kuechenplaner.data.model.MenuPlanEntry;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * Table model displaying menu plan entries.
 */
public class MenuPlanTableModel extends AbstractTableModel {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final String[] COLUMN_NAMES = {"Datum", "Gericht", "Portionen"};

    private final List<MenuPlanEntry> entries = new ArrayList<>();

    public void setEntries(List<MenuPlanEntry> newEntries) {
        entries.clear();
        entries.addAll(newEntries);
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return entries.size();
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
        MenuPlanEntry entry = entries.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> DATE_FORMATTER.format(entry.getDate());
            case 1 -> entry.getMeal().getName();
            case 2 -> entry.getMeal().getServings();
            default -> "";
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 2 -> Integer.class;
            default -> String.class;
        };
    }

    public MenuPlanEntry getEntry(int rowIndex) {
        return entries.get(rowIndex);
    }
}
