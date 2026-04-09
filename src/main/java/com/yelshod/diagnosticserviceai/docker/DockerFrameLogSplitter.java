package com.yelshod.diagnosticserviceai.docker;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DockerFrameLogSplitter {

    public List<String> split(byte[] payload) {
        String text = new String(payload, StandardCharsets.UTF_8);
        return Arrays.stream(text.split("\\R"))
                .filter(line -> !line.isBlank())
                .toList();
    }
}
