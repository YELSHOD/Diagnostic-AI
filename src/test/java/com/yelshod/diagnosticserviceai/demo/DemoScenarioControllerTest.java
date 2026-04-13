package com.yelshod.diagnosticserviceai.demo;

import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yelshod.diagnosticserviceai.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DemoScenarioController.class)
@AutoConfigureMockMvc(addFilters = false)
class DemoScenarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DemoScenarioService demoScenarioService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void startsRequestedDemoScenario() throws Exception {
        mockMvc.perform(post("/api/demo/scenarios/orders/start")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "variant": "ORDERS_PAYMENT_DELAY"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("started"));

        verify(demoScenarioService).start(DemoScenarioType.ORDERS_PAYMENT_DELAY);
    }

    @Test
    void stopsRunningDemoScenarioStream() throws Exception {
        mockMvc.perform(post("/api/demo/scenarios/stop"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("stopped"));

        verify(demoScenarioService).stop();
    }
}
