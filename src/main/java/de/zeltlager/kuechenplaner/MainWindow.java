package de.zeltlager.kuechenplaner;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

/**
 * Encapsulates the main application window and its initial layout.
 */
public class MainWindow {

    private final JFrame frame;

    public MainWindow() {
        frame = new JFrame("Zeltlager Küchenplaner");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setPreferredSize(new Dimension(1024, 768));

        frame.setJMenuBar(createMenuBar());
        frame.add(createTabbedPane(), BorderLayout.CENTER);
    }

    public void showWindow() {
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
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
                "Zeltlager Küchenplaner\nVersion 0.1.0\nEntwickelt für eine einfache Küchenorganisation.",
                "Über",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private JTabbedPane createTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Rezepte", createRecipesPanel());
        tabbedPane.addTab("Einkaufsplanung", createShoppingPanel());
        return tabbedPane;
    }

    private JPanel createRecipesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new PlaceholderPanel("Rezepte verwalten"), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createShoppingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new PlaceholderPanel("Einkaufsplanung organisieren"), BorderLayout.CENTER);
        return panel;
    }
}
