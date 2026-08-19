package com.careerforge.dto.request;

import com.careerforge.entity.enums.CompanyVerificationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyVerificationUpdateRequest {

    @NotNull(message = "Verification status is required")
    private CompanyVerificationStatus verificationStatus;

    @NotBlank(message = "Reason for verification update is required")
    private String reason;
}
