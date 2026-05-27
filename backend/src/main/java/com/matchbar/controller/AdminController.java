package com.matchbar.controller;

import com.matchbar.dto.response.BarResponse;
import com.matchbar.dto.response.IncidentResponse;
import com.matchbar.dto.response.PendingBarResponse;
import com.matchbar.entity.Bar;
import com.matchbar.exception.ApiException;
import com.matchbar.repository.BarRepository;
import com.matchbar.service.BarService;
import com.matchbar.service.IncidentService;
import com.matchbar.service.LicenseDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final BarService barService;
    private final IncidentService incidentService;
    private final LicenseDocService licenseDocService;
    private final BarRepository barRepository;

    @GetMapping("/bars/pending")
    public ResponseEntity<List<PendingBarResponse>> pending() {
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

    @GetMapping("/bars/{id}/license")
    public ResponseEntity<InputStreamResource> downloadLicense(@PathVariable String id) throws IOException {
        Bar bar = barRepository.findById(id)
                .orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "Bar no encontrado"));
        if (bar.getLicenseDocFileId() == null) {
            throw new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "Este bar no tiene licencia adjunta");
        }
        var resource = licenseDocService.load(bar.getLicenseDocFileId());
        String filename = bar.getLicenseDocFilename() != null ? bar.getLicenseDocFilename() : "licencia.pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(new InputStreamResource(resource.getInputStream()));
    }

    @GetMapping("/incidents")
    public ResponseEntity<List<IncidentResponse>> listIncidents() {
        return ResponseEntity.ok(incidentService.listAll());
    }

    @PatchMapping("/incidents/{id}/resolve")
    public ResponseEntity<IncidentResponse> resolveIncident(@PathVariable String id) {
        return ResponseEntity.ok(incidentService.resolve(id));
    }

    @DeleteMapping("/incidents/{id}")
    public ResponseEntity<Void> deleteIncident(@PathVariable String id) {
        incidentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
