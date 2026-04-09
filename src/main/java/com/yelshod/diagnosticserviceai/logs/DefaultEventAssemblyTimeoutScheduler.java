package com.yelshod.diagnosticserviceai.logs;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class DefaultEventAssemblyTimeoutScheduler implements EventAssemblyTimeoutScheduler {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "event-assembler-timeout");
        t.setDaemon(true);
        return t;
    });

    @Override
    public Cancellable schedule(String service, Runnable action) {
        var future = scheduler.schedule(action, 1500, TimeUnit.MILLISECONDS);
        return () -> future.cancel(false);
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }
}
