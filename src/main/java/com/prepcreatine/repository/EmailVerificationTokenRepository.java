package com.prepcreatine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<com.prepcreatine.domain.EmailVerificationToken, UUID> {
    Optional<com.prepcreatine.domain.EmailVerificationToken> findByTokenAndType(String token, String type);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE EmailVerificationToken e SET e.usedAt = current_timestamp WHERE e.userId = :userId AND e.type = :type AND e.usedAt IS NULL")
    void invalidateExistingTokens(UUID userId, String type);
}
