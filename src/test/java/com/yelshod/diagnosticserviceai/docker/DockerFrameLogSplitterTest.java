package com.yelshod.diagnosticserviceai.docker;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class DockerFrameLogSplitterTest {

    private final DockerFrameLogSplitter splitter = new DockerFrameLogSplitter();

    @Test
    void splitsFramePayloadIntoNonBlankLogLines() {
        byte[] payload = "first line\n\nsecond line\r\n".getBytes(StandardCharsets.UTF_8);

        List<String> lines = splitter.split(payload);

        assertThat(lines).containsExactly("first line", "second line");
    }
}
