package de.zeltlager.kuechenplaner;

import javax.swing.SwingUtilities;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import de.zeltlager.kuechenplaner.ui.UiTheme;

/**
 * Entry point for the kitchen planning application.
 */
@SpringBootApplication
public final class App {

    private App() {
        // utility class
    }

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");

        ConfigurableApplicationContext context = new SpringApplicationBuilder(App.class)
                .headless(false)
                .run(args);

        UiTheme.apply();

        SwingUtilities.invokeLater(() -> {
            try {
                MainWindow window = context.getBean(MainWindow.class);
                window.onWindowClosed(context::close);
                window.showWindow();
            } catch (Exception e) {
                System.err.println("Konnte das Hauptfenster nicht starten: " + e.getMessage());
                e.printStackTrace();
                context.close();
            }
        });
    }
}
