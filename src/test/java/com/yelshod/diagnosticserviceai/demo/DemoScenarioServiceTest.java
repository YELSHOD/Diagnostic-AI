package com.yelshod.diagnosticserviceai.demo;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import com.yelshod.diagnosticserviceai.config.AppProperties;
import java.nio.file.Path;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.boot.DefaultApplicationArguments;

class DemoScenarioServiceTest {

    @Test
    void startsOrdersScenarioAndWritesExpectedOrderedSteps() throws Exception {
        DemoScenarioWriter writer = mock(DemoScenarioWriter.class);
        AppProperties properties = new AppProperties(
                null,
                null,
                null,
                new AppProperties.Demo(
                        true,
                        false,
                        0L,
                        "./logs/orders-demo.log",
                        "./logs/restaurant-demo.log"
                )
        );
        Executor sameThread = Runnable::run;
        DemoScenarioService service = new DemoScenarioService(properties, writer);
        service.setExecutorForTest(sameThread);

        service.start(DemoScenarioType.ORDERS_HAPPY_PATH);

        InOrder inOrder = inOrder(writer);
        inOrder.verify(writer).append(any(Path.class), argThat(line -> line.message().contains("Order created")));
        inOrder.verify(writer).append(any(Path.class), argThat(line -> line.message().contains("Payment authorized")));
        inOrder.verify(writer).append(any(Path.class), argThat(line -> line.message().contains("Restaurant accepted")));
        inOrder.verify(writer).append(any(Path.class), argThat(line -> line.message().contains("Order delivered")));
    }

    @Test
    void rejectsConcurrentStartForTheSameScenarioRun() {
        DemoScenarioWriter writer = mock(DemoScenarioWriter.class);
        AppProperties properties = new AppProperties(
                null,
                null,
                null,
                new AppProperties.Demo(
                        true,
                        false,
                        0L,
                        "./logs/orders-demo.log",
                        "./logs/restaurant-demo.log"
                )
        );
        Executor neverRuns = command -> { };
        DemoScenarioService service = new DemoScenarioService(properties, writer);
        service.setExecutorForTest(neverRuns);
        service.markRunningForTest();

        assertThatThrownBy(() -> service.start(DemoScenarioType.ORDERS_HAPPY_PATH))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already running");
    }

    @Test
    void autoStartsHappyPathWhenEnabledInDev() throws Exception {
        DemoScenarioService service = mock(DemoScenarioService.class);
        AppProperties properties = new AppProperties(
                null,
                null,
                null,
                new AppProperties.Demo(
                        true,
                        true,
                        0L,
                        "./logs/orders-demo.log",
                        "./logs/restaurant-demo.log"
                )
        );

        new DemoScenarioAutoStarter(properties, service).run(new DefaultApplicationArguments(new String[0]));

        org.mockito.Mockito.verify(service).start(DemoScenarioType.ORDERS_HAPPY_PATH);
    }
}
