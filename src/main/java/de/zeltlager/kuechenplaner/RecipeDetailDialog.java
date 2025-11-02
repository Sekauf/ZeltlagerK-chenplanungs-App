package de.zeltlager.kuechenplaner;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

/**
 * Modal dialog that captures the details of a single recipe.
 */
public class RecipeDetailDialog extends JDialog {

    /**
     * Immutable record containing the user input of the dialog.
     */
    public record FormData(String name, Long categoryId, int baseServings, String instructions) { }

    private final JTextField nameField;
    private final JTextField categoryField;
    private final JSpinner baseServingsSpinner;
    private final JTextArea instructionsArea;
    private final JButton saveButton;
    private final JButton cancelButton;

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

        setLayout(new BorderLayout());
        add(createFormPanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        getRootPane().setDefaultButton(saveButton);
        pack();
        setLocationRelativeTo(owner);
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
        } else {
            nameField.setText("");
            categoryField.setText("");
            baseServingsSpinner.setValue(10);
            instructionsArea.setText("");
        }

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
        return new FormData(name, categoryId, baseServings, instructions);
    }

    private void showValidationError(String message) {
        JOptionPane.showMessageDialog(this, Objects.requireNonNull(message), "Ungültige Eingabe", JOptionPane.WARNING_MESSAGE);
    }
}
