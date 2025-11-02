package de.zeltlager.kuechenplaner;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.awt.Window;
import java.util.Objects;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

/**
 * Modal dialog that captures the details of a single recipe.
 */
public class RecipeDetailDialog extends JDialog {

    /**
     * Immutable record containing the user input of the dialog.
     */
    public record FormData(String name,
                           Long categoryId,
                           int baseServings,
                           String instructions,
                           java.util.List<IngredientFormEntry> ingredients) { }

    /**
     * Represents a single ingredient entry of the form.
     */
    public record IngredientFormEntry(Long id,
                                      Long recipeId,
                                      String name,
                                      String unit,
                                      double amountPerServing,
                                      String notes) { }

    private final JTextField nameField;
    private final JTextField categoryField;
    private final JSpinner baseServingsSpinner;
    private final JTextArea instructionsArea;
    private final JButton saveButton;
    private final JButton cancelButton;
    private final JTable ingredientTable;
    private final IngredientTableModel ingredientTableModel;
    private final JButton addIngredientButton;
    private final JButton editIngredientButton;
    private final JButton removeIngredientButton;

    private boolean confirmed;

    public RecipeDetailDialog(Window owner) {
        super(owner, "Rezept bearbeiten", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        nameField = new JTextField(25);
        categoryField = new JTextField(25);
        baseServingsSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));
        instructionsArea = new JTextArea(8, 30);
        instructionsArea.setLineWrap(true);
        instructionsArea.setWrapStyleWord(true);

        saveButton = new JButton("Speichern");
        saveButton.addActionListener(event -> onSave());

        cancelButton = new JButton("Abbrechen");
        cancelButton.addActionListener(event -> onCancel());

