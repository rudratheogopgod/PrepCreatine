package com.prepcreatine.service;

import com.prepcreatine.domain.*;
import com.prepcreatine.dto.response.UserResponse;
import com.prepcreatine.dto.response.UserSummaryResponse;
import com.prepcreatine.exception.*;
import com.prepcreatine.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Mentor-specific service per BSDD §16.
 * Mentors can:
 *   - List their students
 *   - View a student's full public profile
 *   - Write/upsert a private note per student
 *   - Remove a student link
 */
@Service
@Transactional
public class MentorService {

    private final MentorStudentLinkRepository linkRepo;
    private final MentorNoteRepository        noteRepo;
    private final UserRepository              userRepo;
    private final UserMapper                  mapper;

    public MentorService(MentorStudentLinkRepository linkRepo,
                         MentorNoteRepository noteRepo,
                         UserRepository userRepo,
                         UserMapper mapper) {
        this.linkRepo = linkRepo;
        this.noteRepo = noteRepo;
        this.userRepo = userRepo;
        this.mapper   = mapper;
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listStudents(UUID mentorId) {
        return linkRepo.findByMentorId(mentorId)
            .stream()
            .map(link -> userRepo.findByIdAndIsActiveTrue(link.getStudentId()).orElse(null))
            .filter(u -> u != null)
            .map(mapper::toSummary)
            .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getStudent(UUID mentorId, UUID studentId) {
        linkRepo.findByMentorIdAndStudentId(mentorId, studentId)
            .orElseThrow(() -> new ForbiddenException("Student not linked to this mentor.", mentorId));
        return userRepo.findByIdAndIsActiveTrue(studentId)
            .map(mapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
    }

    public String upsertNote(UUID mentorId, UUID studentId, String noteContent) {
        linkRepo.findByMentorIdAndStudentId(mentorId, studentId)
            .orElseThrow(() -> new ForbiddenException("Student not linked to this mentor.", mentorId));

        MentorNote note = noteRepo.findByMentorIdAndStudentId(mentorId, studentId)
            .orElseGet(() -> {
                MentorNote n = new MentorNote();
                n.setMentorId(mentorId);
                n.setStudentId(studentId);
                return n;
            });
        note.setNote(noteContent);
        return noteRepo.save(note).getNote();
    }

    @Transactional(readOnly = true)
    public String getNote(UUID mentorId, UUID studentId) {
        linkRepo.findByMentorIdAndStudentId(mentorId, studentId)
            .orElseThrow(() -> new ForbiddenException("Student not linked to this mentor.", mentorId));
        return noteRepo.findByMentorIdAndStudentId(mentorId, studentId)
            .map(MentorNote::getNote)
            .orElse("");
    }

    public void removeStudent(UUID mentorId, UUID studentId) {
        MentorStudentLink link = linkRepo.findByMentorIdAndStudentId(mentorId, studentId)
            .orElseThrow(() -> new ForbiddenException("Student not linked to this mentor.", mentorId));
        linkRepo.delete(link);
    }
}
