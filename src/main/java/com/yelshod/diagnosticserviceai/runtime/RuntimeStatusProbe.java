package com.yelshod.diagnosticserviceai.runtime;

public interface RuntimeStatusProbe {

    RuntimeTargetStatus probe(String healthUrl);
}
