package de.zeltlager.kuechenplaner.user;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

@Component
public class UserContext {

    private final AtomicReference<String> currentUsername = new AtomicReference<>("camp-admin");
    private final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    public String getCurrentUsername() {
        return currentUsername.get();
    }

    public void setCurrentUsername(String username) {
        String sanitized = sanitize(username);
        String previous = currentUsername.getAndSet(sanitized);
        if (!previous.equals(sanitized)) {
            notifyListeners(sanitized);
        }
    }

    public void addListener(Consumer<String> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    private void notifyListeners(String username) {
        for (Consumer<String> listener : listeners) {
            listener.accept(username);
        }
    }

    private String sanitize(String username) {
        String value = Objects.requireNonNull(username, "username").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        return value;
    }
}
