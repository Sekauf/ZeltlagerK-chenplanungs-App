package de.zeltlager.kuechenplaner;

import de.zeltlager.kuechenplaner.data.model.RecipeWithIngredients;
import de.zeltlager.kuechenplaner.logic.RecipeService;
import de.zeltlager.kuechenplaner.ui.UiTheme;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.springframework.stereotype.Component;

/**
 * Dedicated panel that combines the import and export capabilities of the
 * {@link RecipeService}. The layout mirrors the mock-up by placing two cards
 * side by side: one for importing external data and one for exporting the
 * current dataset.
 */
@Component
public class ImportExportPanel extends JPanel {

    private final RecipeService recipeService;
    private final JComboBox<RecipeService.ImportFormat> importFormatBox;
    private final JComboBox<RecipeService.ExportFormat> exportFormatBox;
    private final javax.swing.JButton importButton;
    private final javax.swing.JButton exportButton;
    private final JLabel importStatusLabel;
    private final JLabel exportStatusLabel;
    private Runnable recipesUpdatedListener;

    public ImportExportPanel(RecipeService recipeService) {
        super(new BorderLayout());
        this.recipeService = Objects.requireNonNull(recipeService, "recipeService");

        setOpaque(false);
        setBorder(new EmptyBorder(24, 24, 24, 24));

        importFormatBox = new JComboBox<>(new DefaultComboBoxModel<>(RecipeService.ImportFormat.values()));
        exportFormatBox = new JComboBox<>(new DefaultComboBoxModel<>(RecipeService.ExportFormat.values()));
        importStatusLabel = new JLabel("Bereit für Import");
        importStatusLabel.setForeground(UiTheme.TEXT_MUTED);
        exportStatusLabel = new JLabel("Bereit für Export");
        exportStatusLabel.setForeground(UiTheme.TEXT_MUTED);

        importButton = UiTheme.createPrimaryButton("Import starten");
        importButton.addActionListener(event -> startImportFlow());

        exportButton = UiTheme.createSecondaryButton("Exportieren");
        exportButton.addActionListener(event -> startExportFlow());

        add(UiTheme.createHeader("Import/Export", null), BorderLayout.NORTH);

        JPanel cardsContainer = new JPanel(new java.awt.GridLayout(1, 2, 24, 0));
        cardsContainer.setOpaque(false);

        JPanel importCard = UiTheme.createCard(new BorderLayout(16, 16));
        importCard.add(buildSectionTitle("CSV / MealMaster importieren"), BorderLayout.NORTH);
        importCard.add(createImportDescription(), BorderLayout.CENTER);
        importCard.add(createImportControls(), BorderLayout.SOUTH);

        JPanel exportCard = UiTheme.createCard(new BorderLayout(16, 16));
        exportCard.add(buildSectionTitle("Rezepte exportieren"), BorderLayout.NORTH);
        exportCard.add(createExportDescription(), BorderLayout.CENTER);
        exportCard.add(createExportControls(), BorderLayout.SOUTH);

        cardsContainer.add(importCard);
        cardsContainer.add(exportCard);

        add(cardsContainer, BorderLayout.CENTER);
    }

    private JLabel buildSectionTitle(String title) {
        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        return label;
    }

