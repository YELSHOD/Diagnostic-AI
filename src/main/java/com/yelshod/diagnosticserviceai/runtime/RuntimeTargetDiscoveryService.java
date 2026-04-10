package com.yelshod.diagnosticserviceai.runtime;

import java.util.List;

public interface RuntimeTargetDiscoveryService {

    List<RuntimeTargetDto> discover();
}
