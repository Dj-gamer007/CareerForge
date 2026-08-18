package com.careerforge.service;

import com.careerforge.dto.request.CompanyCreateRequest;
import com.careerforge.dto.request.CompanyUpdateRequest;
import com.careerforge.dto.response.CompanyResponse;
import com.careerforge.entity.Company;
import com.careerforge.entity.RecruiterProfile;
import com.careerforge.entity.User;
import com.careerforge.entity.enums.CompanyVerificationStatus;
import com.careerforge.entity.enums.Role;
import com.careerforge.exception.BadRequestException;
import com.careerforge.exception.UnauthorizedException;
import com.careerforge.repository.CompanyRepository;
import com.careerforge.repository.JobRepository;
import com.careerforge.repository.RecruiterProfileRepository;
import com.careerforge.service.impl.CompanyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private RecruiterProfileRepository recruiterProfileRepository;
    @Mock
    private RecruiterService recruiterService;
    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private CompanyServiceImpl companyService;

    private User recruiterUser;
    private RecruiterProfile recruiterProfile;
    private Company testCompany;

    @BeforeEach
    void setUp() {
        recruiterUser = User.builder()
                .id(2L)
                .email("recruiter@careerforge.local")
                .role(Role.ROLE_RECRUITER)
                .build();

        recruiterProfile = RecruiterProfile.builder()
                .id(20L)
                .user(recruiterUser)
                .firstName("Sarah")
                .lastName("Connor")
                .designation("Head of Talent")
                .isCompanyAdmin(false)
                .build();

        testCompany = Company.builder()
                .id(100L)
                .name("Acme Corporation")
                .slug("acme-corporation")
                .industry("Technology")
                .companySize("51-200")
                .location("San Francisco, CA")
                .verificationStatus(CompanyVerificationStatus.VERIFIED)
                .build();
    }

    @Test
    @DisplayName("Should create company and assign recruiter as admin")
    void testCreateCompany_Success() {
        CompanyCreateRequest request = CompanyCreateRequest.builder()
                .name("Acme Corporation")
                .industry("Technology")
                .companySize("51-200")
                .location("San Francisco, CA")
                .build();

        when(companyRepository.existsByNameIgnoreCase("Acme Corporation")).thenReturn(false);
        when(recruiterService.getOrCreateProfileEntity(2L)).thenReturn(recruiterProfile);
        when(companyRepository.existsBySlug(anyString())).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company c = invocation.getArgument(0);
            c.setId(100L);
            return c;
        });

        CompanyResponse response = companyService.createCompany(2L, request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Acme Corporation");
        assertThat(response.getSlug()).isNotNull();
        assertThat(response.getVerificationStatus()).isEqualTo(CompanyVerificationStatus.VERIFIED);
        assertThat(recruiterProfile.isCompanyAdmin()).isTrue();
        assertThat(recruiterProfile.getCompany()).isNotNull();
        assertThat(recruiterProfile.getCompany().getId()).isEqualTo(100L);
        assertThat(recruiterProfile.getCompany().getName()).isEqualTo("Acme Corporation");
        verify(recruiterProfileRepository).save(recruiterProfile);
    }

    @Test
    @DisplayName("Should reject company creation if name already exists")
    void testCreateCompany_DuplicateName() {
        CompanyCreateRequest request = CompanyCreateRequest.builder()
                .name("Acme Corporation")
                .industry("Technology")
                .build();

        when(companyRepository.existsByNameIgnoreCase("Acme Corporation")).thenReturn(true);

        assertThatThrownBy(() -> companyService.createCompany(2L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should reject company update if recruiter is not company admin")
    void testUpdateCompany_NotAdmin() {
        recruiterProfile.setCompany(testCompany);
        recruiterProfile.setCompanyAdmin(false);

        CompanyUpdateRequest request = CompanyUpdateRequest.builder()
                .industry("Fintech")
                .build();

        when(recruiterService.getProfileEntityByUserId(2L)).thenReturn(recruiterProfile);

        assertThatThrownBy(() -> companyService.updateMyCompany(2L, request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Only company admins can modify");
    }

    @Test
    @DisplayName("Should update company if recruiter is company admin")
    void testUpdateCompany_Success() {
        recruiterProfile.setCompany(testCompany);
        recruiterProfile.setCompanyAdmin(true);

        CompanyUpdateRequest request = CompanyUpdateRequest.builder()
                .industry("Fintech")
                .location("New York, NY")
                .build();

        when(recruiterService.getProfileEntityByUserId(2L)).thenReturn(recruiterProfile);
        when(companyRepository.save(any(Company.class))).thenReturn(testCompany);

        CompanyResponse response = companyService.updateMyCompany(2L, request);

        assertThat(response).isNotNull();
        assertThat(testCompany.getIndustry()).isEqualTo("Fintech");
        assertThat(testCompany.getLocation()).isEqualTo("New York, NY");
        verify(companyRepository).save(testCompany);
    }

    @Test
    @DisplayName("Should get company by ID")
    void testGetCompanyById_Success() {
        when(companyRepository.findById(100L)).thenReturn(Optional.of(testCompany));

        CompanyResponse response = companyService.getCompanyById(100L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getName()).isEqualTo("Acme Corporation");
    }
}
