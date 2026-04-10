package com.yelshod.diagnosticserviceai.runtime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
