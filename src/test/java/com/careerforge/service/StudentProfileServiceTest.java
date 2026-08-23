package com.careerforge.service;

import com.careerforge.dto.request.*;
import com.careerforge.dto.response.*;
import com.careerforge.entity.*;
import com.careerforge.entity.enums.Role;
import com.careerforge.entity.enums.SkillProficiency;
import com.careerforge.exception.BadRequestException;
import com.careerforge.exception.ResourceNotFoundException;
import com.careerforge.repository.*;
import com.careerforge.service.impl.StudentProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentProfileServiceTest {

    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SkillRepository skillRepository;
    @Mock
    private StudentSkillRepository studentSkillRepository;
    @Mock
    private EducationRepository educationRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private CertificationRepository certificationRepository;
    @Mock
    private ResumeRepository resumeRepository;

    @InjectMocks
    private StudentProfileServiceImpl studentProfileService;

    private User testUser;
    private StudentProfile testProfile;
    private Skill testSkill;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("student@careerforge.local")
                .role(Role.ROLE_STUDENT)
                .enabled(true)
                .build();

        testProfile = StudentProfile.builder()
                .id(10L)
                .user(testUser)
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .location("San Francisco, CA")
                .bio("Aspiring Software Engineer")
                .profileCompletionPercentage(25)
                .build();

        testSkill = Skill.builder()
                .id(100L)
                .name("Java")
                .category("Backend")
                .build();
    }

    @Test
    @DisplayName("Should create student profile successfully")
    void testCreateProfile_Success() {
        StudentProfileRequest request = StudentProfileRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .location("San Francisco, CA")
                .bio("Aspiring Software Engineer")
                .build();

        when(studentProfileRepository.existsByUser_Id(1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(invocation -> {
            StudentProfile p = invocation.getArgument(0);
            p.setId(10L);
            return p;
        });

        StudentProfileResponse response = studentProfileService.createProfile(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");
        assertThat(response.getProfileCompletionPercentage()).isGreaterThan(0);
        verify(studentProfileRepository).save(any(StudentProfile.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException if student profile already exists on create")
    void testCreateProfile_AlreadyExists() {
        StudentProfileRequest request = StudentProfileRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .build();

        when(studentProfileRepository.existsByUser_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> studentProfileService.createProfile(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should get profile by user ID")
    void testGetProfile_Success() {
        when(studentProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(testProfile));

        StudentProfileResponse response = studentProfileService.getProfileByUserId(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getEmail()).isEqualTo("student@careerforge.local");
    }

    @Test
    @DisplayName("Should add skill to student profile")
    void testAddSkill_Success() {
        StudentSkillRequest request = StudentSkillRequest.builder()
                .skillId(100L)
                .proficiency(SkillProficiency.ADVANCED)
                .build();

        when(studentProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(testProfile));
        when(skillRepository.findById(100L)).thenReturn(Optional.of(testSkill));
        when(studentSkillRepository.existsByStudentProfileAndSkill_Id(testProfile, 100L)).thenReturn(false);
        when(studentSkillRepository.save(any(StudentSkill.class))).thenAnswer(invocation -> {
            StudentSkill ss = invocation.getArgument(0);
            ss.setId(50L);
            return ss;
        });

        StudentSkillResponse response = studentProfileService.addSkill(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getSkillName()).isEqualTo("Java");
        assertThat(response.getProficiency()).isEqualTo(SkillProficiency.ADVANCED);
    }

    @Test
    @DisplayName("Should reject duplicate skill addition")
    void testAddSkill_DuplicateRejection() {
        StudentSkillRequest request = StudentSkillRequest.builder()
                .skillId(100L)
                .proficiency(SkillProficiency.ADVANCED)
                .build();

        when(studentProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(testProfile));
        when(skillRepository.findById(100L)).thenReturn(Optional.of(testSkill));
        when(studentSkillRepository.existsByStudentProfileAndSkill_Id(testProfile, 100L)).thenReturn(true);

        assertThatThrownBy(() -> studentProfileService.addSkill(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already added");
    }

    @Test
    @DisplayName("Should add education successfully")
    void testAddEducation_Success() {
        EducationRequest request = EducationRequest.builder()
                .institution("MIT")
                .degree("Bachelor of Science")
                .fieldOfStudy("Computer Science")
                .startDate(LocalDate.of(2020, 9, 1))
                .currentlyStudying(true)
                .gradeOrGpa("3.9")
                .build();

        when(studentProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(testProfile));
        when(educationRepository.save(any(Education.class))).thenAnswer(invocation -> {
            Education edu = invocation.getArgument(0);
            edu.setId(20L);
            return edu;
        });

        EducationResponse response = studentProfileService.addEducation(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getInstitution()).isEqualTo("MIT");
        assertThat(response.isCurrentlyStudying()).isTrue();
    }

    @Test
    @DisplayName("Should enforce ownership when deleting another student's education")
    void testDeleteEducation_OwnershipEnforced() {
        when(studentProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(testProfile));
        when(educationRepository.findByIdAndStudentProfile(999L, testProfile)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentProfileService.deleteEducation(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should add project successfully")
    void testAddProject_Success() {
        ProjectRequest request = ProjectRequest.builder()
                .title("CareerForge")
                .description("Intelligent Career Platform")
                .technologies("Java, Spring Boot, React")
                .build();

        when(studentProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(testProfile));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project p = invocation.getArgument(0);
            p.setId(30L);
            return p;
        });

        ProjectResponse response = studentProfileService.addProject(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("CareerForge");
    }

    @Test
    @DisplayName("Should add certification successfully")
    void testAddCertification_Success() {
        CertificationRequest request = CertificationRequest.builder()
                .name("AWS Certified Developer")
                .issuingOrganization("Amazon Web Services")
                .issueDate(LocalDate.of(2023, 5, 1))
                .credentialId("AWS-12345")
                .build();

        when(studentProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(testProfile));
        when(certificationRepository.save(any(Certification.class))).thenAnswer(invocation -> {
            Certification c = invocation.getArgument(0);
            c.setId(40L);
            return c;
        });

        CertificationResponse response = studentProfileService.addCertification(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("AWS Certified Developer");
    }

    @Test
    @DisplayName("Should return existing profile unchanged when calling getOrCreateProfileEntity")
    void testGetOrCreateProfileEntity_ExistingProfile_ReturnsUnchanged() {
        when(studentProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(testProfile));

        StudentProfile result = studentProfileService.getOrCreateProfileEntity(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        verify(studentProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create and return new default profile when user has no existing profile")
    void testGetOrCreateProfileEntity_MissingProfile_CreatesDefaultProfile() {
        when(studentProfileRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(invocation -> {
            StudentProfile p = invocation.getArgument(0);
            p.setId(99L);
            return p;
        });

        StudentProfile result = studentProfileService.getOrCreateProfileEntity(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getFirstName()).isEqualTo("Student");
        assertThat(result.getLastName()).isEqualTo("User");
        assertThat(result.getUser()).isEqualTo(testUser);
        verify(studentProfileRepository, times(1)).save(any(StudentProfile.class));
    }
}
