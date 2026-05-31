package com.matchbar.controller;

import com.matchbar.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> submit(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam("subject") String subject,
            @RequestParam("message") String message,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos) {
        incidentService.create(principal.getUsername(), subject, message, photos);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
