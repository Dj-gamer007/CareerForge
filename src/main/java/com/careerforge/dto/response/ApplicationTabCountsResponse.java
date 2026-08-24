package com.careerforge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationTabCountsResponse {

    private long all;
    private long applied;
    private long shortlisted;
    private long interview;
}
