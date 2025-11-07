package de.zeltlager.kuechenplaner;

import de.zeltlager.kuechenplaner.data.model.Meal;
import de.zeltlager.kuechenplaner.data.model.MenuPlanEntry;
import de.zeltlager.kuechenplaner.logic.MenuPlanService;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.ListSelectionModel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import de.zeltlager.kuechenplaner.ui.UiTheme;

/**
 * Panel that shows and edits menu plan entries backed by the database.
 */
public class MenuPlanPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final MenuPlanService menuPlanService;
    private final MenuPlanTableModel tableModel;
    private final JTable table;
    private final JButton reloadButton;
    private final JButton addButton;
    private final JButton deleteButton;
    private final JTextField dateField;
    private final JTextField mealField;
    private final JSpinner servingsSpinner;
    private final JLabel statusLabel;
    private Runnable menuPlanUpdatedListener;
    private boolean deleteInProgress;

    public MenuPlanPanel(MenuPlanService menuPlanService) {
        super(new BorderLayout(16, 16));
        this.menuPlanService = menuPlanService;
        this.tableModel = new MenuPlanTableModel();

        setOpaque(false);
        setBorder(new EmptyBorder(0, 0, 0, 0));

        table = new JTable(tableModel);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateDeleteButtonState();
            }
        });
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

        add(UiTheme.createHeader("Planer", headerActions), BorderLayout.NORTH);

        JPanel tableCard = UiTheme.createCard(new BorderLayout(12, 12));
        JPanel tableToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        tableToolbar.setOpaque(false);
        deleteButton = UiTheme.createSecondaryButton("Auswahl löschen");
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(event -> deleteSelectedEntry());
        tableToolbar.add(deleteButton);
        tableCard.add(tableToolbar, BorderLayout.NORTH);
        tableCard.add(tableScrollPane, BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);

        JPanel formCard = UiTheme.createCard(new FlowLayout(FlowLayout.LEFT, 12, 8));
        JLabel dateLabel = new JLabel("Datum (DD-MM-YYYY):");
        formCard.add(dateLabel);
        dateField = new JTextField(10);
        dateField.setText(DATE_FORMATTER.format(LocalDate.now()));
        UiTheme.styleTextField(dateField);
        formCard.add(dateField);

        JLabel mealLabel = new JLabel("Gericht:");
        formCard.add(mealLabel);
        mealField = new JTextField(15);
        UiTheme.styleTextField(mealField);
        formCard.add(mealField);

        JLabel servingsLabel = new JLabel("Portionen:");
        formCard.add(servingsLabel);
        servingsSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));
        JSpinner.DefaultEditor spinnerEditor = (JSpinner.DefaultEditor) servingsSpinner.getEditor();
        spinnerEditor.getTextField().setColumns(4);
        spinnerEditor.getTextField().setBackground(UiTheme.SURFACE_ALT);
        spinnerEditor.getTextField().setForeground(UiTheme.TEXT_PRIMARY);
        spinnerEditor.getTextField().setBorder(new EmptyBorder(8, 8, 8, 8));
        formCard.add(servingsSpinner);

        addButton = UiTheme.createPrimaryButton("Eintrag hinzufügen");
        addButton.addActionListener(event -> addEntry());
        formCard.add(addButton);

        add(formCard, BorderLayout.SOUTH);
    }

    public void reloadData() {
        reloadButton.setEnabled(false);
        statusLabel.setText("Aktualisiere...");
        new SwingWorker<List<MenuPlanEntry>, Void>() {
            @Override
            protected List<MenuPlanEntry> doInBackground() {
                return menuPlanService.getMenuPlan();
            }

            @Override
            protected void done() {
                try {
                    List<MenuPlanEntry> entries = get();
                    tableModel.setEntries(entries);
                    statusLabel.setText(entries.isEmpty() ? "Keine Einträge vorhanden" : entries.size() + " Einträge");
                    table.clearSelection();
                    deleteInProgress = false;
                    updateDeleteButtonState();
                    notifyMenuPlanUpdated();
                } catch (Exception e) {
                    showError("Menüplan konnte nicht geladen werden: " + e.getMessage());
                    statusLabel.setText("Fehler beim Laden");
                    deleteInProgress = false;
                    updateDeleteButtonState();
                } finally {
                    reloadButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void addEntry() {
        LocalDate date;
        try {
            date = LocalDate.parse(dateField.getText().trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            showError("Bitte ein gültiges Datum im Format DD-MM-YYYY eingeben.");
            return;
        }

        String mealName = mealField.getText().trim();
        if (mealName.isEmpty()) {
            showError("Bitte einen Gerichtsnamen angeben.");
            return;
        }

        int servings = (Integer) servingsSpinner.getValue();
        MenuPlanEntry entry = new MenuPlanEntry(date, new Meal(mealName, servings));

        toggleFormEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                menuPlanService.addMenuPlanEntry(entry);
                return null;
            }

            @Override
            protected void done() {
                toggleFormEnabled(true);
                try {
                    get();
                    mealField.setText("");
                    servingsSpinner.setValue(servings);
                    reloadData();
                } catch (Exception e) {
                    showError("Eintrag konnte nicht gespeichert werden: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void deleteSelectedEntry() {
        if (deleteInProgress) {
            return;
        }

        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            showError("Bitte einen Eintrag auswählen, der gelöscht werden soll.");
            return;
        }

        int modelRow = table.convertRowIndexToModel(viewRow);
        MenuPlanEntry entry = tableModel.getEntry(modelRow);

        int choice = JOptionPane.showConfirmDialog(this,
                "Soll der Eintrag für " + DATE_FORMATTER.format(entry.getDate()) + " ("
                        + entry.getMeal().getName() + ") wirklich gelöscht werden?",
                "Eintrag löschen",
                JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        deleteInProgress = true;
        statusLabel.setText("Lösche Eintrag...");
        updateDeleteButtonState();
        reloadButton.setEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                menuPlanService.deleteMenuPlanEntry(entry);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    table.clearSelection();
                    deleteInProgress = false;
                    updateDeleteButtonState();
                    reloadData();
                } catch (Exception e) {
                    deleteInProgress = false;
                    showError("Eintrag konnte nicht gelöscht werden: " + e.getMessage());
                    statusLabel.setText("Fehler beim Löschen");
                    reloadButton.setEnabled(true);
                    updateDeleteButtonState();
                }
            }
        }.execute();
    }

    private void toggleFormEnabled(boolean enabled) {
        addButton.setEnabled(enabled);
        dateField.setEnabled(enabled);
        mealField.setEnabled(enabled);
        servingsSpinner.setEnabled(enabled);
    }

    private void updateDeleteButtonState() {
        boolean hasSelection = table.getSelectedRow() >= 0;
        deleteButton.setEnabled(hasSelection && !deleteInProgress);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Fehler", JOptionPane.ERROR_MESSAGE);
    }

    public void setMenuPlanUpdatedListener(Runnable listener) {
        this.menuPlanUpdatedListener = listener;
    }

    private void notifyMenuPlanUpdated() {
        if (menuPlanUpdatedListener != null) {
            menuPlanUpdatedListener.run();
        }
    }
}
