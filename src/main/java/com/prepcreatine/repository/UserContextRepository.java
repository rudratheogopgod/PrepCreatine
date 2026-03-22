package com.prepcreatine.repository;

import com.prepcreatine.domain.UserContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserContextRepository extends JpaRepository<UserContext, UUID> {

    Optional<UserContext> findByUserId(UUID userId);
}
