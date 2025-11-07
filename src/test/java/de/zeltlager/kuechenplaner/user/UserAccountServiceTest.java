package de.zeltlager.kuechenplaner.user;

import de.zeltlager.kuechenplaner.data.persistence.entity.UserEntity;
import de.zeltlager.kuechenplaner.data.persistence.repository.UserEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    private UserEntityRepository userEntityRepository;

    private UserContext userContext;
    private UserAccountService service;

    @BeforeEach
    void setUp() {
        userContext = new UserContext();
        userContext.setCurrentUsername("admin");
        service = new UserAccountService(userEntityRepository, userContext);
    }

    @Test
    void ensureUserExistsCreatesNewEntityWithNormalizedValues() {
        when(userEntityRepository.findByUsernameIgnoreCase("cook")).thenReturn(Optional.empty());
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        when(userEntityRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserEntity created = service.ensureUserExists(" Cook ", "  Chef  ");

        verify(userEntityRepository).findByUsernameIgnoreCase("cook");
        verify(userEntityRepository).save(captor.capture());
        UserEntity persisted = captor.getValue();
        assertThat(persisted.getUsername()).isEqualTo("cook");
        assertThat(persisted.getDisplayName()).isEqualTo("Chef");
        assertThat(created.getUsername()).isEqualTo("cook");
    }

    @Test
    void ensureUserExistsUpdatesDisplayNameWhenUserAlreadyPresent() {
        UserEntity existing = new UserEntity();
        existing.setUsername("cook");
        existing.setDisplayName("Alt");
        when(userEntityRepository.findByUsernameIgnoreCase("cook")).thenReturn(Optional.of(existing));

        UserEntity result = service.ensureUserExists("COOK", " Neuer Name ");

        assertThat(result.getDisplayName()).isEqualTo("Neuer Name");
        verify(userEntityRepository).findByUsernameIgnoreCase("cook");
        verifyNoMoreInteractions(userEntityRepository);
    }

    @Test
    void createUserSanitizesBlankDisplayName() {
        when(userEntityRepository.findByUsernameIgnoreCase("gast")).thenReturn(Optional.empty());
        when(userEntityRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserEntity result = service.createUser(" Gast ", "   ");

        assertThat(result.getUsername()).isEqualTo("gast");
        assertThat(result.getDisplayName()).isNull();
    }

    @Test
    void switchUserNormalizesUsernameAndUpdatesContext() {
        when(userEntityRepository.findByUsernameIgnoreCase("koch")).thenReturn(Optional.empty());
        when(userEntityRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.switchUser(" KOCH ");

        assertThat(userContext.getCurrentUsername()).isEqualTo("koch");
        verify(userEntityRepository).findByUsernameIgnoreCase("koch");
        verify(userEntityRepository).save(any(UserEntity.class));
    }

    @Test
    void normalizeRejectsBlankUsernames() {
        assertThrows(IllegalArgumentException.class, () -> service.ensureUserExists("   ", "Name"));
    }

    @Test
    void ensureCurrentUserEntityEnsuresExistingUser() {
        userContext.setCurrentUsername("reporter");
        when(userEntityRepository.findByUsernameIgnoreCase("reporter")).thenReturn(Optional.empty());
        when(userEntityRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserEntity user = service.ensureCurrentUserEntity();

        assertThat(user.getUsername()).isEqualTo("reporter");
        verify(userEntityRepository).findByUsernameIgnoreCase("reporter");
        verify(userEntityRepository).save(any(UserEntity.class));
    }

    @Test
    void getAllUsersDelegatesToRepository() {
        when(userEntityRepository.findAll(any(Sort.class))).thenReturn(List.of());

        assertThat(service.getAllUsers()).isEmpty();
        verify(userEntityRepository).findAll(Sort.by(Sort.Direction.ASC, "username"));
    }
}
