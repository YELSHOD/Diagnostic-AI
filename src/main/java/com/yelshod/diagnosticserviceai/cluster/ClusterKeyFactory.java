package com.yelshod.diagnosticserviceai.cluster;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class ClusterKeyFactory {

    public String build(String exceptionType, String topFrame, String message) {
        return exceptionType + "|" + topFrame + "|" + sha256(normalize(message));
    }

    private String normalize(String message) {
        if (message == null) {
            return "";
        }
        return message
                .toLowerCase()
                .replaceAll("[0-9]+", "#")
                .replaceAll("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}", "{uuid}")
                .replaceAll("\\d{4}-\\d{2}-\\d{2}[ t]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?", "{date}")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
