package de.zeltlager.kuechenplaner;

import de.zeltlager.kuechenplaner.data.model.InventoryItem;
import de.zeltlager.kuechenplaner.logic.InventoryService;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import org.springframework.stereotype.Component;

import de.zeltlager.kuechenplaner.ui.UiTheme;

/**
 * Panel that displays and updates the inventory backed by the SQLite database.
 */
@Component
public class InventoryPanel extends JPanel {

    private final InventoryService inventoryService;
    private final InventoryTableModel tableModel;
    private final JButton reloadButton;
    private final JButton saveButton;
    private final JTextField ingredientField;
    private final JSpinner quantitySpinner;
    private final JTextField unitField;
    private final JLabel statusLabel;

    public InventoryPanel(InventoryService inventoryService) {
        super(new BorderLayout(16, 16));
        this.inventoryService = Objects.requireNonNull(inventoryService, "inventoryService");
        this.tableModel = new InventoryTableModel();

        setOpaque(false);
        setBorder(new EmptyBorder(0, 0, 0, 0));

        JTable table = new JTable(tableModel);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        UiTheme.styleTable(table);

        JScrollPane tableScrollPane = new JScrollPane(table);
        UiTheme.styleScrollPane(tableScrollPane);

        reloadButton = UiTheme.createSecondaryButton("Aktualisieren");
        reloadButton.addActionListener(event -> reloadData());

        statusLabel = new JLabel("Bereit");
        statusLabel.setForeground(UiTheme.TEXT_MUTED);

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        headerActions.setOpaque(false);
        headerActions.add(statusLabel);
        headerActions.add(reloadButton);

        add(UiTheme.createHeader("Lagerbestand", headerActions), BorderLayout.NORTH);

        JPanel tableCard = UiTheme.createCard(new BorderLayout());
        tableCard.add(tableScrollPane, BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);

        JPanel formCard = UiTheme.createCard(new FlowLayout(FlowLayout.LEFT, 12, 8));
        formCard.add(new JLabel("Zutat:"));
        ingredientField = new JTextField(15);
        UiTheme.styleTextField(ingredientField);
        formCard.add(ingredientField);

        formCard.add(new JLabel("Menge:"));
        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 0, 100000, 1));
        JSpinner.DefaultEditor quantityEditor = (JSpinner.DefaultEditor) quantitySpinner.getEditor();
        quantityEditor.getTextField().setBackground(UiTheme.SURFACE_ALT);
        quantityEditor.getTextField().setForeground(UiTheme.TEXT_PRIMARY);
        quantityEditor.getTextField().setBorder(new EmptyBorder(8, 8, 8, 8));
        formCard.add(quantitySpinner);

        formCard.add(new JLabel("Einheit:"));
        unitField = new JTextField(8);
        UiTheme.styleTextField(unitField);
        formCard.add(unitField);

        saveButton = UiTheme.createPrimaryButton("Speichern");
        saveButton.addActionListener(event -> saveItem());
        formCard.add(saveButton);

        add(formCard, BorderLayout.SOUTH);
    }

    public void reloadData() {
        reloadButton.setEnabled(false);
        statusLabel.setText("Aktualisiere...");
        new SwingWorker<List<InventoryItem>, Void>() {
            @Override
            protected List<InventoryItem> doInBackground() {
                return inventoryService.getInventory();
            }

            @Override
            protected void done() {
                try {
                    List<InventoryItem> items = get();
                    tableModel.setItems(items);
                    statusLabel.setText(items.isEmpty() ? "Keine Artikel vorhanden" : items.size() + " Artikel");
                } catch (Exception e) {
                    showError("Lagerbestand konnte nicht geladen werden: " + e.getMessage());
                    statusLabel.setText("Fehler beim Laden");
                } finally {
                    reloadButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void saveItem() {
        String ingredient = ingredientField.getText().trim();
        if (ingredient.isEmpty()) {
            showError("Bitte eine Zutat angeben.");
            return;
        }

        int quantity = (Integer) quantitySpinner.getValue();
        String unit = unitField.getText().trim();
        if (unit.isEmpty()) {
            showError("Bitte eine Einheit angeben.");
            return;
        }

        InventoryItem item = new InventoryItem(ingredient, quantity, unit);

        toggleFormEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                inventoryService.upsertInventoryItem(item);
                return null;
            }

            @Override
            protected void done() {
                toggleFormEnabled(true);
                try {
                    get();
                    ingredientField.setText("");
                    quantitySpinner.setValue(1);
                    unitField.setText("");
                    reloadData();
                } catch (Exception e) {
                    showError("Eintrag konnte nicht gespeichert werden: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void toggleFormEnabled(boolean enabled) {
        ingredientField.setEnabled(enabled);
        quantitySpinner.setEnabled(enabled);
        unitField.setEnabled(enabled);
        saveButton.setEnabled(enabled);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Fehler", JOptionPane.ERROR_MESSAGE);
    }
}
