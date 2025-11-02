package de.zeltlager.kuechenplaner;

import de.zeltlager.kuechenplaner.data.model.Ingredient;
import de.zeltlager.kuechenplaner.data.model.Recipe;
import de.zeltlager.kuechenplaner.data.model.RecipeWithIngredients;
import de.zeltlager.kuechenplaner.logic.RecipeService;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;

/**
 * Panel that presents the list of recipes and allows inspecting or editing their details.
 */
public class RecipePanel extends JPanel {

    private final RecipeService recipeService;
    private final RecipeTableModel tableModel;
    private final JTable recipeTable;
    private final JButton reloadButton;
    private final JLabel statusLabel;

    private final JTextField nameField;
    private final JTextField categoryField;
    private final JSpinner baseServingsSpinner;
    private final JTextArea instructionsArea;
    private final DefaultListModel<String> ingredientListModel;
    private final JButton editButton;

    private RecipeWithIngredients selectedRecipe;

    public RecipePanel(RecipeService recipeService) {
        super(new BorderLayout());
        this.recipeService = Objects.requireNonNull(recipeService, "recipeService");
        this.tableModel = new RecipeTableModel();

        recipeTable = new JTable(tableModel);
        recipeTable.setAutoCreateRowSorter(true);
        recipeTable.setFillsViewportHeight(true);
        recipeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recipeTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateSelectionFromTable();
            }
        });

        JScrollPane tableScrollPane = new JScrollPane(recipeTable);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        reloadButton = new JButton("Aktualisieren");
        reloadButton.addActionListener(event -> reloadData());
        topPanel.add(reloadButton);

        statusLabel = new JLabel(" ");
        topPanel.add(statusLabel);
        add(topPanel, BorderLayout.NORTH);

        JPanel detailPanel = createDetailPanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScrollPane, detailPanel);
        splitPane.setResizeWeight(0.4);
        add(splitPane, BorderLayout.CENTER);

        nameField = new JTextField();
        nameField.setEditable(false);

        categoryField = new JTextField();
        categoryField.setEditable(false);

        baseServingsSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));
        JSpinner.DefaultEditor spinnerEditor = (JSpinner.DefaultEditor) baseServingsSpinner.getEditor();
        spinnerEditor.getTextField().setEditable(false);
        spinnerEditor.getTextField().setFocusable(false);
        instructionsArea = new JTextArea(8, 30);
        instructionsArea.setLineWrap(true);
        instructionsArea.setWrapStyleWord(true);
        instructionsArea.setEditable(false);
        ingredientListModel = new DefaultListModel<>();
        editButton = new JButton("Bearbeiten…");
        editButton.addActionListener(event -> openDetailDialog());

        // finalize detail panel wiring
        initializeDetailPanel(detailPanel);
        updateDetailEnabled(false);
    }

    private JPanel createDetailPanel() {
        JPanel detailPanel = new JPanel(new BorderLayout());
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        detailPanel.add(formPanel, BorderLayout.CENTER);
        return detailPanel;
    }

    private void initializeDetailPanel(JPanel detailPanel) {
        JPanel formPanel = (JPanel) detailPanel.getComponent(0);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 0;

        formPanel.add(new JLabel("Name:"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        formPanel.add(nameField, constraints);

        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("Kategorie (ID):"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        formPanel.add(categoryField, constraints);

        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("Basisportionen:"), constraints);

        constraints.gridx = 1;
        formPanel.add(baseServingsSpinner, constraints);

        constraints.gridx = 0;
        constraints.gridy++;
        formPanel.add(new JLabel("Anleitung:"), constraints);

        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1.0;
        JScrollPane instructionsScrollPane = new JScrollPane(instructionsArea);
        formPanel.add(instructionsScrollPane, constraints);

        constraints.gridx = 0;
        constraints.gridy++;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weighty = 0.0;
        formPanel.add(new JLabel("Zutaten:"), constraints);

        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.BOTH;
        JList<String> ingredientList = new JList<>(ingredientListModel);
        JScrollPane ingredientScrollPane = new JScrollPane(ingredientList);
        ingredientScrollPane.setPreferredSize(new java.awt.Dimension(200, 120));
        formPanel.add(ingredientScrollPane, constraints);

        constraints.gridx = 1;
        constraints.gridy++;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.NORTHEAST;
        formPanel.add(editButton, constraints);
    }

    public void reloadData() {
        reloadButton.setEnabled(false);
        statusLabel.setText("Aktualisiere...");
        updateDetailEnabled(false);
        Long selectedId = selectedRecipe != null ? selectedRecipe.getRecipe().getId().orElse(null) : null;
        new SwingWorker<List<RecipeWithIngredients>, Void>() {
            @Override
            protected List<RecipeWithIngredients> doInBackground() {
                return recipeService.getAllRecipes();
            }

            @Override
            protected void done() {
                try {
                    List<RecipeWithIngredients> recipes = get();
                    tableModel.setRecipes(recipes);
                    statusLabel.setText(recipes.isEmpty() ? "Keine Rezepte vorhanden" : recipes.size() + " Rezepte");
                    restoreSelection(selectedId);
                } catch (Exception e) {
                    showError("Rezepte konnten nicht geladen werden: " + e.getMessage());
                    statusLabel.setText("Fehler beim Laden");
                    tableModel.setRecipes(List.of());
                    clearSelection();
                } finally {
                    reloadButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void restoreSelection(Long selectedId) {
        if (tableModel.getRowCount() == 0) {
            clearSelection();
            return;
        }
        if (selectedId != null) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                Recipe recipe = tableModel.getRecipeAt(i).getRecipe();
                if (recipe.getId().isPresent() && Objects.equals(recipe.getId().get(), selectedId)) {
                    int viewIndex = recipeTable.convertRowIndexToView(i);
                    recipeTable.getSelectionModel().setSelectionInterval(viewIndex, viewIndex);
                    return;
                }
            }
        }
        recipeTable.getSelectionModel().setSelectionInterval(0, 0);
    }

    private void updateSelectionFromTable() {
        int viewIndex = recipeTable.getSelectedRow();
        if (viewIndex < 0) {
            clearSelection();
            return;
        }
        int modelIndex = recipeTable.convertRowIndexToModel(viewIndex);
        RecipeWithIngredients recipe = tableModel.getRecipeAt(modelIndex);
        selectedRecipe = recipe;
        populateDetailFields(recipe);
        updateDetailEnabled(true);
    }

    private void populateDetailFields(RecipeWithIngredients recipe) {
        Recipe baseRecipe = recipe.getRecipe();
        nameField.setText(baseRecipe.getName());
        categoryField.setText(baseRecipe.getCategoryId().map(Object::toString).orElse(""));
        baseServingsSpinner.setValue(baseRecipe.getBaseServings());
        instructionsArea.setText(baseRecipe.getInstructions());

        ingredientListModel.clear();
        for (Ingredient ingredient : recipe.getIngredients()) {
            StringBuilder builder = new StringBuilder();
            builder.append(ingredient.getName());
            builder.append(" - ");
            builder.append(ingredient.getAmountPerServing());
            builder.append(" ");
            builder.append(ingredient.getUnit());
            ingredient.getNotes().ifPresent(notes -> builder.append(" (").append(notes).append(")"));
            ingredientListModel.addElement(builder.toString());
        }
    }

    private void clearSelection() {
        selectedRecipe = null;
        nameField.setText("");
        categoryField.setText("");
        baseServingsSpinner.setValue(10);
        instructionsArea.setText("");
        ingredientListModel.clear();
        updateDetailEnabled(false);
    }

    private void updateDetailEnabled(boolean enabled) {
        nameField.setEnabled(enabled);
        categoryField.setEnabled(enabled);
        baseServingsSpinner.setEnabled(enabled);
        instructionsArea.setEnabled(enabled);
        editButton.setEnabled(enabled);
    }

    private void openDetailDialog() {
        if (selectedRecipe == null) {
            return;
        }
        Recipe baseRecipe = selectedRecipe.getRecipe();
        RecipeDetailDialog dialog = new RecipeDetailDialog(javax.swing.SwingUtilities.getWindowAncestor(this));
        List<RecipeDetailDialog.IngredientFormEntry> ingredientEntries = new ArrayList<>();
        for (Ingredient ingredient : selectedRecipe.getIngredients()) {
            ingredientEntries.add(new RecipeDetailDialog.IngredientFormEntry(
                    ingredient.getId().orElse(null),
                    ingredient.getRecipeId().orElse(null),
                    ingredient.getName(),
                    ingredient.getUnit(),
                    ingredient.getAmountPerServing(),
                    ingredient.getNotes().orElse(null)));
        }
        RecipeDetailDialog.FormData initialData = new RecipeDetailDialog.FormData(
                baseRecipe.getName(),
                baseRecipe.getCategoryId().orElse(null),
                baseRecipe.getBaseServings(),
                baseRecipe.getInstructions(),
                ingredientEntries);
        dialog.showDialog(initialData).ifPresent(this::submitRecipeUpdate);
    }

    private void submitRecipeUpdate(RecipeDetailDialog.FormData formData) {
        if (selectedRecipe == null) {
            return;
        }
        Recipe baseRecipe = selectedRecipe.getRecipe();
        long id = baseRecipe.getId().orElseThrow();
        List<Ingredient> ingredients = new ArrayList<>();
        for (RecipeDetailDialog.IngredientFormEntry entry : formData.ingredients()) {
            ingredients.add(new Ingredient(
                    entry.id(),
                    entry.recipeId(),
                    entry.name(),
                    entry.unit(),
                    entry.amountPerServing(),
                    entry.notes()));
        }

        updateDetailEnabled(false);
        reloadButton.setEnabled(false);
        statusLabel.setText("Speichere...");
        new SwingWorker<RecipeWithIngredients, Void>() {
            @Override
            protected RecipeWithIngredients doInBackground() {
                return recipeService.updateRecipe(
                        id,
                        formData.name(),
                        formData.categoryId(),
                        formData.baseServings(),
                        formData.instructions(),
                        ingredients);
            }

            @Override
            protected void done() {
                try {
                    RecipeWithIngredients updated = get();
                    selectedRecipe = updated;
                    populateDetailFields(updated);
                    statusLabel.setText("Änderungen gespeichert");
                    reloadData();
                } catch (Exception e) {
                    showError("Rezept konnte nicht gespeichert werden: " + e.getMessage());
                    statusLabel.setText("Fehler beim Speichern");
                    updateDetailEnabled(true);
                    reloadButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Fehler", JOptionPane.ERROR_MESSAGE);
    }
}
