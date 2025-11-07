package de.zeltlager.kuechenplaner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import de.zeltlager.kuechenplaner.user.CurrentUserAuditorAware;

@Configuration
@EnableJpaAuditing
public class PersistenceConfig {

    @Bean
    public AuditorAware<String> auditorAware(CurrentUserAuditorAware currentUserAuditorAware) {
        return currentUserAuditorAware;
    }
}
