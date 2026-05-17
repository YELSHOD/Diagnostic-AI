package com.yelshod.diagnosticserviceai.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank
        @Size(max = 160)
        String name
) {
}
