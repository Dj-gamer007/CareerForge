package com.careerforge.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
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

    @JsonAlias({"notes", "recruiterNotes"})
    @Size(max = 3000, message = "Recruiter notes must not exceed 3000 characters")
    private String recruiterNotes;
}
