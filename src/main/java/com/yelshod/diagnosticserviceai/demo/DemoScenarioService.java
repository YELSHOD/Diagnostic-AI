package com.yelshod.diagnosticserviceai.demo;

import com.yelshod.diagnosticserviceai.config.AppProperties;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;

@Service
public class DemoScenarioService {

    private final AppProperties.Demo properties;
    private final DemoScenarioWriter writer;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Executor executor = Runnable::run;

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
                for (DemoScenarioLine line : linesFor(type)) {
                    writer.append(resolvePath(line.service()), line);
                    if (properties.stepDelayMs() > 0) {
                        Thread.sleep(properties.stepDelayMs());
                    }
                }
            } catch (IOException | InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Unable to write demo scenario", ex);
            } finally {
                running.set(false);
            }
        });
    }

    void markRunningForTest() {
        running.set(true);
    }

    void setExecutorForTest(Executor executor) {
        this.executor = executor;
    }

    private Path resolvePath(String service) {
        if ("restaurant-demo".equals(service)) {
            return Path.of(properties.restaurantLogFile());
        }
        return Path.of(properties.ordersLogFile());
    }

    private List<DemoScenarioLine> linesFor(DemoScenarioType type) {
        Instant now = Instant.parse("2026-04-13T06:30:00Z");
        return switch (type) {
            case ORDERS_HAPPY_PATH -> List.of(
                    line(now, "INFO", "orders-demo",
                            "Order created orderId=ORD-20260413-1001 customer=\"Aruzhan S.\" phone=\"+7 700 *** 12 34\" restaurant=\"Tokyo Bowl\" amount=8450"),
                    line(now.plusSeconds(2), "INFO", "orders-demo",
                            "Payment authorized orderId=ORD-20260413-1001 paymentId=PAY-88421 method=CARD"),
                    line(now.plusSeconds(4), "INFO", "restaurant-demo",
                            "Restaurant accepted orderId=ORD-20260413-1001 etaMinutes=24"),
                    line(now.plusSeconds(6), "INFO", "restaurant-demo",
                            "Kitchen started preparation orderId=ORD-20260413-1001 station=HOT"),
                    line(now.plusSeconds(8), "INFO", "restaurant-demo",
                            "Kitchen marked order ready orderId=ORD-20260413-1001"),
                    line(now.plusSeconds(10), "INFO", "orders-demo",
                            "Courier assigned orderId=ORD-20260413-1001 courier=\"Nurlan T.\""),
                    line(now.plusSeconds(12), "INFO", "orders-demo",
                            "Order delivered orderId=ORD-20260413-1001 courier=\"Nurlan T.\"")
            );
            case ORDERS_PAYMENT_DELAY -> List.of(
                    line(now, "INFO", "orders-demo",
                            "Order created orderId=ORD-20260413-2001 customer=\"Amina K.\" phone=\"+7 701 *** 44 55\" restaurant=\"Neo Pizza\" amount=6290"),
                    line(now.plusSeconds(2), "WARN", "orders-demo",
                            "Payment confirmation delayed orderId=ORD-20260413-2001 gateway=KaspiPay retry=1"),
                    line(now.plusSeconds(4), "INFO", "orders-demo",
                            "Payment authorized orderId=ORD-20260413-2001 paymentId=PAY-99114 method=CARD"),
                    line(now.plusSeconds(6), "INFO", "restaurant-demo",
                            "Restaurant accepted orderId=ORD-20260413-2001 etaMinutes=19")
            );
            case RESTAURANT_PREP_DELAY -> List.of(
                    line(now, "INFO", "orders-demo",
                            "Order created orderId=ORD-20260413-3001 customer=\"Dias R.\" phone=\"+7 705 *** 90 11\" restaurant=\"Chef Plov\" amount=7120"),
                    line(now.plusSeconds(2), "INFO", "orders-demo",
                            "Payment authorized orderId=ORD-20260413-3001 paymentId=PAY-44001 method=CARD"),
                    line(now.plusSeconds(4), "INFO", "restaurant-demo",
                            "Restaurant accepted orderId=ORD-20260413-3001 etaMinutes=31"),
                    line(now.plusSeconds(6), "WARN", "restaurant-demo",
                            "Kitchen load elevated restaurantId=RST-14 activeOrders=18"),
                    line(now.plusSeconds(8), "WARN", "restaurant-demo",
                            "Preparation delayed orderId=ORD-20260413-3001 reason=high-load extraMinutes=12")
            );
        };
    }

    private DemoScenarioLine line(Instant timestamp, String level, String service, String message) {
        return new DemoScenarioLine(timestamp, level, service, message);
    }
}
