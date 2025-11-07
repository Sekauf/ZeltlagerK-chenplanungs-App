package de.zeltlager.kuechenplaner.user;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import de.zeltlager.kuechenplaner.data.persistence.entity.UserEntity;
import de.zeltlager.kuechenplaner.data.persistence.repository.UserEntityRepository;

@Service
public class UserAccountService {

    private final UserEntityRepository userEntityRepository;
    private final UserContext userContext;

    public UserAccountService(UserEntityRepository userEntityRepository, UserContext userContext) {
        this.userEntityRepository = userEntityRepository;
        this.userContext = userContext;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureDefaultUser() {
        ensureUserExists(userContext.getCurrentUsername(), userContext.getCurrentUsername());
    }

    @Transactional
    public UserEntity ensureCurrentUserEntity() {
        String username = userContext.getCurrentUsername();
        return ensureUserExists(username, username);
    }

    @Transactional
    public UserEntity ensureUserExists(String username, String displayName) {
        String normalized = normalize(username);
        String sanitizedDisplayName = sanitizeDisplayName(displayName);
        return userEntityRepository.findByUsernameIgnoreCase(normalized)
                .map(existing -> updateDisplayNameIfNecessary(existing, sanitizedDisplayName))
                .orElseGet(() -> {
                    UserEntity entity = new UserEntity();
                    entity.setUsername(normalized);
                    entity.setDisplayName(sanitizedDisplayName);
                    return userEntityRepository.save(entity);
                });
    }

    @Transactional(readOnly = true)
    public List<UserEntity> getAllUsers() {
        return userEntityRepository.findAll(Sort.by(Sort.Direction.ASC, "username"));
    }

    @Transactional
    public UserEntity createUser(String username, String displayName) {
        String normalized = normalize(username);
        String sanitizedDisplayName = sanitizeDisplayName(displayName);
        return userEntityRepository.findByUsernameIgnoreCase(normalized)
                .map(existing -> updateDisplayNameIfNecessary(existing, sanitizedDisplayName))
                .orElseGet(() -> {
                    UserEntity entity = new UserEntity();
                    entity.setUsername(normalized);
                    entity.setDisplayName(sanitizedDisplayName);
                    return userEntityRepository.save(entity);
                });
    }

    @Transactional
    public void switchUser(String username) {
        String normalized = normalize(username);
        ensureUserExists(normalized, normalized);
        userContext.setCurrentUsername(normalized);
    }

    private String normalize(String username) {
        Objects.requireNonNull(username, "username");
        String trimmed = username.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private UserEntity updateDisplayNameIfNecessary(UserEntity entity, String displayName) {
        if (displayName != null && !displayName.isBlank()) {
            entity.setDisplayName(displayName);
        }
        return entity;
    }

    private String sanitizeDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }
        String trimmed = displayName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
