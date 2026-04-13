package com.yelshod.diagnosticserviceai.demo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.springframework.stereotype.Component;

@Component
public class DemoScenarioWriter {

    public void append(Path path, DemoScenarioLine line) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!Files.exists(path)) {
            Files.createFile(path);
        }

        String formatted = "%s %-5s [%s] %s%n".formatted(
                line.timestamp(),
                line.level(),
                line.service(),
                line.message()
        );
        Files.writeString(path, formatted, StandardOpenOption.APPEND);
    }
}
