package com.yelshod.diagnosticserviceai.runtime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpsertRuntimeTargetRequest(
        @NotBlank
        @Size(max = 120)
        String name,

        @NotBlank
        @Size(max = 255)
        String host,

        @NotNull
        @Min(1)
        @Max(65535)
        Integer port,

        @NotBlank
        @Size(max = 500)
        @Pattern(regexp = "^https?://.+$")
        String healthUrl,

        @NotNull
        LogSourceType logSourceType,

        @NotBlank
        @Size(max = 1000)
        String logSourceRef,

        boolean enabled
) {
}
