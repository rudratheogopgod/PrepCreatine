package com.prepcreatine.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * MentorStudentLink — maps to `mentor_student_links` table.
 * Created when a student enters a valid mentor code during onboarding
 * or via GET /api/me/connect-mentor?code=XXX.
 */
@Entity
@Table(name = "mentor_student_links",
       uniqueConstraints = @UniqueConstraint(columnNames = {"mentor_id", "student_id"}))
public class MentorStudentLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mentor_id", nullable = false)
    private UUID mentorId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "mentor_code", nullable = false, length = 20)
    private String mentorCode;

    @Column(name = "linked_at", nullable = false, updatable = false)
    private OffsetDateTime linkedAt = OffsetDateTime.now();

    public MentorStudentLink() {}

    public UUID getId() { return id; }
    public UUID getMentorId() { return mentorId; }
    public void setMentorId(UUID mentorId) { this.mentorId = mentorId; }
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }
    public String getMentorCode() { return mentorCode; }
    public void setMentorCode(String mentorCode) { this.mentorCode = mentorCode; }
    public OffsetDateTime getLinkedAt() { return linkedAt; }
}
