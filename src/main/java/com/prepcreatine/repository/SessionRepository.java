package com.prepcreatine.repository;

import com.prepcreatine.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findByUserIdAndDate(UUID userId, LocalDate date);

    boolean existsByUserIdAndDate(UUID userId, LocalDate date);

    @Query("SELECT COUNT(DISTINCT s.date) FROM Session s WHERE s.userId = :userId")
    int countDistinctDaysByUserId(UUID userId);

    interface DailyStudy {
        LocalDate getDate();
        int getDurationMins();
    }

    @Query("SELECT s.date as date, s.durationMins as durationMins FROM Session s WHERE s.userId = :userId AND s.date >= :fromDate ORDER BY s.date ASC")
    List<DailyStudy> findDailyStudyMins(UUID userId, LocalDate fromDate);
}
