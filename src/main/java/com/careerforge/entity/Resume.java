package com.careerforge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Stores resume file metadata only — the binary PDF is stored on the local
 * filesystem (or cloud in future), never in MySQL.
 *
 * Only one resume should be active at a time per student. This is enforced
 * at the service layer. isActive drives which resume recruiters see.
 */
@Entity
@Table(name = "resumes", indexes = {
    @Index(name = "idx_resume_student_profile", columnList = "student_profile_id"),
    @Index(name = "idx_resume_active",          columnList = "student_profile_id, is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    /** Original filename provided by the user — for display/download header only. */
    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    /** UUID-based filename stored on disk — safe, non-guessable. */
    @Column(name = "stored_file_name", nullable = false, unique = true, length = 255)
    private String storedFileName;

    /** Absolute path to the file on local storage. */
    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;

    /** MIME type validated at upload time (always application/pdf in Phase 2). */
    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    /** File size in bytes. */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /** Monotonically increasing version per student — set by service layer. */
    @Builder.Default
    @Column(nullable = false)
    private Integer version = 1;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = false;

    @Builder.Default
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt = Instant.now();
}
