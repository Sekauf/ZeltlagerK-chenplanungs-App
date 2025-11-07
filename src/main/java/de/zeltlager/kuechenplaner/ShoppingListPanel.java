package de.zeltlager.kuechenplaner;

import de.zeltlager.kuechenplaner.data.model.MenuPlanEntry;
import de.zeltlager.kuechenplaner.data.model.RecipeWithIngredients;
import de.zeltlager.kuechenplaner.data.model.ShoppingListItem;
import de.zeltlager.kuechenplaner.logic.MenuPlanService;
import de.zeltlager.kuechenplaner.logic.RecipeService;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.print.PrinterException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Panel that renders a consolidated shopping list for the selected menu plan.
 */
public class ShoppingListPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final MenuPlanService menuPlanService;
    private final RecipeService recipeService;
    private final ShoppingListTableModel tableModel;
    private final JButton reloadButton;
    private final JLabel statusLabel;
    private final JTable table;
    private final JPanel infoContainer;
    private final JTextArea infoTextArea;
    private SwingWorker<ShoppingListData, Void> reloadWorker;
    private boolean reloadPending;
    private ShoppingListData lastData;

    public ShoppingListPanel(MenuPlanService menuPlanService, RecipeService recipeService) {
        super(new BorderLayout());
        this.menuPlanService = Objects.requireNonNull(menuPlanService, "menuPlanService");
        this.recipeService = Objects.requireNonNull(recipeService, "recipeService");
        this.tableModel = new ShoppingListTableModel();

        table = new JTable(tableModel);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Double.class, createAmountRenderer());
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        reloadButton = new JButton("Aktualisieren");
        reloadButton.addActionListener(event -> reloadData());
        topPanel.add(reloadButton);

        JButton printButton = new JButton("Drucken…");
        printButton.addActionListener(event -> printTable());
        topPanel.add(printButton);

        JButton exportButton = new JButton("Exportieren…");
        exportButton.addActionListener(event -> exportShoppingList());
        topPanel.add(exportButton);

        statusLabel = new JLabel(" ");
        topPanel.add(statusLabel);
        add(topPanel, BorderLayout.NORTH);

        infoTextArea = new JTextArea(3, 40);
        infoTextArea.setEditable(false);
        infoTextArea.setLineWrap(true);
        infoTextArea.setWrapStyleWord(true);
        infoTextArea.setOpaque(false);
        infoTextArea.setBorder(null);

        infoContainer = new JPanel(new BorderLayout());
        infoContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 8, 8, 8),
                BorderFactory.createTitledBorder("Hinweise")));
        infoContainer.add(infoTextArea, BorderLayout.CENTER);
        infoContainer.setVisible(false);
        add(infoContainer, BorderLayout.SOUTH);
    }

    public void reloadData() {
        if (reloadWorker != null) {
            reloadPending = true;
            statusLabel.setText("Aktualisierung ausstehend...");
            return;
        }
        startReload();
    }

    private void startReload() {
        reloadButton.setEnabled(false);
        statusLabel.setText("Aktualisiere...");
        reloadWorker = new SwingWorker<ShoppingListData, Void>() {
            @Override
            protected ShoppingListData doInBackground() {
                List<MenuPlanEntry> menuPlanEntries = menuPlanService.getMenuPlan();
                List<RecipeWithIngredients> recipes = recipeService.getAllRecipes();

                Map<String, RecipeWithIngredients> recipesByName = new LinkedHashMap<>();
                for (RecipeWithIngredients recipe : recipes) {
                    String normalizedName = normalizeName(recipe.getRecipe().getName());
                    recipesByName.putIfAbsent(normalizedName, recipe);
                }

                List<RecipeService.RecipeSelection> selections = new ArrayList<>();
                List<String> missingMeals = new ArrayList<>();
                for (MenuPlanEntry entry : menuPlanEntries) {
                    String normalizedMealName = normalizeName(entry.getMeal().getName());
                    RecipeWithIngredients matchingRecipe = recipesByName.get(normalizedMealName);
                    if (matchingRecipe == null || matchingRecipe.getRecipe().getId().isEmpty()) {
                        missingMeals.add(entry.getMeal().getName() + " (" + DATE_FORMATTER.format(entry.getDate()) + ")");
                        continue;
                    }
                    long recipeId = matchingRecipe.getRecipe().getId().orElseThrow();
                    selections.add(new RecipeService.RecipeSelection(recipeId, entry.getMeal().getServings()));
                }

                List<ShoppingListItem> shoppingItems = selections.isEmpty()
                        ? List.of()
                        : recipeService.generateShoppingList(selections);

                return new ShoppingListData(List.copyOf(shoppingItems), List.copyOf(missingMeals), menuPlanEntries.size());
            }

            @Override
            protected void done() {
                try {
                    if (isCancelled()) {
                        return;
                    }
                    ShoppingListData data = get();
                    tableModel.setItems(data.items());
                    lastData = data;
                    updateStatusLabel(data);
                    updateInfoPanel(data);
                } catch (Exception e) {
                    showError("Einkaufsliste konnte nicht geladen werden: " + e.getMessage());
                    statusLabel.setText("Fehler beim Laden");
                    tableModel.setItems(List.of());
                    lastData = null;
                    infoContainer.setVisible(false);
                } finally {
                    reloadButton.setEnabled(true);
                    reloadWorker = null;
                    if (reloadPending) {
                        reloadPending = false;
                        startReload();
                    }
                }
            }
        };
        reloadWorker.execute();
    }

    private void updateStatusLabel(ShoppingListData data) {
        if (data.totalMenuEntries() == 0) {
            statusLabel.setText("Keine Menüeinträge vorhanden");
        } else if (data.items().isEmpty()) {
            statusLabel.setText("Keine Einkaufsposten berechnet");
        } else {
            statusLabel.setText(data.items().size() + " Einkaufsposten");
        }
    }

    private void updateInfoPanel(ShoppingListData data) {
        if (data.missingMeals().isEmpty()) {
            infoTextArea.setText("");
            infoContainer.setVisible(false);
            return;
        }

        String message = "Für folgende Menüeinträge wurde kein passendes Rezept gefunden:\n"
                + data.missingMeals().stream()
                .map(value -> "• " + value)
                .collect(Collectors.joining("\n"));
        infoTextArea.setText(message);
        infoContainer.setVisible(true);
    }

    private void printTable() {
        try {
            if (!table.print(JTable.PrintMode.FIT_WIDTH, new MessageFormat("Einkaufsliste"), null)) {
                JOptionPane.showMessageDialog(this, "Druckvorgang wurde abgebrochen.", "Drucken", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (PrinterException e) {
            showError("Drucken nicht möglich: " + e.getMessage());
        }
    }

    private void exportShoppingList() {
        if (tableModel.getRowCount() == 0) {
            showError("Es sind keine Einkaufsposten zum Exportieren vorhanden.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Einkaufsliste exportieren");
        fileChooser.setSelectedFile(new File("Einkaufsliste.txt"));

        int result = fileChooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = ensureTxtExtension(fileChooser.getSelectedFile());
        if (selectedFile.exists()) {
            int overwrite = JOptionPane.showConfirmDialog(
                    this,
                    "Die Datei existiert bereits. Überschreiben?",
                    "Datei überschreiben",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (overwrite != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            writeShoppingListToFile(selectedFile.toPath());
            JOptionPane.showMessageDialog(this, "Einkaufsliste wurde exportiert.", "Export abgeschlossen", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            showError("Einkaufsliste konnte nicht exportiert werden: " + e.getMessage());
        }
    }

    private File ensureTxtExtension(File file) {
        if (file.getName().contains(".")) {
            return file;
        }
        return new File(file.getParentFile(), file.getName() + ".txt");
    }

    private void writeShoppingListToFile(Path path) throws IOException {
        NumberFormat numberFormat = createNumberFormat();
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("Einkaufsliste");
            writer.newLine();
            writer.newLine();

            if (lastData != null) {
                writer.write("Menüeinträge insgesamt: " + lastData.totalMenuEntries());
                writer.newLine();
                if (!lastData.missingMeals().isEmpty()) {
                    writer.write("Nicht zugeordnete Menüeinträge:");
                    writer.newLine();
                    for (String missing : lastData.missingMeals()) {
                        writer.write("  • " + missing);
                        writer.newLine();
                    }
                    writer.newLine();
                }
            }

            writeColumnHeaders(writer);
            for (int row = 0; row < table.getRowCount(); row++) {
                int modelRow = table.convertRowIndexToModel(row);
                ShoppingListItem item = tableModel.getItem(modelRow);
                String category = item.getCategory().orElse("-");
                String amount = numberFormat.format(item.getTotalAmount());
                String notes = item.getNotes().isEmpty() ? "" : String.join(", ", item.getNotes());
                writer.write(String.join("\t",
                        category,
                        item.getName(),
                        amount,
                        item.getUnit(),
                        notes));
                writer.newLine();
            }
        }
    }

    private void writeColumnHeaders(BufferedWriter writer) throws IOException {
        List<String> headers = new ArrayList<>();
        for (int column = 0; column < tableModel.getColumnCount(); column++) {
            headers.add(tableModel.getColumnName(column));
        }
        writer.write(String.join("\t", headers));
        writer.newLine();
        writer.write("--------------------------------------------------------------");
        writer.newLine();
    }

    private DefaultTableCellRenderer createAmountRenderer() {
        return new DefaultTableCellRenderer() {
            private final NumberFormat format = createNumberFormat();

            {
                setHorizontalAlignment(SwingConstants.RIGHT);
            }

            @Override
            public void setValue(Object value) {
                if (value instanceof Number number) {
                    setText(format.format(number.doubleValue()));
                } else {
                    super.setValue(value);
                }
            }
        };
    }

    private NumberFormat createNumberFormat() {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.GERMANY);
        numberFormat.setMaximumFractionDigits(2);
        numberFormat.setMinimumFractionDigits(0);
        return numberFormat;
    }

    private String normalizeName(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Fehler", JOptionPane.ERROR_MESSAGE);
    }

    private record ShoppingListData(List<ShoppingListItem> items, List<String> missingMeals, int totalMenuEntries) {
    }
}
