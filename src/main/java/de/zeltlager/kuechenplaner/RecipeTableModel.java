package de.zeltlager.kuechenplaner;

import de.zeltlager.kuechenplaner.data.model.RecipeWithIngredients;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.table.AbstractTableModel;

/**
 * Table model that exposes a compact overview of all recipes.
 */
public class RecipeTableModel extends AbstractTableModel {

    private static final String[] COLUMN_NAMES = {"Name", "Basisportionen", "Zutaten"};

    private final List<RecipeWithIngredients> recipes = new ArrayList<>();
    private final List<RecipeWithIngredients> filteredRecipes = new ArrayList<>();
    private String filterText = "";

    public void setRecipes(List<RecipeWithIngredients> newRecipes) {
        recipes.clear();
        recipes.addAll(newRecipes);
        applyFilter();
    }

    public void setFilter(String newFilterText) {
        filterText = newFilterText == null ? "" : newFilterText.trim();
        applyFilter();
    }

    public RecipeWithIngredients getRecipeAt(int index) {
        return filteredRecipes.get(index);
    }

    public int getTotalRecipeCount() {
        return recipes.size();
    }

    @Override
    public int getRowCount() {
        return filteredRecipes.size();
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
        RecipeWithIngredients recipe = filteredRecipes.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> recipe.getRecipe().getName();
            case 1 -> recipe.getRecipe().getBaseServings();
            case 2 -> recipe.getIngredients().size();
            default -> "";
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 1, 2 -> Integer.class;
            default -> String.class;
        };
    }

    private void applyFilter() {
        filteredRecipes.clear();
        if (filterText.isBlank()) {
            filteredRecipes.addAll(recipes);
        } else {
            String normalizedFilter = filterText.toLowerCase(Locale.ROOT);
            for (RecipeWithIngredients recipe : recipes) {
                boolean nameMatches = recipe.getRecipe().getName().toLowerCase(Locale.ROOT).contains(normalizedFilter);
                boolean ingredientMatches = recipe.getIngredients().stream()
                        .anyMatch(ingredient -> ingredient.getName().toLowerCase(Locale.ROOT).contains(normalizedFilter));
                if (nameMatches || ingredientMatches) {
                    filteredRecipes.add(recipe);
                }
            }
        }
        fireTableDataChanged();
    }
}