    private JScrollPane createImportDescription() {
        JTextArea textArea = new JTextArea("Wähle eine CSV- oder MealMaster-Datei aus."
                + " Die Daten werden in die vorhandene Rezeptsammlung übernommen.");
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.setOpaque(false);
        textArea.setForeground(UiTheme.TEXT_MUTED);
        textArea.setBorder(null);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    private JPanel createImportControls() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        controls.setOpaque(false);
        controls.add(new JLabel("Format:"));
        controls.add(importFormatBox);
        controls.add(importButton);

        panel.add(controls, BorderLayout.NORTH);
        panel.add(importStatusLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JScrollPane createExportDescription() {
        JTextArea textArea = new JTextArea("Exportiere alle aktuell gespeicherten Rezepte."
                + " Wähle das gewünschte Zielformat aus und speichere die Datei auf deinem System.");
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.setOpaque(false);
        textArea.setForeground(UiTheme.TEXT_MUTED);
        textArea.setBorder(null);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    private JPanel createExportControls() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        controls.setOpaque(false);
        controls.add(new JLabel("Format:"));
        controls.add(exportFormatBox);
        controls.add(exportButton);

        panel.add(controls, BorderLayout.NORTH);
        panel.add(exportStatusLabel, BorderLayout.SOUTH);
        return panel;
    }

    private void startImportFlow() {
        RecipeService.ImportFormat format = (RecipeService.ImportFormat) importFormatBox.getSelectedItem();
        if (format == null) {
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Rezepte importieren");
        if (format == RecipeService.ImportFormat.CSV) {
            fileChooser.setFileFilter(new FileNameExtensionFilter("CSV-Dateien", "csv"));
        } else {
            fileChooser.setFileFilter(new FileNameExtensionFilter("MealMaster", "mmf", "txt"));
        }

        int result = fileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION || fileChooser.getSelectedFile() == null) {
            return;
        }

        Path importPath = fileChooser.getSelectedFile().toPath();
        if (!Files.isRegularFile(importPath)) {
            JOptionPane.showMessageDialog(this,
                    "Die ausgewählte Datei konnte nicht gelesen werden.",
                    "Import fehlgeschlagen",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        importButton.setEnabled(false);
        importStatusLabel.setText("Importiere...");

        new SwingWorker<List<RecipeWithIngredients>, Void>() {
            @Override
            protected List<RecipeWithIngredients> doInBackground() throws Exception {
                try (var reader = Files.newBufferedReader(importPath, StandardCharsets.UTF_8)) {
                    return recipeService.importRecipes(reader, format);
                }
            }

            @Override
            protected void done() {
                importButton.setEnabled(true);
                try {
                    List<RecipeWithIngredients> imported = get();
                    if (imported.isEmpty()) {
                        importStatusLabel.setText("Keine neuen Rezepte gefunden");
                    } else if (imported.size() == 1) {
                        importStatusLabel.setText("1 Rezept importiert");
                    } else {
                        importStatusLabel.setText(imported.size() + " Rezepte importiert");
                    }
                    JOptionPane.showMessageDialog(ImportExportPanel.this,
                            "Import abgeschlossen.",
                            "Fertig",
                            JOptionPane.INFORMATION_MESSAGE);
                    notifyRecipesUpdated();
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    importStatusLabel.setText("Import fehlgeschlagen");
                    JOptionPane.showMessageDialog(ImportExportPanel.this,
                            "Rezepte konnten nicht importiert werden: " + cause.getMessage(),
                            "Fehler",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void startExportFlow() {
        RecipeService.ExportFormat format = (RecipeService.ExportFormat) exportFormatBox.getSelectedItem();
        if (format == null) {
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Rezepte exportieren");
        String suggestedName = format == RecipeService.ExportFormat.CSV ? "rezepte.csv" : "rezepte.txt";
        fileChooser.setSelectedFile(new java.io.File(suggestedName));

        int result = fileChooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION || fileChooser.getSelectedFile() == null) {
            return;
        }

        Path exportPath = fileChooser.getSelectedFile().toPath();
        if (Files.exists(exportPath)) {
            int overwrite = JOptionPane.showConfirmDialog(this,
                    "Die Datei existiert bereits. Überschreiben?",
                    "Datei überschreiben",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (overwrite != JOptionPane.YES_OPTION) {
                return;
            }
        }

        exportButton.setEnabled(false);
        exportStatusLabel.setText("Exportiere...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try (BufferedWriter writer = Files.newBufferedWriter(exportPath, StandardCharsets.UTF_8)) {
                    recipeService.exportRecipes(writer, format);
                }
                return null;
            }

            @Override
            protected void done() {
                exportButton.setEnabled(true);
                try {
                    get();
                    exportStatusLabel.setText("Rezepte exportiert");
                    JOptionPane.showMessageDialog(ImportExportPanel.this,
                            "Rezepte wurden exportiert.",
                            "Export abgeschlossen",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    exportStatusLabel.setText("Export fehlgeschlagen");
                    JOptionPane.showMessageDialog(ImportExportPanel.this,
                            "Rezepte konnten nicht exportiert werden: " + cause.getMessage(),
                            "Fehler",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    public void setRecipesUpdatedListener(Runnable listener) {
        this.recipesUpdatedListener = listener;
    }

    private void notifyRecipesUpdated() {
        if (recipesUpdatedListener != null) {
            recipesUpdatedListener.run();
        }
    }
}

