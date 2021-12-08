package io.javabrains.springsecurityjpa;

import io.javabrains.springsecurityjpa.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

@ReadReplicaOnlyRepository
public interface UserReadReplicaOnlyRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUserName(String userName);
}
