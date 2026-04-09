package com.yelshod.diagnosticserviceai.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yelshod.diagnosticserviceai.docker.DockerContainerService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DockerContainerService dockerContainerService;

    @Test
    void returnsVisibleProjectContainers() throws Exception {
        when(dockerContainerService.listDemoProjectContainers()).thenReturn(List.of(
                new ProjectContainerDto(
                        "id-1",
                        "orders",
                        "orders:latest",
                        "Up",
                        Instant.parse("2026-04-09T10:00:00Z"),
                        Map.of("env", "demo"))));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].containerId").value("id-1"))
                .andExpect(jsonPath("$[0].name").value("orders"))
                .andExpect(jsonPath("$[0].labels.env").value("demo"));
    }
}
