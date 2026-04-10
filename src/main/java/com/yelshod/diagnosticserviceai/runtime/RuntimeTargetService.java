package com.yelshod.diagnosticserviceai.runtime;

import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
