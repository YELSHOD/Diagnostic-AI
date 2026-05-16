package com.yelshod.diagnosticserviceai.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yelshod.diagnosticserviceai.config.AppProperties;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;

class RuntimeTargetBootstrapTest {

    @Test
    void seedsConfiguredLocalTargetsWhenRepositoryIsEmpty() throws Exception {
        RuntimeTargetRepository repository = mock(RuntimeTargetRepository.class);
        when(repository.count()).thenReturn(0L);
        when(repository.findByName(anyString())).thenReturn(Optional.empty());

        AppProperties properties = new AppProperties(
                new AppProperties.Docker(true, "ai.project.env", "demo", 200),
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
                )),
                null
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

    @Test
    void seedsConfiguredDemoTargetsWhenRepositoryIsEmpty() throws Exception {
        RuntimeTargetRepository repository = mock(RuntimeTargetRepository.class);
        when(repository.count()).thenReturn(0L);
        when(repository.findByName(anyString())).thenReturn(Optional.empty());

        AppProperties properties = new AppProperties(
                new AppProperties.Docker(true, "ai.project.env", "demo", 200),
                null,
                new AppProperties.Runtime(List.of(
                        new AppProperties.LocalTarget(
                                "orders-demo",
                                "localhost",
                                8080,
                                "http://localhost:8080/actuator/health",
                                "FILE_TAIL",
                                "./logs/orders-demo.log"
                        ),
                        new AppProperties.LocalTarget(
                                "restaurant-demo",
                                "localhost",
                                8080,
                                "http://localhost:8080/actuator/health",
                                "FILE_TAIL",
                                "./logs/restaurant-demo.log"
                        ),
                        new AppProperties.LocalTarget(
                                "delivery-demo",
                                "localhost",
                                8080,
                                "http://localhost:8080/actuator/health",
                                "FILE_TAIL",
                                "./logs/delivery-demo.log"
                        )
                )),
                null
        );

        RuntimeTargetBootstrap bootstrap = new RuntimeTargetBootstrap(repository, properties);

        bootstrap.run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<List<RuntimeTargetEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(RuntimeTargetEntity::getName, RuntimeTargetEntity::getLogSourceRef)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("orders-demo", "./logs/orders-demo.log"),
                        org.assertj.core.groups.Tuple.tuple("restaurant-demo", "./logs/restaurant-demo.log"),
                        org.assertj.core.groups.Tuple.tuple("delivery-demo", "./logs/delivery-demo.log")
                );
    }
}
