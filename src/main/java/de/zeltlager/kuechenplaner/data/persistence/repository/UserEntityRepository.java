package de.zeltlager.kuechenplaner.data.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import de.zeltlager.kuechenplaner.data.persistence.entity.UserEntity;

public interface UserEntityRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsernameIgnoreCase(String username);
}
