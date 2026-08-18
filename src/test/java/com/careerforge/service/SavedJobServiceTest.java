package com.careerforge.service;

import com.careerforge.dto.response.PagedResponse;
import com.careerforge.dto.response.SavedJobResponse;
import com.careerforge.entity.Company;
import com.careerforge.entity.Job;
import com.careerforge.entity.SavedJob;
import com.careerforge.entity.StudentProfile;
import com.careerforge.entity.enums.*;
import com.careerforge.exception.BadRequestException;
import com.careerforge.exception.ResourceNotFoundException;
import com.careerforge.repository.JobRepository;
import com.careerforge.repository.JobSkillRepository;
import com.careerforge.repository.SavedJobRepository;
import com.careerforge.repository.StudentProfileRepository;
import com.careerforge.service.impl.SavedJobServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedJobServiceTest {

    @Mock
    private SavedJobRepository savedJobRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private JobSkillRepository jobSkillRepository;

    @InjectMocks
    private SavedJobServiceImpl savedJobService;

    private StudentProfile studentProfile;
    private Company company;
    private Job publishedJob;
    private Job draftJob;
    private SavedJob savedJob;

    @BeforeEach
    void setUp() {
        studentProfile = StudentProfile.builder()
                .id(10L)
                .firstName("John")
                .lastName("Doe")
                .build();

        company = Company.builder()
                .id(50L)
                .name("Acme Tech")
                .slug("acme-tech")
                .build();

        publishedJob = Job.builder()
                .id(100L)
                .title("Software Engineer")
                .slug("software-engineer-100")
                .company(company)
                .location("Bengaluru")
                .workMode(WorkMode.REMOTE)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.MID_LEVEL)
                .salaryMin(BigDecimal.valueOf(1000000))
                .salaryMax(BigDecimal.valueOf(1500000))
                .currency("INR")
                .status(JobStatus.PUBLISHED)
                .deadline(LocalDateTime.now().plusDays(20))
                .build();

        draftJob = Job.builder()
                .id(101L)
                .title("Draft Engineer")
                .company(company)
                .status(JobStatus.DRAFT)
                .build();

        savedJob = SavedJob.builder()
                .id(500L)
                .studentProfile(studentProfile)
                .job(publishedJob)
                .build();
    }

    @Test
    @DisplayName("Should bookmark published job successfully")
    void testSaveJob_Success() {
        when(studentProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(studentProfile));
        when(jobRepository.findById(100L)).thenReturn(Optional.of(publishedJob));
        when(savedJobRepository.existsByStudentProfile_IdAndJob_Id(10L, 100L)).thenReturn(false);
        when(savedJobRepository.save(any(SavedJob.class))).thenAnswer(i -> {
            SavedJob sj = i.getArgument(0);
            sj.setId(500L);
            return sj;
        });
        when(jobSkillRepository.findAllByJobWithSkill(publishedJob)).thenReturn(Collections.emptyList());

        SavedJobResponse response = savedJobService.saveJob(1L, 100L);

        assertThat(response).isNotNull();
        assertThat(response.getJobId()).isEqualTo(100L);
        assertThat(response.getJobTitle()).isEqualTo("Software Engineer");
        verify(savedJobRepository).save(any(SavedJob.class));
    }

    @Test
    @DisplayName("Should reject bookmarking unpublished draft job")
    void testSaveJob_UnpublishedJob() {
        when(studentProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(studentProfile));
        when(jobRepository.findById(101L)).thenReturn(Optional.of(draftJob));

        assertThatThrownBy(() -> savedJobService.saveJob(1L, 101L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only published jobs can be bookmarked");
    }

    @Test
    @DisplayName("Should reject duplicate bookmarking of the same job")
    void testSaveJob_Duplicate() {
        when(studentProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(studentProfile));
        when(jobRepository.findById(100L)).thenReturn(Optional.of(publishedJob));
        when(savedJobRepository.existsByStudentProfile_IdAndJob_Id(10L, 100L)).thenReturn(true);

        assertThatThrownBy(() -> savedJobService.saveJob(1L, 100L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already saved");
    }

    @Test
    @DisplayName("Should remove saved job from bookmarks")
    void testRemoveSavedJob_Success() {
        when(studentProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(studentProfile));
        when(savedJobRepository.findByStudentProfile_IdAndJob_Id(10L, 100L)).thenReturn(Optional.of(savedJob));

        savedJobService.removeSavedJob(1L, 100L);

        verify(savedJobRepository).delete(savedJob);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when removing unsaved job")
    void testRemoveSavedJob_NotFound() {
        when(studentProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(studentProfile));
        when(savedJobRepository.findByStudentProfile_IdAndJob_Id(10L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> savedJobService.removeSavedJob(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should return paginated saved jobs for student")
    void testGetSavedJobs() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SavedJob> page = new PageImpl<>(List.of(savedJob), pageable, 1);

        when(studentProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(studentProfile));
        when(savedJobRepository.findAllByStudentProfile_Id(10L, pageable)).thenReturn(page);
        when(jobSkillRepository.findAllByJob_IdInWithSkill(List.of(100L))).thenReturn(Collections.emptyList());

        PagedResponse<SavedJobResponse> response = savedJobService.getSavedJobs(1L, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getJobId()).isEqualTo(100L);
    }
}
