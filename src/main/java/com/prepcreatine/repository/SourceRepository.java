package com.prepcreatine.repository;

import com.prepcreatine.domain.Source;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SourceRepository extends JpaRepository<Source, UUID> {
    
    List<Source> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    Optional<Source> findByIdAndUserId(UUID id, UUID userId);
}
