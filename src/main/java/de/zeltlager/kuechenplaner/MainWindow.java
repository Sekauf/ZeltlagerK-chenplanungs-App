package de.zeltlager.kuechenplaner;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

import de.zeltlager.kuechenplaner.data.repository.sqlite.SqliteDatabase;
import de.zeltlager.kuechenplaner.logic.InventoryService;
import de.zeltlager.kuechenplaner.logic.MenuPlanService;
import de.zeltlager.kuechenplaner.logic.RecipeService;

/**
 * Encapsulates the main application window and its initial layout.
 */
public class MainWindow {

    private final JFrame frame;
    private final MenuPlanPanel menuPlanPanel;
    private final InventoryPanel inventoryPanel;
    private final RecipePanel recipePanel;
    private final ShoppingListPanel shoppingListPanel;
    private final SqliteDatabase database;

    public MainWindow(MenuPlanService menuPlanService,
            InventoryService inventoryService,
            RecipeService recipeService,
            SqliteDatabase database) {
        this.database = Objects.requireNonNull(database, "database");

        frame = new JFrame("Zeltlager Küchenplaner");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setPreferredSize(new Dimension(1024, 768));

        frame.setJMenuBar(createMenuBar());

        menuPlanPanel = new MenuPlanPanel(menuPlanService);
        inventoryPanel = new InventoryPanel(inventoryService);
        recipePanel = new RecipePanel(recipeService);
        shoppingListPanel = new ShoppingListPanel(menuPlanService, recipeService);

        menuPlanPanel.setMenuPlanUpdatedListener(shoppingListPanel::reloadData);
        recipePanel.setRecipesUpdatedListener(shoppingListPanel::reloadData);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                try {
                    MainWindow.this.database.close();
                } catch (Exception ex) {
                    showErrorDialog("Datenbank konnte nicht geschlossen werden: " + ex.getMessage());
                }
            }
        });

        frame.add(createTabbedPane(), BorderLayout.CENTER);
    }

    public void showWindow() {
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        menuPlanPanel.reloadData();
        inventoryPanel.reloadData();
        recipePanel.reloadData();
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("Datei");
        JMenuItem backupItem = new JMenuItem("Backup erstellen...");
        backupItem.addActionListener(event -> showBackupDialog());
        fileMenu.add(backupItem);
        JMenuItem exitItem = new JMenuItem("Beenden");
        exitItem.addActionListener(event -> frame.dispose());
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        JMenu helpMenu = new JMenu("Hilfe");
        JMenuItem aboutItem = new JMenuItem("Info");
        aboutItem.addActionListener(event -> showAboutDialog());
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(frame,
                "Zeltlager Küchenplaner\nVersion 0.1.0\nAnbindung an aktuelle SQLite-Datenbank.",
                "Über",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private JTabbedPane createTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Menüplan", menuPlanPanel);
        tabbedPane.addTab("Einkaufsliste", shoppingListPanel);
        tabbedPane.addTab("Lagerbestand", inventoryPanel);
        tabbedPane.addTab("Rezepte", recipePanel);
        return tabbedPane;
    }

    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(frame, message, "Fehler", JOptionPane.ERROR_MESSAGE);
    }

    private void showBackupDialog() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Backup speichern");

        Path databaseFile = database.getDatabaseFile();
        String defaultBaseName = databaseFile != null && databaseFile.getFileName() != null
                ? databaseFile.getFileName().toString().replaceFirst("\\.db$", "")
                : "datenbank";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"));
        String suggestedName = defaultBaseName + "-backup-" + timestamp + ".db";
        fileChooser.setSelectedFile(new File(suggestedName));

        int userSelection = fileChooser.showSaveDialog(frame);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            Path targetPath = fileChooser.getSelectedFile().toPath();
            try {
                Path writtenFile = database.backupTo(targetPath);
                JOptionPane.showMessageDialog(frame,
                        "Backup erfolgreich erstellt:\n" + writtenFile,
                        "Backup abgeschlossen",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IllegalStateException ex) {
                showErrorDialog("Backup fehlgeschlagen: " + ex.getMessage());
            }
        }
    }
}
