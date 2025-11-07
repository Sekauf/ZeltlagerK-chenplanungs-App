package de.zeltlager.kuechenplaner.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentUserAuditorAwareTest {

    @Test
    void returnsUsernameFromContext() {
        UserContext context = new UserContext();
        context.setCurrentUsername("alice");
        CurrentUserAuditorAware auditorAware = new CurrentUserAuditorAware(context);

        assertThat(auditorAware.getCurrentAuditor()).contains("alice");
    }

    @Test
    void returnsEmptyWhenUsernameNull() {
        UserContext context = new UserContext() {
            @Override
            public String getCurrentUsername() {
                return null;
            }
        };
        CurrentUserAuditorAware auditorAware = new CurrentUserAuditorAware(context);

        assertThat(auditorAware.getCurrentAuditor()).isEmpty();
    }
}
