package com.careerforge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationNotesRequest {

    @NotBlank(message = "Recruiter notes cannot be empty")
    @Size(max = 3000, message = "Recruiter notes must not exceed 3000 characters")
    private String recruiterNotes;
}
