package com.careerforge.service.impl;

import com.careerforge.dto.request.CompanyCreateRequest;
import com.careerforge.dto.request.CompanyUpdateRequest;
import com.careerforge.dto.response.CompanyResponse;
import com.careerforge.dto.response.CompanySummaryResponse;
import com.careerforge.dto.response.PagedResponse;
import com.careerforge.entity.Company;
import com.careerforge.entity.RecruiterProfile;
import com.careerforge.entity.enums.CompanyVerificationStatus;
import com.careerforge.entity.enums.JobStatus;
import com.careerforge.exception.BadRequestException;
import com.careerforge.exception.ResourceNotFoundException;
import com.careerforge.exception.UnauthorizedException;
import com.careerforge.repository.CompanyRepository;
import com.careerforge.repository.JobRepository;
import com.careerforge.repository.RecruiterProfileRepository;
import com.careerforge.service.CompanyService;
import com.careerforge.service.RecruiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private final CompanyRepository companyRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final RecruiterService recruiterService;
    private final JobRepository jobRepository;

    @Override
    @Transactional
    public CompanyResponse createCompany(Long userId, CompanyCreateRequest request) {
        String companyName = request.getName().trim();

        if (companyRepository.existsByNameIgnoreCase(companyName)) {
            throw new BadRequestException("A company with the name '" + companyName + "' already exists");
        }

        RecruiterProfile recruiter = recruiterService.getOrCreateProfileEntity(userId);
        if (recruiter.getCompany() != null) {
            throw new BadRequestException("You are already associated with company: " + recruiter.getCompany().getName());
        }

        String slug = generateUniqueSlug(companyName);

        Company company = Company.builder()
                .name(companyName)
                .slug(slug)
                .website(request.getWebsite())
                .logoUrl(request.getLogoUrl())
                .description(request.getDescription())
                .industry(request.getIndustry().trim())
                .companySize(request.getCompanySize())
                .location(request.getLocation())
                .verificationStatus(CompanyVerificationStatus.VERIFIED)
                .build();

        Company savedCompany = companyRepository.save(company);

        // Associate creating recruiter as company admin
        recruiter.setCompany(savedCompany);
        recruiter.setCompanyAdmin(true);
        recruiterProfileRepository.save(recruiter);

        log.info("Created company '{}' (id: {}) by recruiter user ID: {}", savedCompany.getName(), savedCompany.getId(), userId);
        return mapToResponse(savedCompany);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse getMyCompany(Long userId) {
        RecruiterProfile recruiter = recruiterService.getProfileEntityByUserId(userId);
        if (recruiter.getCompany() == null) {
            throw new ResourceNotFoundException("Company", "recruiterUserId", userId);
        }
        return mapToResponse(recruiter.getCompany());
    }

    @Override
    @Transactional
    public CompanyResponse updateMyCompany(Long userId, CompanyUpdateRequest request) {
        RecruiterProfile recruiter = recruiterService.getProfileEntityByUserId(userId);
        Company company = recruiter.getCompany();

        if (company == null) {
            throw new BadRequestException("You are not associated with any company");
        }

        if (!recruiter.isCompanyAdmin()) {
            throw new UnauthorizedException("Only company admins can modify the company profile");
        }

        company.setWebsite(request.getWebsite());
        company.setLogoUrl(request.getLogoUrl());
        company.setDescription(request.getDescription());
        company.setIndustry(request.getIndustry().trim());
        company.setCompanySize(request.getCompanySize());
        company.setLocation(request.getLocation());

        Company saved = companyRepository.save(company);
        log.info("Updated company ID: {} by recruiter user ID: {}", saved.getId(), userId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(Long id) {
        Company company = getCompanyEntityById(id);
        return mapToResponse(company);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse getCompanyBySlug(String slug) {
        Company company = companyRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "slug", slug));
        return mapToResponse(company);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CompanySummaryResponse> getVerifiedCompanies(String search, Pageable pageable) {
        Page<Company> page;
        if (StringUtils.hasText(search)) {
            page = companyRepository.findAllByVerificationStatusAndNameContainingIgnoreCase(
                    CompanyVerificationStatus.VERIFIED, search.trim(), pageable);
        } else {
            page = companyRepository.findAllByVerificationStatus(CompanyVerificationStatus.VERIFIED, pageable);
        }

        List<CompanySummaryResponse> summaries = page.getContent().stream()
                .map(this::mapToSummaryResponse)
                .collect(Collectors.toList());

        return PagedResponse.of(page, summaries);
    }

    @Override
    @Transactional(readOnly = true)
    public Company getCompanyEntityById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", id));
    }

    private String generateUniqueSlug(String name) {
        String nowhitespace = WHITESPACE.matcher(name).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("").toLowerCase(Locale.ENGLISH);

        if (!companyRepository.existsBySlug(slug)) {
            return slug;
        }

        return slug + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    private CompanyResponse mapToResponse(Company company) {
        long totalJobs = jobRepository.countByCompany_Id(company.getId());
        long activeJobs = jobRepository.countByCompany_IdAndStatus(company.getId(), JobStatus.PUBLISHED);

        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .slug(company.getSlug())
                .website(company.getWebsite())
                .logoUrl(company.getLogoUrl())
                .description(company.getDescription())
                .industry(company.getIndustry())
                .companySize(company.getCompanySize())
                .location(company.getLocation())
                .verificationStatus(company.getVerificationStatus())
                .totalJobsCount(totalJobs)
                .activeJobsCount(activeJobs)
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }

    private CompanySummaryResponse mapToSummaryResponse(Company company) {
        long activeJobs = jobRepository.countByCompany_IdAndStatus(company.getId(), JobStatus.PUBLISHED);

        return CompanySummaryResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .slug(company.getSlug())
                .logoUrl(company.getLogoUrl())
                .industry(company.getIndustry())
                .location(company.getLocation())
                .companySize(company.getCompanySize())
                .activeJobsCount(activeJobs)
                .build();
    }
}
