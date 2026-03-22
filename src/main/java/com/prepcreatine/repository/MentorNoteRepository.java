package com.prepcreatine.repository;

import com.prepcreatine.domain.MentorNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MentorNoteRepository extends JpaRepository<MentorNote, UUID> {

    Optional<MentorNote> findByMentorIdAndStudentId(UUID mentorId, UUID studentId);
}
