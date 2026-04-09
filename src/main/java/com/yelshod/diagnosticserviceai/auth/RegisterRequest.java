package com.yelshod.diagnosticserviceai.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email
        @NotBlank
        @Size(max = 320)
        String email,

        @NotBlank
        @Size(min = 3, max = 100)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$")
        String username,

        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @NotBlank
        @Pattern(regexp = "^[A-Z_]+$")
        String role
) {
}
