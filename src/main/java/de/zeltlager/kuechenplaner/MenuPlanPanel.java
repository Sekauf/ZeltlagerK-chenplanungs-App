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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;

/**
 * Panel that shows and edits menu plan entries backed by the database.
 */
public class MenuPlanPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final MenuPlanService menuPlanService;
    private final MenuPlanTableModel tableModel;
    private final JButton reloadButton;
    private final JButton addButton;
    private final JTextField dateField;
    private final JTextField mealField;
    private final JSpinner servingsSpinner;
    private final JLabel statusLabel;
    private Runnable menuPlanUpdatedListener;

    public MenuPlanPanel(MenuPlanService menuPlanService) {
        super(new BorderLayout());
        this.menuPlanService = menuPlanService;
        this.tableModel = new MenuPlanTableModel();

        JTable table = new JTable(tableModel);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        reloadButton = new JButton("Aktualisieren");
        reloadButton.addActionListener(event -> reloadData());
        topPanel.add(reloadButton);

        statusLabel = new JLabel(" ");
        topPanel.add(statusLabel);
        add(topPanel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        formPanel.add(new JLabel("Datum (YYYY-MM-DD):"));
        dateField = new JTextField(10);
        dateField.setText(DATE_FORMATTER.format(LocalDate.now()));
        formPanel.add(dateField);

        formPanel.add(new JLabel("Gericht:"));
        mealField = new JTextField(15);
        formPanel.add(mealField);

        formPanel.add(new JLabel("Portionen:"));
        servingsSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));
        formPanel.add(servingsSpinner);

        addButton = new JButton("Eintrag hinzufügen");
        addButton.addActionListener(event -> addEntry());
        formPanel.add(addButton);

        add(formPanel, BorderLayout.SOUTH);
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
                    notifyMenuPlanUpdated();
                } catch (Exception e) {
                    showError("Menüplan konnte nicht geladen werden: " + e.getMessage());
                    statusLabel.setText("Fehler beim Laden");
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
            showError("Bitte ein gültiges Datum im Format YYYY-MM-DD eingeben.");
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

    private void toggleFormEnabled(boolean enabled) {
        addButton.setEnabled(enabled);
        dateField.setEnabled(enabled);
        mealField.setEnabled(enabled);
        servingsSpinner.setEnabled(enabled);
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
