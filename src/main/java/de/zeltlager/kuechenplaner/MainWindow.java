package de.zeltlager.kuechenplaner;

import de.zeltlager.kuechenplaner.data.repository.sqlite.SqliteDatabase;
import de.zeltlager.kuechenplaner.logic.InventoryService;
import de.zeltlager.kuechenplaner.logic.MenuPlanService;
import de.zeltlager.kuechenplaner.logic.RecipeService;
import de.zeltlager.kuechenplaner.ui.UiTheme;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

/**
 * Encapsulates the main application window and its navigation. The layout
 * mirrors the mock-up with a vertical navigation bar on the left and the
 * feature panels presented on the right inside a card layout.
 */
public class MainWindow {

    private final JFrame frame;
    private final CardLayout contentLayout;
    private final JPanel contentPanel;

    private final MenuPlanPanel menuPlanPanel;
    private final InventoryPanel inventoryPanel;
    private final RecipePanel recipePanel;
    private final ShoppingListPanel shoppingListPanel;
    private final ImportExportPanel importExportPanel;
    private final SettingsPanel settingsPanel;

    private final SqliteDatabase database;

    public MainWindow(MenuPlanService menuPlanService,
            InventoryService inventoryService,
            RecipeService recipeService,
            SqliteDatabase database) {
        this.database = database;

        frame = new JFrame("Zeltlager Küchenplaner");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1280, 768));
        frame.getContentPane().setBackground(UiTheme.BACKGROUND);

        frame.setJMenuBar(createMenuBar());

        menuPlanPanel = new MenuPlanPanel(menuPlanService);
        inventoryPanel = new InventoryPanel(inventoryService);
        recipePanel = new RecipePanel(recipeService);
        shoppingListPanel = new ShoppingListPanel(menuPlanService, recipeService);
        importExportPanel = new ImportExportPanel(recipeService);
        settingsPanel = new SettingsPanel();

        menuPlanPanel.setMenuPlanUpdatedListener(shoppingListPanel::reloadData);
        recipePanel.setRecipesUpdatedListener(shoppingListPanel::reloadData);
        importExportPanel.setRecipesUpdatedListener(() -> {
            recipePanel.reloadData();
            shoppingListPanel.reloadData();
        });

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

        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        contentPanel.setOpaque(false);

        contentPanel.add(wrapContent(recipePanel), View.RECIPES.name());
        contentPanel.add(wrapContent(menuPlanPanel), View.PLANNER.name());
        contentPanel.add(wrapContent(shoppingListPanel), View.SHOPPING_LIST.name());
        contentPanel.add(wrapContent(importExportPanel), View.IMPORT_EXPORT.name());
        contentPanel.add(wrapContent(inventoryPanel), View.INVENTORY.name());
        contentPanel.add(wrapContent(settingsPanel), View.SETTINGS.name());

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(createNavigation(), BorderLayout.WEST);
        frame.getContentPane().add(contentPanel, BorderLayout.CENTER);
    }

    public void showWindow() {
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // initial data load
        SwingUtilities.invokeLater(() -> {
            showView(View.RECIPES);
            menuPlanPanel.reloadData();
            inventoryPanel.reloadData();
            recipePanel.reloadData();
            shoppingListPanel.reloadData();
        });
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("Datei");
        JMenuItem backupItem = new JMenuItem("Backup erstellen...");
        backupItem.addActionListener(this::showBackupDialog);
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

    private JPanel createNavigation() {
        JPanel navigation = new JPanel();
        navigation.setLayout(new javax.swing.BoxLayout(navigation, javax.swing.BoxLayout.Y_AXIS));
        navigation.setBackground(UiTheme.SURFACE_ALT);
        navigation.setBorder(new EmptyBorder(24, 20, 24, 20));
        navigation.setPreferredSize(new Dimension(240, 0));

        JLabel brand = new JLabel("Zeltlager Küchenplaner");
        brand.setFont(new Font("SansSerif", Font.BOLD, 18));
        brand.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        navigation.add(brand);
        navigation.add(Box.createVerticalStrut(24));

        ButtonGroup group = new ButtonGroup();
        navigation.add(createNavigationButton("Rezepte", View.RECIPES, group));
        navigation.add(Box.createVerticalStrut(8));
        navigation.add(createNavigationButton("Planer", View.PLANNER, group));
        navigation.add(Box.createVerticalStrut(8));
        navigation.add(createNavigationButton("Einkaufsliste", View.SHOPPING_LIST, group));
        navigation.add(Box.createVerticalStrut(8));
        navigation.add(createNavigationButton("Import/Export", View.IMPORT_EXPORT, group));
        navigation.add(Box.createVerticalStrut(8));
        navigation.add(createNavigationButton("Lagerbestand", View.INVENTORY, group));
        navigation.add(Box.createVerticalStrut(8));
        navigation.add(createNavigationButton("Einstellungen", View.SETTINGS, group));

        navigation.add(Box.createVerticalGlue());

        JLabel versionLabel = new JLabel("Version 0.2.0");
        versionLabel.setForeground(UiTheme.TEXT_MUTED);
        versionLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        navigation.add(versionLabel);

        return navigation;
    }

    private NavigationButton createNavigationButton(String text, View view, ButtonGroup group) {
        NavigationButton button = new NavigationButton(text);
        button.addActionListener(event -> showView(view));
        button.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        group.add(button);
        if (view == View.RECIPES) {
            button.setSelected(true);
        }
        return button;
    }

    private JPanel wrapContent(JPanel panel) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(24, 24, 24, 24));
        wrapper.add(panel, BorderLayout.CENTER);
        return wrapper;
    }

    private void showView(View view) {
        contentLayout.show(contentPanel, view.name());
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(frame,
                "Zeltlager Küchenplaner\nVersion 0.2.0\nDunkles UI-Layout inspiriert vom Mock-up.",
                "Über",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(frame, message, "Fehler", JOptionPane.ERROR_MESSAGE);
    }

    private void showBackupDialog(ActionEvent event) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Backup speichern");

        Path databaseFile = database.getDatabaseFile();
        String defaultBaseName = databaseFile != null && databaseFile.getFileName() != null
                ? databaseFile.getFileName().toString().replaceFirst("\\.db$", "")
                : "datenbank";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy-HHmm"));
        String suggestedName = defaultBaseName + "-backup-" + timestamp + ".db";
        fileChooser.setSelectedFile(new java.io.File(suggestedName));

        int userSelection = fileChooser.showSaveDialog(frame);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }

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

    private enum View {
        RECIPES,
        PLANNER,
        SHOPPING_LIST,
        IMPORT_EXPORT,
        INVENTORY,
        SETTINGS
    }

    private static final class NavigationButton extends javax.swing.JToggleButton {

        NavigationButton(String text) {
            super(text);
            setFocusPainted(false);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(new EmptyBorder(12, 16, 12, 16));
            setOpaque(true);
            setBackground(UiTheme.SURFACE_ALT);
            setForeground(UiTheme.TEXT_PRIMARY);
        }

        @Override
        public void setSelected(boolean selected) {
            super.setSelected(selected);
            if (selected) {
                setBackground(UiTheme.ACCENT);
                setForeground(UiTheme.TEXT_PRIMARY);
            } else {
                setBackground(UiTheme.SURFACE_ALT);
                setForeground(UiTheme.TEXT_PRIMARY);
            }
        }
    }
}

