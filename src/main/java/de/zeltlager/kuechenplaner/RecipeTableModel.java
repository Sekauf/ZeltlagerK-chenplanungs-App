package de.zeltlager.kuechenplaner;

import de.zeltlager.kuechenplaner.data.model.RecipeWithIngredients;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * Table model that exposes a compact overview of all recipes.
 */
public class RecipeTableModel extends AbstractTableModel {

    private static final String[] COLUMN_NAMES = {"Name", "Basisportionen", "Zutaten"};

    private final List<RecipeWithIngredients> recipes = new ArrayList<>();

    public void setRecipes(List<RecipeWithIngredients> newRecipes) {
        recipes.clear();
        recipes.addAll(newRecipes);
        fireTableDataChanged();
    }

    public RecipeWithIngredients getRecipeAt(int index) {
        return recipes.get(index);
    }

    @Override
    public int getRowCount() {
        return recipes.size();
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
        RecipeWithIngredients recipe = recipes.get(rowIndex);
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
}
