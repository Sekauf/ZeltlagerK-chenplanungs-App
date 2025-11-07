package de.zeltlager.kuechenplaner.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.LayoutManager;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

/**
 * Central place that contains the colour palette and helper utilities for the
 * dark theme of the Swing application. The goal is not to introduce a new look
 * and feel, but to provide a consistent styling layer across the individual
 * panels so that the UI resembles the provided mock-ups.
 */
public final class UiTheme {

    public static final Color BACKGROUND = new Color(15, 23, 42);
    public static final Color SURFACE = new Color(30, 41, 59);
    public static final Color SURFACE_ALT = new Color(22, 33, 55);
    public static final Color ACCENT = new Color(59, 130, 246);
    public static final Color ACCENT_DARK = new Color(37, 99, 235);
    public static final Color TEXT_PRIMARY = new Color(226, 232, 240);
    public static final Color TEXT_MUTED = new Color(148, 163, 184);
    public static final Color BORDER = new Color(51, 65, 85);

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 20);
    private static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 14);

    private UiTheme() {
        // utility
    }

    /**
     * Applies a couple of sensible defaults to the Swing {@link UIManager} so
     * that newly created components pick up the base palette.
     */
    public static void apply() {
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("OptionPane.foreground", TEXT_PRIMARY);
        UIManager.put("Label.foreground", TEXT_PRIMARY);
        UIManager.put("ScrollPane.background", BACKGROUND);
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.foreground", TEXT_PRIMARY);
        UIManager.put("Table.selectionBackground", ACCENT_DARK);
        UIManager.put("Table.selectionForeground", TEXT_PRIMARY);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("control", BACKGROUND);
        UIManager.put("text", TEXT_PRIMARY);
    }

    public static JPanel createHeader(String title, JPanel trailingPanel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(0, 0, 16, 0));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(TITLE_FONT);
        panel.add(titleLabel, BorderLayout.WEST);

        if (trailingPanel != null) {
            trailingPanel.setOpaque(false);
            panel.add(trailingPanel, BorderLayout.EAST);
        }

        return panel;
    }

    public static JPanel createCard(LayoutManager layout) {
        JPanel card = new JPanel(layout);
        card.setOpaque(true);
        card.setBackground(SURFACE);
        card.setBorder(new EmptyBorder(16, 16, 16, 16));
        return card;
    }

    public static JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        styleButton(button, ACCENT, TEXT_PRIMARY);
        return button;
    }

    public static JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        styleButton(button, SURFACE_ALT, TEXT_PRIMARY);
        return button;
    }

    private static void styleButton(JButton button, Color background, Color foreground) {
        button.setFocusPainted(false);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFont(BUTTON_FONT);
        button.setBorder(new EmptyBorder(10, 18, 10, 18));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
    }

    public static void styleTable(JTable table) {
        table.setBackground(SURFACE);
        table.setForeground(TEXT_PRIMARY);
        table.setGridColor(BORDER);
        table.setRowHeight(28);
        table.setSelectionBackground(ACCENT_DARK);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setShowGrid(false);
        table.setIntercellSpacing(new java.awt.Dimension(0, 0));
        if (table.getTableHeader() != null) {
            table.getTableHeader().setBackground(SURFACE_ALT);
            table.getTableHeader().setForeground(TEXT_PRIMARY);
            table.getTableHeader().setReorderingAllowed(false);
            table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        }
    }

    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(SURFACE);
        scrollPane.setOpaque(false);
    }

    public static void styleTextField(JTextField textField) {
        textField.setBackground(SURFACE_ALT);
        textField.setForeground(TEXT_PRIMARY);
        textField.setBorder(new EmptyBorder(8, 12, 8, 12));
        textField.setCaretColor(TEXT_PRIMARY);
    }

    public static void styleTextArea(JTextArea textArea) {
        textArea.setBackground(SURFACE_ALT);
        textArea.setForeground(TEXT_PRIMARY);
        textArea.setCaretColor(TEXT_PRIMARY);
        textArea.setBorder(new EmptyBorder(12, 12, 12, 12));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
    }
}

