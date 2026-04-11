package com.yelshod.diagnosticserviceai.runtime;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yelshod.diagnosticserviceai.security.JwtAuthenticationFilter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RuntimeTargetController.class)
@AutoConfigureMockMvc(addFilters = false)
class RuntimeTargetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RuntimeTargetService runtimeTargetService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void returnsUnifiedRuntimeTargets() throws Exception {
        when(runtimeTargetService.listRuntimeTargets()).thenReturn(List.of(
                new RuntimeTargetDto(
                        "docker-orders",
                        "orders",
                        RuntimeTargetType.DOCKER_CONTAINER,
                        RuntimeTargetStatus.UP,
                        "localhost",
                        8081,
                        "http://localhost:8081/actuator/health",
                        LogSourceType.DOCKER,
                        "docker-orders",
                        Map.of("image", "orders:latest"))));

        mockMvc.perform(get("/api/runtime-targets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("docker-orders"))
                .andExpect(jsonPath("$[0].type").value("DOCKER_CONTAINER"))
                .andExpect(jsonPath("$[0].status").value("UP"))
                .andExpect(jsonPath("$[0].logSourceType").value("DOCKER"));
    }

    @Test
    void createsLocalServiceRuntimeTarget() throws Exception {
        when(runtimeTargetService.createLocalService(new UpsertRuntimeTargetRequest(
                "diagnostic-ai-front",
                "localhost",
                5173,
                "http://localhost:5173/actuator/health",
                LogSourceType.FILE_TAIL,
                "/tmp/diagnostic-ai-front.log",
                true))).thenReturn(new RuntimeTargetDto(
                        "local-front",
                        "diagnostic-ai-front",
                        RuntimeTargetType.LOCAL_SERVICE,
                        RuntimeTargetStatus.UNKNOWN,
                        "localhost",
                        5173,
                        "http://localhost:5173/actuator/health",
                        LogSourceType.FILE_TAIL,
                        "/tmp/diagnostic-ai-front.log",
                        Map.of("source", "database")));

        mockMvc.perform(post("/api/runtime-targets")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "diagnostic-ai-front",
                                  "host": "localhost",
                                  "port": 5173,
                                  "healthUrl": "http://localhost:5173/actuator/health",
                                  "logSourceType": "FILE_TAIL",
                                  "logSourceRef": "/tmp/diagnostic-ai-front.log",
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("local-front"))
                .andExpect(jsonPath("$.type").value("LOCAL_SERVICE"))
                .andExpect(jsonPath("$.logSourceType").value("FILE_TAIL"));
    }

    @Test
    void updatesLocalServiceRuntimeTarget() throws Exception {
        when(runtimeTargetService.updateLocalService("local-front", new UpsertRuntimeTargetRequest(
                "diagnostic-ai-front",
                "127.0.0.1",
                5174,
                "http://127.0.0.1:5174/actuator/health",
                LogSourceType.HTTP_INGEST,
                "front-service",
                true))).thenReturn(new RuntimeTargetDto(
                        "local-front",
                        "diagnostic-ai-front",
                        RuntimeTargetType.LOCAL_SERVICE,
                        RuntimeTargetStatus.UNKNOWN,
                        "127.0.0.1",
                        5174,
                        "http://127.0.0.1:5174/actuator/health",
                        LogSourceType.HTTP_INGEST,
                        "front-service",
                        Map.of("source", "database")));

        mockMvc.perform(patch("/api/runtime-targets/local-front")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "diagnostic-ai-front",
                                  "host": "127.0.0.1",
                                  "port": 5174,
                                  "healthUrl": "http://127.0.0.1:5174/actuator/health",
                                  "logSourceType": "HTTP_INGEST",
                                  "logSourceRef": "front-service",
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("local-front"))
                .andExpect(jsonPath("$.port").value(5174))
                .andExpect(jsonPath("$.logSourceType").value("HTTP_INGEST"));
    }

    @Test
    void deletesLocalServiceRuntimeTarget() throws Exception {
        mockMvc.perform(delete("/api/runtime-targets/local-front"))
                .andExpect(status().isNoContent());
    }
}
