package com.yelshod.diagnosticserviceai.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yelshod.diagnosticserviceai.config.AppProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;

class RuntimeTargetBootstrapTest {

    @Test
    void seedsConfiguredLocalTargetsWhenRepositoryIsEmpty() throws Exception {
        RuntimeTargetRepository repository = mock(RuntimeTargetRepository.class);
        when(repository.count()).thenReturn(0L);

        AppProperties properties = new AppProperties(
                new AppProperties.Docker("ai.project.env", "demo", 200),
                null,
                new AppProperties.Runtime(List.of(
                        new AppProperties.LocalTarget(
                                "diagnostic-ai-front",
                                "localhost",
                                5173,
                                "http://localhost:5173",
                                "FILE_TAIL",
                                "/tmp/diagnostic-ai-front.log"
                        )
                ))
        );

        RuntimeTargetBootstrap bootstrap = new RuntimeTargetBootstrap(repository, properties);

        bootstrap.run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<List<RuntimeTargetEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .singleElement()
                .satisfies(target -> {
                    assertThat(target.getName()).isEqualTo("diagnostic-ai-front");
                    assertThat(target.getType()).isEqualTo(RuntimeTargetType.LOCAL_SERVICE);
                    assertThat(target.getLogSourceType()).isEqualTo(LogSourceType.FILE_TAIL);
                });
    }
}
