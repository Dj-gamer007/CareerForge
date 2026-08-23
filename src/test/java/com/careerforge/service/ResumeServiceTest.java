package com.careerforge.service;

import com.careerforge.config.StorageConfigProperties;
import com.careerforge.dto.response.ResumeResponse;
import com.careerforge.entity.Application;
import com.careerforge.entity.Resume;
import com.careerforge.entity.StudentProfile;
import com.careerforge.entity.User;
import com.careerforge.entity.enums.Role;
import com.careerforge.exception.BadRequestException;
import com.careerforge.exception.ResourceNotFoundException;
import com.careerforge.repository.ResumeRepository;
import com.careerforge.service.impl.ResumeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private StudentProfileService studentProfileService;
    @Mock
    private com.careerforge.repository.ApplicationRepository applicationRepository;
    @Mock
    private StorageService storageService;
    @Spy
    private StorageConfigProperties storageConfigProperties = new StorageConfigProperties();

    @InjectMocks
    private ResumeServiceImpl resumeService;

    private User testUser;
    private StudentProfile testProfile;
    private Resume testResume;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("student@careerforge.local")
                .role(Role.ROLE_STUDENT)
                .build();

        testProfile = StudentProfile.builder()
                .id(10L)
                .user(testUser)
                .firstName("John")
                .lastName("Doe")
                .build();

        testResume = Resume.builder()
                .id(100L)
                .studentProfile(testProfile)
                .originalFileName("my_resume.pdf")
                .storedFileName("uuid-12345.pdf")
                .storagePath("/uploads/resumes/uuid-12345.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .version(1)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should upload valid PDF resume successfully")
    void testUploadResume_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "%PDF-1.4 dummy pdf content".getBytes()
        );

        when(studentProfileService.getOrCreateProfileEntity(1L)).thenReturn(testProfile);
        when(storageService.store(file)).thenReturn("uuid-generated.pdf");
        when(storageService.getFilePath("uuid-generated.pdf")).thenReturn(Paths.get("/tmp/uuid-generated.pdf"));
        when(resumeRepository.findAllByStudentProfileOrderByUploadedAtDesc(testProfile)).thenReturn(Collections.emptyList());
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume r = invocation.getArgument(0);
            r.setId(100L);
            return r;
        });

        ResumeResponse response = resumeService.uploadResume(1L, file);

        assertThat(response).isNotNull();
        assertThat(response.getOriginalFileName()).isEqualTo("resume.pdf");
        assertThat(response.getContentType()).isEqualTo("application/pdf");
        assertThat(response.isActive()).isTrue();
        verify(storageService).store(file);
        verify(studentProfileService).updateProfileCompletion(testProfile);
    }

    @Test
    @DisplayName("Should reject non-PDF file upload")
    void testUploadResume_RejectNonPdf() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script.exe",
                "application/x-msdownload",
                "malicious content".getBytes()
        );

        assertThatThrownBy(() -> resumeService.uploadResume(1L, file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only PDF files (.pdf) are supported");

        verifyNoInteractions(storageService);
    }

    @Test
    @DisplayName("Should reject oversized file upload")
    void testUploadResume_RejectOversized() {
        byte[] largeBytes = new byte[6 * 1024 * 1024]; // 6 MB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.pdf",
                "application/pdf",
                largeBytes
        );

        assertThatThrownBy(() -> resumeService.uploadResume(1L, file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exceeds maximum permitted limit");

        verifyNoInteractions(storageService);
    }

    @Test
    @DisplayName("Should download own resume successfully")
    void testDownloadResume_Success() {
        Resource mockResource = new ByteArrayResource("%PDF dummy".getBytes());

        when(studentProfileService.getProfileEntityByUserId(1L)).thenReturn(testProfile);
        when(resumeRepository.findByIdAndStudentProfile(100L, testProfile)).thenReturn(Optional.of(testResume));
        when(storageService.loadAsResource("uuid-12345.pdf")).thenReturn(mockResource);

        ResumeService.ResumeDownloadResult result = resumeService.downloadResume(1L, 100L);

        assertThat(result).isNotNull();
        assertThat(result.originalFileName()).isEqualTo("my_resume.pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.resource()).isNotNull();
    }

    @Test
    @DisplayName("Should reject download of another student's resume")
    void testDownloadResume_OwnershipEnforced() {
        when(studentProfileService.getProfileEntityByUserId(1L)).thenReturn(testProfile);
        when(resumeRepository.findByIdAndStudentProfile(999L, testProfile)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resumeService.downloadResume(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should set active resume and deactivate previous")
    void testSetActiveResume_Success() {
        when(studentProfileService.getProfileEntityByUserId(1L)).thenReturn(testProfile);
        when(resumeRepository.findByIdAndStudentProfile(100L, testProfile)).thenReturn(Optional.of(testResume));
        when(resumeRepository.save(any(Resume.class))).thenReturn(testResume);

        ResumeResponse response = resumeService.setActiveResume(1L, 100L);

        assertThat(response).isNotNull();
        verify(resumeRepository).deactivateAllByStudentProfile(testProfile);
        verify(studentProfileService).updateProfileCompletion(testProfile);
    }

    @Test
    @DisplayName("Should delete resume from storage and database")
    void testDeleteResume_Success() {
        when(studentProfileService.getOrCreateProfileEntity(1L)).thenReturn(testProfile);
        when(resumeRepository.findByIdAndStudentProfile(100L, testProfile)).thenReturn(Optional.of(testResume));
        when(applicationRepository.findAllByResume(testResume)).thenReturn(Collections.emptyList());
        when(resumeRepository.findAllByStudentProfileOrderByUploadedAtDesc(testProfile)).thenReturn(Collections.emptyList());

        resumeService.deleteResume(1L, 100L);

        verify(storageService).delete("uuid-12345.pdf");
        verify(resumeRepository).delete(testResume);
        verify(studentProfileService).updateProfileCompletion(testProfile);
    }

    @Test
    @DisplayName("Should delete active resume and automatically activate next newest resume")
    void testDeleteActiveResume_ActivatesNextRemainingResume() {
        Resume nextResume = Resume.builder()
                .id(101L)
                .studentProfile(testProfile)
                .originalFileName("second_resume.pdf")
                .storedFileName("uuid-67890.pdf")
                .isActive(false)
                .build();

        when(studentProfileService.getOrCreateProfileEntity(1L)).thenReturn(testProfile);
        when(resumeRepository.findByIdAndStudentProfile(100L, testProfile)).thenReturn(Optional.of(testResume));
        when(applicationRepository.findAllByResume(testResume)).thenReturn(Collections.emptyList());
        when(resumeRepository.findAllByStudentProfileOrderByUploadedAtDesc(testProfile)).thenReturn(List.of(nextResume));

        resumeService.deleteResume(1L, 100L);

        verify(storageService).delete("uuid-12345.pdf");
        verify(resumeRepository).delete(testResume);
        assertThat(nextResume.isActive()).isTrue();
        verify(resumeRepository).save(nextResume);
        verify(studentProfileService).updateProfileCompletion(testProfile);
    }

    @Test
    @DisplayName("Should reassign linked applications when deleting resume if other resumes exist")
    void testDeleteResume_ReassignsLinkedApplications() {
        Resume nextResume = Resume.builder()
                .id(101L)
                .studentProfile(testProfile)
                .originalFileName("second_resume.pdf")
                .storedFileName("uuid-67890.pdf")
                .isActive(false)
                .build();

        Application linkedApp = Application.builder()
                .id(50L)
                .studentProfile(testProfile)
                .resume(testResume)
                .build();

        when(studentProfileService.getOrCreateProfileEntity(1L)).thenReturn(testProfile);
        when(resumeRepository.findByIdAndStudentProfile(100L, testProfile)).thenReturn(Optional.of(testResume));
        when(applicationRepository.findAllByResume(testResume)).thenReturn(List.of(linkedApp));
        when(resumeRepository.findAllByStudentProfileOrderByUploadedAtDesc(testProfile)).thenReturn(List.of(nextResume));

        resumeService.deleteResume(1L, 100L);

        assertThat(linkedApp.getResume()).isEqualTo(nextResume);
        verify(applicationRepository).saveAll(List.of(linkedApp));
        verify(resumeRepository).delete(testResume);
        verify(storageService).delete("uuid-12345.pdf");
    }
}
