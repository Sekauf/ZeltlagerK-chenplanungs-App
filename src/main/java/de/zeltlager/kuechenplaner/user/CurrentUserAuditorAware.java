package de.zeltlager.kuechenplaner.user;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserAuditorAware implements AuditorAware<String> {

    private final UserContext userContext;

    public CurrentUserAuditorAware(UserContext userContext) {
        this.userContext = userContext;
    }

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(userContext.getCurrentUsername());
    }
}
