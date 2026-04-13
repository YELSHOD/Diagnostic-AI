package com.yelshod.diagnosticserviceai.demo;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo/scenarios")
@RequiredArgsConstructor
public class DemoScenarioController {

    private final DemoScenarioService demoScenarioService;

    @PostMapping("/orders/start")
    ResponseEntity<Map<String, String>> startOrdersScenario(@RequestBody(required = false) DemoScenarioRequest request) {
        DemoScenarioType scenarioType = request == null || request.variant() == null || request.variant().isBlank()
                ? DemoScenarioType.ORDERS_HAPPY_PATH
                : DemoScenarioType.valueOf(request.variant());
        demoScenarioService.start(scenarioType);
        return ResponseEntity.accepted().body(Map.of("status", "started"));
    }
}
