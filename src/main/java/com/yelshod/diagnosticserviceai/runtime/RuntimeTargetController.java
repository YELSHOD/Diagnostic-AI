package com.yelshod.diagnosticserviceai.runtime;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/runtime-targets")
@RequiredArgsConstructor
public class RuntimeTargetController {

    private final RuntimeTargetService runtimeTargetService;

    @GetMapping
    public List<RuntimeTargetDto> runtimeTargets() {
        return runtimeTargetService.listRuntimeTargets();
    }
}
