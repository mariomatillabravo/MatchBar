package com.matchbar.controller;

import com.matchbar.dto.request.IncidentRequest;
import com.matchbar.dto.response.IncidentResponse;
import com.matchbar.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping
    public ResponseEntity<IncidentResponse> submit(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody IncidentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incidentService.create(principal.getUsername(), req));
    }
}
