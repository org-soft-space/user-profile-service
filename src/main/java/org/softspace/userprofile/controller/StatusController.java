package org.softspace.userprofile.controller;

import org.softspace.userprofile.dto.status.StatusResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1")
public class StatusController {

    private final String applicationName;

    public StatusController(
            @Value("${spring.application.name}") String applicationName
    ) {
        this.applicationName = applicationName;
    }

    @GetMapping("/status")
    public ResponseEntity<StatusResponse> getStatus() {

        StatusResponse response = new StatusResponse(applicationName, "UP");

        return ResponseEntity.ok(response);
    }
}
