package de.zeltlager.kuechenplaner;

import javax.swing.SwingUtilities;

/**
 * Entry point for the kitchen planning application.
 */
public final class App {

    private App() {
        // utility class
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.showWindow();
        });
    }
}
