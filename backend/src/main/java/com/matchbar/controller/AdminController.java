package com.matchbar.controller;

import com.matchbar.dto.response.BarResponse;
import com.matchbar.dto.response.IncidentResponse;
import com.matchbar.entity.Bar;
import com.matchbar.service.BarService;
import com.matchbar.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final BarService barService;
    private final IncidentService incidentService;

    @GetMapping("/bars/pending")
    public ResponseEntity<List<BarResponse>> pending() {
        return ResponseEntity.ok(barService.listPending());
    }

    @GetMapping("/bars/stats")
    public ResponseEntity<java.util.Map<String, Long>> stats() {
        return ResponseEntity.ok(barService.getStatusStats());
    }

    @PatchMapping("/bars/{id}/approve")
    public ResponseEntity<BarResponse> approve(@PathVariable String id) {
        return ResponseEntity.ok(barService.setStatus(id, Bar.Status.APPROVED));
    }

    @PatchMapping("/bars/{id}/reject")
    public ResponseEntity<BarResponse> reject(@PathVariable String id) {
        return ResponseEntity.ok(barService.setStatus(id, Bar.Status.REJECTED));
    }

    @GetMapping("/incidents")
    public ResponseEntity<List<IncidentResponse>> listIncidents() {
        return ResponseEntity.ok(incidentService.listAll());
    }

    @PatchMapping("/incidents/{id}/resolve")
    public ResponseEntity<IncidentResponse> resolveIncident(@PathVariable String id) {
        return ResponseEntity.ok(incidentService.resolve(id));
    }
}
