package de.zeltlager.kuechenplaner;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

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

    public MainWindow(MenuPlanService menuPlanService,
            InventoryService inventoryService,
            RecipeService recipeService,
            AutoCloseable shutdownHook) {
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
                if (shutdownHook != null) {
                    try {
                        shutdownHook.close();
                    } catch (Exception ex) {
                        showErrorDialog("Datenbank konnte nicht geschlossen werden: " + ex.getMessage());
                    }
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
}
