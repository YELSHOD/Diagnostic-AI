package com.yelshod.diagnosticserviceai.demo;

import com.yelshod.diagnosticserviceai.config.AppProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoScenarioAutoStarter implements ApplicationRunner {

    private final AppProperties appProperties;
    private final DemoScenarioService demoScenarioService;

    public DemoScenarioAutoStarter(AppProperties appProperties, DemoScenarioService demoScenarioService) {
        this.appProperties = appProperties;
        this.demoScenarioService = demoScenarioService;
    }

    @Override
    public void run(ApplicationArguments args) {
        AppProperties.Demo demo = appProperties.demo();
        if (demo != null && demo.enabled() && demo.autoStart()) {
            demoScenarioService.start(DemoScenarioType.ORDERS_HAPPY_PATH);
        }
    }
}
