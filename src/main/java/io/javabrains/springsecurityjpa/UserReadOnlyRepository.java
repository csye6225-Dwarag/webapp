package io.javabrains.springsecurityjpa;

import io.javabrains.springsecurityjpa.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

@ReadOnlyRepository
public interface UserReadOnlyRepository extends JpaRepository<User, UUID> {
    User findByUsername(String username);
}
