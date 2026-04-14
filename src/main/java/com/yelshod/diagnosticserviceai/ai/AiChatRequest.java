package com.yelshod.diagnosticserviceai.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AiChatRequest(
        @Size(max = 2_000) String message,
        List<@Valid Message> history,
        @Valid Context context
) {
    public record Message(
            @Pattern(regexp = "user|assistant") String role,
            @Size(max = 4_000) String content
    ) {
    }

    public record Context(
            @Size(max = 120) String service,
            List<@Size(max = 2_000) String> logLines
    ) {
    }
}
