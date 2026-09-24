package com.flaver.integrationservice.repository;

import com.flaver.integrationservice.entity.OAuthState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OAuthStateRepository extends JpaRepository<OAuthState, UUID> {
    Optional<OAuthState> findByState(String state);
}
