package de.zeltlager.kuechenplaner;

import de.zeltlager.kuechenplaner.ui.UiTheme;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Placeholder panel that mimics the settings card shown in the mock-up. The
 * actual configuration options can be filled in later without touching the
 * navigation or layout code again.
 */
public class SettingsPanel extends JPanel {

    public SettingsPanel() {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(24, 24, 24, 24));

        add(UiTheme.createHeader("Einstellungen", null), BorderLayout.NORTH);

        JPanel card = UiTheme.createCard(new BorderLayout());
        JLabel placeholder = new JLabel("Weitere Einstellungen folgen in einer späteren Version.");
        placeholder.setForeground(UiTheme.TEXT_MUTED);
        card.add(placeholder, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);
    }
}

