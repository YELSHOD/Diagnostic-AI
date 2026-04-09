package com.yelshod.diagnosticserviceai.logs;

public interface EventAssemblyTimeoutScheduler {

    Cancellable schedule(String service, Runnable action);

    interface Cancellable {
        void cancel();
    }
}