        ingredientTableModel = new IngredientTableModel();
        ingredientTable = new JTable(ingredientTableModel);
        ingredientTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ingredientTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateIngredientButtons();
            }
        });
        ingredientTable.setFillsViewportHeight(true);

        addIngredientButton = new JButton("Hinzufügen");
        addIngredientButton.addActionListener(event -> onAddIngredient());

        editIngredientButton = new JButton("Bearbeiten");
        editIngredientButton.addActionListener(event -> onEditIngredient());

        removeIngredientButton = new JButton("Entfernen");
        removeIngredientButton.addActionListener(event -> onRemoveIngredient());

        setLayout(new BorderLayout());
        add(createFormPanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        getRootPane().setDefaultButton(saveButton);
        pack();
        setLocationRelativeTo(owner);
        updateIngredientButtons();
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(new JLabel("Name:"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        panel.add(nameField, constraints);

        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0.0;
        panel.add(new JLabel("Kategorie (ID):"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        panel.add(categoryField, constraints);

        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0.0;
        panel.add(new JLabel("Basisportionen:"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        panel.add(baseServingsSpinner, constraints);

        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0.0;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("Zubereitung:"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1.0;
        JScrollPane instructionsScrollPane = new JScrollPane(instructionsArea);
        panel.add(instructionsScrollPane, constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        constraints.weightx = 0.0;
        constraints.weighty = 0.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("Zutaten:"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        JScrollPane ingredientScrollPane = new JScrollPane(ingredientTable);
        ingredientScrollPane.setPreferredSize(new java.awt.Dimension(250, 140));
        panel.add(ingredientScrollPane, constraints);

        constraints.gridy++;
        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.NORTHEAST;
        JPanel ingredientButtonPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        ingredientButtonPanel.add(addIngredientButton);
        ingredientButtonPanel.add(editIngredientButton);
        ingredientButtonPanel.add(removeIngredientButton);
        panel.add(ingredientButtonPanel, constraints);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        panel.add(cancelButton);
        panel.add(saveButton);
        return panel;
    }

    /**
     * Opens the dialog, optionally pre-filling it with existing values, and returns
     * the user input when confirmed.
     */
    public Optional<FormData> showDialog(FormData initialData) {
        if (initialData != null) {
            nameField.setText(initialData.name());
            categoryField.setText(initialData.categoryId() != null ? initialData.categoryId().toString() : "");
            baseServingsSpinner.setValue(initialData.baseServings());
            instructionsArea.setText(initialData.instructions());
            ingredientTableModel.setEntries(initialData.ingredients());
        } else {
            nameField.setText("");
            categoryField.setText("");
            baseServingsSpinner.setValue(10);
            instructionsArea.setText("");
            ingredientTableModel.setEntries(java.util.List.of());
        }

        ingredientTable.clearSelection();
        updateIngredientButtons();

        confirmed = false;
        SwingUtilities.invokeLater(() -> nameField.requestFocusInWindow());
        setVisible(true);
        return confirmed ? Optional.of(buildFormData()) : Optional.empty();
    }

    private void onSave() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showValidationError("Bitte einen Namen für das Rezept angeben.");
            return;
        }

        String categoryText = categoryField.getText().trim();
        Long categoryId = null;
        if (!categoryText.isEmpty()) {
            try {
                categoryId = Long.parseLong(categoryText);
            } catch (NumberFormatException ex) {
                showValidationError("Kategorie muss eine Zahl sein.");
                return;
            }
        }

        int baseServings = ((Number) baseServingsSpinner.getValue()).intValue();
        if (baseServings <= 0) {
            showValidationError("Basisportionen müssen größer als 0 sein.");
            return;
        }

        confirmed = true;
        dispose();
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    private FormData buildFormData() {
        String name = nameField.getText().trim();
        String categoryText = categoryField.getText().trim();
        Long categoryId = categoryText.isEmpty() ? null : Long.parseLong(categoryText);
        int baseServings = ((Number) baseServingsSpinner.getValue()).intValue();
        String instructions = instructionsArea.getText().trim();
        return new FormData(name, categoryId, baseServings, instructions, ingredientTableModel.getEntries());
    }

    private void showValidationError(String message) {
        JOptionPane.showMessageDialog(this, Objects.requireNonNull(message), "Ungültige Eingabe", JOptionPane.WARNING_MESSAGE);
    }

    private void updateIngredientButtons() {
        boolean hasSelection = ingredientTable.getSelectedRow() >= 0;
        editIngredientButton.setEnabled(hasSelection);
        removeIngredientButton.setEnabled(hasSelection);
    }

    private void onAddIngredient() {
        showIngredientEditor(null).ifPresent(entry -> {
            ingredientTableModel.addEntry(entry);
            int index = ingredientTableModel.getRowCount() - 1;
            if (index >= 0) {
                ingredientTable.getSelectionModel().setSelectionInterval(index, index);
            }
            updateIngredientButtons();
        });
    }

    private void onEditIngredient() {
        int selectedRow = ingredientTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        int modelIndex = ingredientTable.convertRowIndexToModel(selectedRow);
        IngredientFormEntry currentEntry = ingredientTableModel.getEntryAt(modelIndex);
        showIngredientEditor(currentEntry).ifPresent(updated -> {
            ingredientTableModel.updateEntry(modelIndex, updated);
            ingredientTable.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
        });
    }

    private void onRemoveIngredient() {
        int selectedRow = ingredientTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        int modelIndex = ingredientTable.convertRowIndexToModel(selectedRow);
        ingredientTableModel.removeEntry(modelIndex);
        int newRowCount = ingredientTableModel.getRowCount();
        if (newRowCount > 0) {
            int newSelection = Math.min(modelIndex, newRowCount - 1);
            ingredientTable.getSelectionModel().setSelectionInterval(newSelection, newSelection);
        }
        updateIngredientButtons();
    }

    private Optional<IngredientFormEntry> showIngredientEditor(IngredientFormEntry initialEntry) {
        IngredientEditorPanel editorPanel = new IngredientEditorPanel();
        editorPanel.setValues(initialEntry);
        Long id = initialEntry != null ? initialEntry.id() : null;
        Long recipeId = initialEntry != null ? initialEntry.recipeId() : null;

        while (true) {
            int result = JOptionPane.showConfirmDialog(
                    this,
                    editorPanel,
                    initialEntry == null ? "Zutat hinzufügen" : "Zutat bearbeiten",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return Optional.empty();
            }
            Optional<String> validationError = editorPanel.validateInput();
            if (validationError.isPresent()) {
                showValidationError(validationError.get());
                continue;
            }
            return Optional.of(editorPanel.buildEntry(id, recipeId));
        }
    }

    private static final class IngredientTableModel extends AbstractTableModel {

        private final java.util.List<IngredientFormEntry> entries = new ArrayList<>();

        @Override
        public int getRowCount() {
            return entries.size();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case 0 -> "Name";
                case 1 -> "Menge";
                case 2 -> "Einheit";
                case 3 -> "Notizen";
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 1 -> Double.class;
                default -> String.class;
            };
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            IngredientFormEntry entry = entries.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> entry.name();
                case 1 -> entry.amountPerServing();
                case 2 -> entry.unit();
                case 3 -> entry.notes() != null ? entry.notes() : "";
                default -> null;
            };
        }

        public void setEntries(java.util.List<IngredientFormEntry> newEntries) {
            entries.clear();
            if (newEntries != null) {
                entries.addAll(newEntries);
            }
            fireTableDataChanged();
        }

        public java.util.List<IngredientFormEntry> getEntries() {
            return java.util.List.copyOf(entries);
        }

        public IngredientFormEntry getEntryAt(int index) {
            return entries.get(index);
        }

        public void addEntry(IngredientFormEntry entry) {
            entries.add(entry);
            int newIndex = entries.size() - 1;
            fireTableRowsInserted(newIndex, newIndex);
        }

        public void updateEntry(int index, IngredientFormEntry entry) {
            entries.set(index, entry);
            fireTableRowsUpdated(index, index);
        }

        public void removeEntry(int index) {
            entries.remove(index);
            fireTableRowsDeleted(index, index);
        }
    }

    private static final class IngredientEditorPanel extends JPanel {

        private final JTextField nameField;
        private final JSpinner amountSpinner;
        private final JTextField unitField;
        private final JTextField notesField;

        private IngredientEditorPanel() {
            super(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            constraints.anchor = GridBagConstraints.WEST;
            constraints.gridx = 0;
            constraints.gridy = 0;

            add(new JLabel("Name:"), constraints);

            constraints.gridx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.weightx = 1.0;
            nameField = new JTextField(20);
            add(nameField, constraints);

            constraints.gridx = 0;
            constraints.gridy++;
            constraints.weightx = 0.0;
            constraints.fill = GridBagConstraints.NONE;
            add(new JLabel("Menge pro Basisportion:"), constraints);

            constraints.gridx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            SpinnerNumberModel numberModel = new SpinnerNumberModel(1.0, 0.0, Double.MAX_VALUE, 0.1);
            amountSpinner = new JSpinner(numberModel);
            add(amountSpinner, constraints);

            constraints.gridx = 0;
            constraints.gridy++;
            constraints.fill = GridBagConstraints.NONE;
            add(new JLabel("Einheit:"), constraints);

            constraints.gridx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            unitField = new JTextField(15);
            add(unitField, constraints);

            constraints.gridx = 0;
            constraints.gridy++;
            constraints.fill = GridBagConstraints.NONE;
            add(new JLabel("Notizen:"), constraints);

            constraints.gridx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            notesField = new JTextField(20);
            add(notesField, constraints);
        }

        private void setValues(IngredientFormEntry entry) {
            if (entry == null) {
                nameField.setText("");
                amountSpinner.setValue(1.0);
                unitField.setText("");
                notesField.setText("");
            } else {
                nameField.setText(entry.name());
                amountSpinner.setValue(entry.amountPerServing());
                unitField.setText(entry.unit());
                notesField.setText(entry.notes() != null ? entry.notes() : "");
            }
        }

        private Optional<String> validateInput() {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                return Optional.of("Bitte einen Namen für die Zutat angeben.");
            }
            String unit = unitField.getText().trim();
            if (unit.isEmpty()) {
                return Optional.of("Bitte eine Einheit für die Zutat angeben.");
            }
            double amount = ((Number) amountSpinner.getValue()).doubleValue();
            if (amount <= 0.0) {
                return Optional.of("Die Menge muss größer als 0 sein.");
            }
            return Optional.empty();
        }

        private IngredientFormEntry buildEntry(Long id, Long recipeId) {
            String name = nameField.getText().trim();
            double amount = ((Number) amountSpinner.getValue()).doubleValue();
            String unit = unitField.getText().trim();
            String notes = notesField.getText().trim();
            if (notes.isEmpty()) {
                notes = null;
            }
            return new IngredientFormEntry(id, recipeId, name, unit, amount, notes);
        }
    }
}
