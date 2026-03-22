package com.prepcreatine.repository;

import com.prepcreatine.domain.ProctoringEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProctoringEventRepository extends JpaRepository<ProctoringEvent, UUID> {

    List<ProctoringEvent> findByTestSessionId(UUID testSessionId);

    /**
     * Upsert a proctoring event count.
     * If a row for (test_session_id, event_type) already exists, increments the count.
     * Otherwise inserts a new row.
     */
    @Modifying
    @Query(value = """
        INSERT INTO proctoring_events (id, user_id, test_session_id, event_type, event_count)
        VALUES (gen_random_uuid(), :userId, :sessionId, :eventType, :count)
        ON CONFLICT ON CONSTRAINT proctoring_events_session_type_uq
        DO UPDATE SET event_count = proctoring_events.event_count + EXCLUDED.event_count
        """, nativeQuery = true)
    void upsertEvent(@Param("userId") UUID userId,
                     @Param("sessionId") UUID sessionId,
                     @Param("eventType") String eventType,
                     @Param("count") int count);

    boolean existsByTestSessionId(UUID testSessionId);
}
