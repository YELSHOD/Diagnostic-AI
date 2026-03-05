package com.yelshod.diagnosticserviceai.common;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class RedactionService {

    private static final List<Pattern> SENSITIVE_PATTERNS = List.of(
            Pattern.compile("(?i)(authorization\\s*:\\s*)([^\\s]+)"),
            Pattern.compile("(?i)(password\\s*[=:]\\s*)([^\\s,;]+)"),
            Pattern.compile("(?i)(bearer\\s+)([a-z0-9\\-_.]+)"),
            Pattern.compile("(?i)(jwt\\s*[=:]\\s*)([a-z0-9\\-_.]+)")
    );

    public String redact(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        String result = input;
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            result = pattern.matcher(result).replaceAll("$1[REDACTED]");
        }
        return result;
    }
}
