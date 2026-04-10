package com.yelshod.diagnosticserviceai.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HttpHealthStatusProbeTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void marksTargetUpWhenHealthEndpointReturns200() throws Exception {
        server = serverReturning(200);
        HttpHealthStatusProbe probe = new HttpHealthStatusProbe();

        assertThat(probe.probe(url())).isEqualTo(RuntimeTargetStatus.UP);
    }

    @Test
    void marksTargetDownWhenHealthEndpointCannotBeReached() {
        HttpHealthStatusProbe probe = new HttpHealthStatusProbe();

        assertThat(probe.probe("http://127.0.0.1:65534/health")).isEqualTo(RuntimeTargetStatus.DOWN);
    }

    private HttpServer serverReturning(int status) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/health", exchange -> {
            exchange.sendResponseHeaders(status, 0);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write("{}".getBytes());
            }
        });
        httpServer.start();
        return httpServer;
    }

    private String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/health";
    }
}
