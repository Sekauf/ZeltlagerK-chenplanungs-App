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
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    private final JButton newButton;
    private final JLabel statusLabel;

    private final JTextField nameField;
    private final JTextField categoryField;
    private final JSpinner baseServingsSpinner;
    private final JSpinner targetServingsSpinner;
    private final JTextArea instructionsArea;
    private final DefaultListModel<String> ingredientListModel;
    private final JButton editButton;

    private RecipeWithIngredients selectedRecipe;
    private javax.swing.SwingWorker<RecipeWithIngredients, Void> loadRecipeWorker;
    private boolean suppressTargetServingsChange;

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

        newButton = new JButton("Neues Rezept…");
        newButton.addActionListener(event -> openCreateDialog());
        topPanel.add(newButton);

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
        targetServingsSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));
        targetServingsSpinner.addChangeListener(event -> {
            if (suppressTargetServingsChange) {
                return;
            }
            if (selectedRecipe != null) {
                int targetServings = ((Number) targetServingsSpinner.getValue()).intValue();
                updateIngredientListForServings(selectedRecipe, targetServings);
            }
        });
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
        constraints.weightx = 0.0;
        formPanel.add(new JLabel("Portionen (Ziel):"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        formPanel.add(targetServingsSpinner, constraints);

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
        newButton.setEnabled(false);
        statusLabel.setText("Aktualisiere...");
        updateDetailEnabled(false);
        cancelRecipeLoadWorker();
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
                    newButton.setEnabled(true);
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
        long recipeId = recipe.getRecipe().getId().orElseThrow();

        updateDetailEnabled(false);
        cancelRecipeLoadWorker();
        loadRecipeWorker = new SwingWorker<>() {
            @Override
            protected RecipeWithIngredients doInBackground() {
                return recipeService.getRecipe(recipeId)
                        .orElseThrow(() -> new IllegalStateException("Rezept nicht gefunden"));
            }

            @Override
            protected void done() {
                try {
                    if (isCancelled()) {
                        return;
                    }
                    RecipeWithIngredients loaded = get();
                    selectedRecipe = loaded;
                    populateDetailFields(loaded);
                    updateDetailEnabled(true);
                } catch (Exception e) {
                    showError("Rezept konnte nicht geladen werden: " + e.getMessage());
                    statusLabel.setText("Fehler beim Laden");
                    clearSelection();
                } finally {
                    loadRecipeWorker = null;
                }
            }
        };
        loadRecipeWorker.execute();
    }

    private void populateDetailFields(RecipeWithIngredients recipe) {
        Recipe baseRecipe = recipe.getRecipe();
        nameField.setText(baseRecipe.getName());
        categoryField.setText(baseRecipe.getCategoryId().map(Object::toString).orElse(""));
        baseServingsSpinner.setValue(baseRecipe.getBaseServings());
        instructionsArea.setText(baseRecipe.getInstructions());

        suppressTargetServingsChange = true;
        targetServingsSpinner.setValue(baseRecipe.getBaseServings());
        suppressTargetServingsChange = false;

        updateIngredientListForServings(recipe, baseRecipe.getBaseServings());
    }

    private void clearSelection() {
        selectedRecipe = null;
        nameField.setText("");
        categoryField.setText("");
        baseServingsSpinner.setValue(10);
        suppressTargetServingsChange = true;
        targetServingsSpinner.setValue(10);
        suppressTargetServingsChange = false;
        instructionsArea.setText("");
        ingredientListModel.clear();
        updateDetailEnabled(false);
        cancelRecipeLoadWorker();
    }

    private void updateDetailEnabled(boolean enabled) {
        nameField.setEnabled(enabled);
        categoryField.setEnabled(enabled);
        baseServingsSpinner.setEnabled(enabled);
        targetServingsSpinner.setEnabled(enabled);
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

    private void openCreateDialog() {
        RecipeDetailDialog dialog = new RecipeDetailDialog(javax.swing.SwingUtilities.getWindowAncestor(this));
        dialog.setTitle("Neues Rezept anlegen");
        dialog.showDialog(null).ifPresent(this::submitRecipeCreation);
    }

    private void submitRecipeUpdate(RecipeDetailDialog.FormData formData) {
        if (selectedRecipe == null) {
            return;
        }
        Recipe baseRecipe = selectedRecipe.getRecipe();
        long id = baseRecipe.getId().orElseThrow();
        List<Ingredient> ingredients = buildIngredientsForUpdate(formData, id);

        updateDetailEnabled(false);
        reloadButton.setEnabled(false);
        newButton.setEnabled(false);
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
                    newButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void submitRecipeCreation(RecipeDetailDialog.FormData formData) {
        List<Ingredient> ingredients = buildIngredientsForCreate(formData);

        updateDetailEnabled(false);
        reloadButton.setEnabled(false);
        newButton.setEnabled(false);
        statusLabel.setText("Speichere...");
        new SwingWorker<RecipeWithIngredients, Void>() {
            @Override
            protected RecipeWithIngredients doInBackground() {
                return recipeService.createRecipe(
                        formData.name(),
                        formData.categoryId(),
                        formData.baseServings(),
                        formData.instructions(),
                        ingredients);
            }

            @Override
            protected void done() {
                try {
                    RecipeWithIngredients created = get();
                    selectedRecipe = created;
                    statusLabel.setText("Rezept gespeichert");
                    reloadData();
                } catch (Exception e) {
                    showError("Rezept konnte nicht gespeichert werden: " + e.getMessage());
                    statusLabel.setText("Fehler beim Speichern");
                    reloadButton.setEnabled(true);
                    newButton.setEnabled(true);
                    if (selectedRecipe != null) {
                        populateDetailFields(selectedRecipe);
                        updateDetailEnabled(true);
                    } else {
                        clearSelection();
                    }
                }
            }
        }.execute();
    }

    private List<Ingredient> buildIngredientsForCreate(RecipeDetailDialog.FormData formData) {
        List<Ingredient> ingredients = new ArrayList<>();
        for (RecipeDetailDialog.IngredientFormEntry entry : formData.ingredients()) {
            ingredients.add(new Ingredient(
                    entry.id(),
                    null,
                    entry.name(),
                    entry.unit(),
                    entry.amountPerServing(),
                    entry.notes()));
        }
        return ingredients;
    }

    private List<Ingredient> buildIngredientsForUpdate(RecipeDetailDialog.FormData formData, long recipeId) {
        List<Ingredient> ingredients = new ArrayList<>();
        for (RecipeDetailDialog.IngredientFormEntry entry : formData.ingredients()) {
            Long resolvedRecipeId = entry.recipeId() != null ? entry.recipeId() : recipeId;
            ingredients.add(new Ingredient(
                    entry.id(),
                    resolvedRecipeId,
                    entry.name(),
                    entry.unit(),
                    entry.amountPerServing(),
                    entry.notes()));
        }
        return ingredients;
    }

    private void cancelRecipeLoadWorker() {
        if (loadRecipeWorker != null) {
            loadRecipeWorker.cancel(true);
            loadRecipeWorker = null;
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Fehler", JOptionPane.ERROR_MESSAGE);
    }

    private void updateIngredientListForServings(RecipeWithIngredients recipe, int targetServings) {
        ingredientListModel.clear();
        if (recipe == null) {
            return;
        }

        int baseServings = recipe.getRecipe().getBaseServings();
        if (baseServings <= 0) {
            baseServings = 1;
        }
        if (targetServings <= 0) {
            targetServings = 1;
        }

        double scalingFactor = (double) targetServings / baseServings;
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.GERMANY);
        numberFormat.setMaximumFractionDigits(2);
        numberFormat.setMinimumFractionDigits(0);
        numberFormat.setRoundingMode(RoundingMode.HALF_UP);

        for (Ingredient ingredient : recipe.getIngredients()) {
            double baseAmount = ingredient.getAmountPerServing() * baseServings;
            double scaledAmount = baseAmount * scalingFactor;

            StringBuilder builder = new StringBuilder();
            builder.append(ingredient.getName());
            builder.append(" - ");
            builder.append(numberFormat.format(scaledAmount));
            builder.append(" ");
            builder.append(ingredient.getUnit());
            ingredient.getNotes().ifPresent(notes -> builder.append(" (").append(notes).append(")"));
            ingredientListModel.addElement(builder.toString());
        }
    }
}
