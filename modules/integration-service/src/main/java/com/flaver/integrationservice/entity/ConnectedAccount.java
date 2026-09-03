package com.flaver.integrationservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "connected_accounts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "provider"}))
@Getter
@Setter
public class ConnectedAccount {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String provider;

    private String providerAccountId;

    private String email;

    @Column(length = 1000)
    private String scopes;

    @Column(nullable = false, length = 4000)
    private String encryptedRefreshToken;

    private Instant connectedAt = Instant.now();

    private Instant updatedAt = Instant.now();

    private Instant revokedAt;

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
