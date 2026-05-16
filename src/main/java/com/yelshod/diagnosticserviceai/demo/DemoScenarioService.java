package com.yelshod.diagnosticserviceai.demo;

import com.yelshod.diagnosticserviceai.config.AppProperties;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class DemoScenarioService {

    private final AppProperties.Demo properties;
    private final DemoScenarioWriter writer;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger orderSequence = new AtomicInteger(1000);
    private Executor executor = Runnable::run;
    private int maxBatchesForTest = Integer.MAX_VALUE;

    public DemoScenarioService(AppProperties appProperties, DemoScenarioWriter writer) {
        this.properties = appProperties.demo();
        this.writer = writer;
    }

    public void start(DemoScenarioType type) {
        if (properties == null || !properties.enabled()) {
            throw new IllegalStateException("Demo scenarios are disabled");
        }
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Demo scenario is already running");
        }

        executor.execute(() -> {
            try {
                int emittedBatches = 0;
                while (running.get() && emittedBatches < maxBatchesForTest) {
                    for (DemoScenarioLine line : linesFor(type, orderSequence.incrementAndGet())) {
                        if (!running.get()) {
                            break;
                        }
                        writer.append(resolvePath(line.service()), line);
                        if (properties.stepDelayMs() > 0) {
                            Thread.sleep(properties.stepDelayMs());
                        }
                    }
                    emittedBatches++;
                }
            } catch (IOException | InterruptedException ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new IllegalStateException("Unable to write demo scenario", ex);
            } finally {
                running.set(false);
            }
        });
    }

    public void stop() {
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }

    void markRunningForTest() {
        running.set(true);
    }

    void setExecutorForTest(Executor executor) {
        this.executor = executor;
    }

    void setMaxBatchesForTest(int maxBatchesForTest) {
        this.maxBatchesForTest = maxBatchesForTest;
    }

    private Path resolvePath(String service) {
        if ("restaurant-demo".equals(service)) {
            return Path.of(properties.restaurantLogFile());
        }
        if ("delivery-demo".equals(service)) {
            return Path.of(properties.deliveryLogFile());
        }
        return Path.of(properties.ordersLogFile());
    }

    private List<DemoScenarioLine> linesFor(DemoScenarioType type, int sequence) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String orderId = "ORD-20260413-" + sequence;
        return switch (type) {
            case ORDERS_HAPPY_PATH -> List.of(
                    line(now, "INFO", "orders-demo",
                            "Order created orderId=%s customer=\"Aruzhan S.\" phone=\"+7 700 *** 12 34\" restaurant=\"Tokyo Bowl\" amount=8450".formatted(orderId)),
                    line(now.plusSeconds(2), "INFO", "orders-demo",
                            "Payment authorized orderId=%s paymentId=PAY-%d method=CARD".formatted(orderId, 88000 + sequence)),
                    line(now.plusSeconds(4), "INFO", "restaurant-demo",
                            "Restaurant accepted orderId=%s etaMinutes=24".formatted(orderId)),
                    line(now.plusSeconds(6), "INFO", "restaurant-demo",
                            "Kitchen started preparation orderId=%s station=HOT".formatted(orderId)),
                    line(now.plusSeconds(8), "INFO", "restaurant-demo",
                            "Kitchen marked order ready orderId=%s".formatted(orderId)),
                    line(now.plusSeconds(10), "INFO", "orders-demo",
                            "Courier assigned orderId=%s courier=\"Nurlan T.\"".formatted(orderId)),
                    line(now.plusSeconds(11), "INFO", "delivery-demo",
                            "Courier picked up orderId=%s courier=\"Nurlan T.\" distanceKm=3.8".formatted(orderId)),
                    line(now.plusSeconds(12), "INFO", "delivery-demo",
                            "Order delivered orderId=%s courier=\"Nurlan T.\"".formatted(orderId))
            );
            case ORDERS_PAYMENT_DELAY -> List.of(
                    line(now, "INFO", "orders-demo",
                            "Order created orderId=%s customer=\"Amina K.\" phone=\"+7 701 *** 44 55\" restaurant=\"Neo Pizza\" amount=6290".formatted(orderId)),
                    line(now.plusSeconds(2), "WARN", "orders-demo",
                            "Payment confirmation delayed orderId=%s gateway=KaspiPay retry=1".formatted(orderId)),
                    line(now.plusSeconds(4), "INFO", "orders-demo",
                            "Payment authorized orderId=%s paymentId=PAY-%d method=CARD".formatted(orderId, 99000 + sequence)),
                    line(now.plusSeconds(6), "INFO", "restaurant-demo",
                            "Restaurant accepted orderId=%s etaMinutes=19".formatted(orderId)),
                    line(now.plusSeconds(8), "INFO", "delivery-demo",
                            "Courier pool checked orderId=%s availableCouriers=4".formatted(orderId))
            );
            case RESTAURANT_PREP_DELAY -> List.of(
                    line(now, "INFO", "orders-demo",
                            "Order created orderId=%s customer=\"Dias R.\" phone=\"+7 705 *** 90 11\" restaurant=\"Chef Plov\" amount=7120".formatted(orderId)),
                    line(now.plusSeconds(2), "INFO", "orders-demo",
                            "Payment authorized orderId=%s paymentId=PAY-%d method=CARD".formatted(orderId, 44000 + sequence)),
                    line(now.plusSeconds(4), "INFO", "restaurant-demo",
                            "Restaurant accepted orderId=%s etaMinutes=31".formatted(orderId)),
                    line(now.plusSeconds(6), "WARN", "restaurant-demo",
                            "Kitchen load elevated restaurantId=RST-14 activeOrders=18"),
                    line(now.plusSeconds(8), "WARN", "restaurant-demo",
                            "Preparation delayed orderId=%s reason=high-load extraMinutes=12".formatted(orderId)),
                    line(now.plusSeconds(10), "WARN", "delivery-demo",
                            "Courier assignment waiting orderId=%s reason=restaurant-not-ready".formatted(orderId))
            );
        };
    }

    private DemoScenarioLine line(Instant timestamp, String level, String service, String message) {
        return new DemoScenarioLine(timestamp, level, service, message);
    }
}
