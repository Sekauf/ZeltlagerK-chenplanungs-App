package de.zeltlager.kuechenplaner;

import de.zeltlager.kuechenplaner.ui.UiTheme;
import de.zeltlager.kuechenplaner.user.UserAccountService;
import de.zeltlager.kuechenplaner.user.UserContext;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.Objects;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.springframework.stereotype.Component;

/**
 * Panel to manage multi-user capabilities and backup settings.
 */
@Component
public class SettingsPanel extends JPanel {

    private final UserAccountService userAccountService;
    private final UserContext userContext;
    private final JComboBox<String> userComboBox;
    private final JTextField newUserField;
    private final JTextField displayNameField;
    private final JLabel statusLabel;
    private Runnable usersReloadRequestedListener = () -> { };

    public SettingsPanel(UserAccountService userAccountService, UserContext userContext) {
        super(new BorderLayout());
        this.userAccountService = Objects.requireNonNull(userAccountService, "userAccountService");
        this.userContext = Objects.requireNonNull(userContext, "userContext");

        setOpaque(false);
        setBorder(new EmptyBorder(24, 24, 24, 24));

        add(UiTheme.createHeader("Einstellungen", null), BorderLayout.NORTH);

        JPanel card = UiTheme.createCard(new BorderLayout(16, 16));
        card.setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));

        JLabel usersHeadline = UiTheme.createSectionLabel("Benutzerverwaltung");
        content.add(usersHeadline);
        content.add(Box.createVerticalStrut(12));

        JPanel currentUserRow = new JPanel(new BorderLayout(12, 0));
        currentUserRow.setOpaque(false);
        JLabel currentUserLabel = new JLabel("Aktiver Benutzer:");
        currentUserRow.add(currentUserLabel, BorderLayout.WEST);
        userComboBox = new JComboBox<>();
        userComboBox.setPreferredSize(new Dimension(220, 32));
        currentUserRow.add(userComboBox, BorderLayout.CENTER);
        JButton switchButton = UiTheme.createPrimaryButton("Aktiv setzen");
        switchButton.addActionListener(event -> switchUser());
        currentUserRow.add(switchButton, BorderLayout.EAST);
        content.add(currentUserRow);
        content.add(Box.createVerticalStrut(24));

        JLabel newUserLabel = new JLabel("Neuen Benutzer anlegen:");
        newUserLabel.setAlignmentX(LEFT_ALIGNMENT);
        content.add(newUserLabel);
        content.add(Box.createVerticalStrut(8));

        JPanel createRow = new JPanel(new BorderLayout(12, 0));
        createRow.setOpaque(false);
        newUserField = new JTextField();
        newUserField.setToolTipText("Benutzername (z. B. team-a)");
        UiTheme.styleTextField(newUserField);
        createRow.add(newUserField, BorderLayout.CENTER);
        displayNameField = new JTextField();
        displayNameField.setToolTipText("Anzeigename");
        UiTheme.styleTextField(displayNameField);
        createRow.add(displayNameField, BorderLayout.EAST);
        displayNameField.setPreferredSize(new Dimension(200, 32));
        content.add(createRow);
        content.add(Box.createVerticalStrut(12));

        JButton createButton = UiTheme.createSecondaryButton("Benutzer anlegen");
        createButton.setAlignmentX(LEFT_ALIGNMENT);
        createButton.addActionListener(event -> createUser());
        content.add(createButton);
        content.add(Box.createVerticalStrut(16));

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(UiTheme.TEXT_MUTED);
        statusLabel.setAlignmentX(LEFT_ALIGNMENT);
        content.add(statusLabel);

        card.add(content, BorderLayout.NORTH);
        add(card, BorderLayout.CENTER);
    }

    public void refreshUsers() {
        List<String> users = userAccountService.getAllUsers().stream()
                .map(entity -> entity.getUsername())
                .toList();
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        for (String user : users) {
            model.addElement(user);
        }
        userComboBox.setModel(model);
        userComboBox.setSelectedItem(userContext.getCurrentUsername());
    }

    public void setUsersReloadRequestedListener(Runnable listener) {
        this.usersReloadRequestedListener = Objects.requireNonNull(listener, "listener");
    }

    private void switchUser() {
        String selected = (String) userComboBox.getSelectedItem();
        if (selected == null || selected.isBlank()) {
            setStatus("Bitte einen Benutzer auswählen.");
            return;
        }
        try {
            userAccountService.switchUser(selected);
            setStatus("Benutzer gewechselt: " + selected);
            notifyReloadRequested();
        } catch (IllegalArgumentException ex) {
            setStatus("Benutzerwechsel fehlgeschlagen: " + ex.getMessage());
        }
    }

    private void createUser() {
        String username = newUserField.getText().trim();
        String displayName = displayNameField.getText().trim();
        if (username.isEmpty()) {
            setStatus("Der Benutzername darf nicht leer sein.");
            return;
        }
        try {
            userAccountService.createUser(username, displayName.isEmpty() ? username : displayName);
            userAccountService.switchUser(username);
            newUserField.setText("");
            displayNameField.setText("");
            setStatus("Benutzer angelegt und aktiviert: " + username);
            notifyReloadRequested();
        } catch (IllegalArgumentException ex) {
            setStatus("Benutzer konnte nicht angelegt werden: " + ex.getMessage());
        }
    }

    private void notifyReloadRequested() {
        refreshUsers();
        usersReloadRequestedListener.run();
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

}
