package com.yelshod.diagnosticserviceai.docker;

@FunctionalInterface
public interface DockerLogSession extends AutoCloseable {

    @Override
    void close();
}
