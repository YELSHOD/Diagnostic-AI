package com.yelshod.diagnosticserviceai.runtime;

import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RuntimeTargetService {

    private final List<RuntimeTargetDiscoveryService> discoveryServices;

    public List<RuntimeTargetDto> listRuntimeTargets() {
        return discoveryServices.stream()
                .flatMap(service -> service.discover().stream())
                .sorted(Comparator.comparing(RuntimeTargetDto::id))
                .toList();
    }

    public RuntimeTargetDto findRequiredTarget(String runtimeTargetId) {
        return listRuntimeTargets().stream()
                .filter(target -> target.id().equals(runtimeTargetId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Runtime target not found: " + runtimeTargetId));
    }
}
