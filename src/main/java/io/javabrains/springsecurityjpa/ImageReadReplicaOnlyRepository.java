package io.javabrains.springsecurityjpa;

import io.javabrains.springsecurityjpa.models.UserPic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


@ReadReplicaOnlyRepository
public interface ImageReadReplicaOnlyRepository extends JpaRepository<UserPic, Integer> {
    public UserPic findByUserId(String userId);
}

