package com.flaver.integrationservice.repository;

import com.flaver.integrationservice.entity.ConnectedAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConnectedAccountRepository extends JpaRepository<ConnectedAccount, UUID> {
    Optional<ConnectedAccount> findByUserIdAndProviderAndRevokedAtIsNull(UUID userId, String provider);
}
