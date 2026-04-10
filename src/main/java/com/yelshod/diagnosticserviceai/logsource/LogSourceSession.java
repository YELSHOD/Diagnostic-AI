package com.yelshod.diagnosticserviceai.logsource;

@FunctionalInterface
public interface LogSourceSession extends AutoCloseable {

    @Override
    void close();
}
