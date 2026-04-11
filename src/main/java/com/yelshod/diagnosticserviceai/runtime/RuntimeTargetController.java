package com.yelshod.diagnosticserviceai.runtime;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RuntimeTargetDto createLocalService(@Valid @RequestBody UpsertRuntimeTargetRequest request) {
        return runtimeTargetService.createLocalService(request);
    }

    @PatchMapping("/{runtimeTargetId}")
    public RuntimeTargetDto updateLocalService(
            @PathVariable String runtimeTargetId,
            @Valid @RequestBody UpsertRuntimeTargetRequest request
    ) {
        return runtimeTargetService.updateLocalService(runtimeTargetId, request);
    }

    @DeleteMapping("/{runtimeTargetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLocalService(@PathVariable String runtimeTargetId) {
        runtimeTargetService.deleteLocalService(runtimeTargetId);
    }
}
