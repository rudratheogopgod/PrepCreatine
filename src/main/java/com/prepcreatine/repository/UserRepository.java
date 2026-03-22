package com.prepcreatine.repository;

import com.prepcreatine.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByShareToken(String shareToken);

    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.isActive = false WHERE u.id = :userId")
    int deactivateUser(UUID userId);

    Optional<User> findByIdAndIsActiveTrue(UUID id);

    Optional<User> findByEmailAndIsActiveTrue(String email);

    Optional<User> findByMentorCodeAndIsActiveTrue(String mentorCode);
}
