package com.prepcreatine.repository;

import com.prepcreatine.domain.MentorStudentLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MentorStudentLinkRepository extends JpaRepository<MentorStudentLink, UUID> {

    List<MentorStudentLink> findByMentorId(UUID mentorId);

    Optional<MentorStudentLink> findByMentorIdAndStudentId(UUID mentorId, UUID studentId);
}
