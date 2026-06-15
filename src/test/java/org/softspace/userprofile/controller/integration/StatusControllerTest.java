package org.softspace.userprofile.controller.integration;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.softspace.userprofile.dto.StatusResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class StatusControllerTest {

    @Autowired
    private TestRestTemplate testTemplate;


    @Test
    void getStatusTest() {
        ResponseEntity<StatusResponse> response = testTemplate.getForEntity(
                "/api/v1/status",
                StatusResponse.class
        );


        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        StatusResponse responseBody = response.getBody();

        Assertions.assertThat(responseBody).isNotNull();
        Assertions.assertThat(responseBody.name()).isEqualTo("user-profile-service");
        Assertions.assertThat(responseBody.status()).isEqualTo("UP");
    }
}
