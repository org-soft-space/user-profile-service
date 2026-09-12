package org.softspace.userprofile.controller.integration;

import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.softspace.userprofile.controller.StatusController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(StatusController.class)
@ActiveProfiles("test")
class StatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Tracer tracer;


    @Test
    void getStatusTest() throws Exception {
        mockMvc.perform(get("/api/v1/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("user-profile-service"))
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
