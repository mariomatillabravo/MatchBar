package com.matchbar.controller;

import com.matchbar.dto.request.BarAdminUpdateRequest;
import com.matchbar.dto.request.UserAdminUpdateRequest;
import com.matchbar.dto.response.BarAdminResponse;
import com.matchbar.dto.response.BarResponse;
import com.matchbar.dto.response.IncidentResponse;
import com.matchbar.dto.response.PendingBarResponse;
import com.matchbar.dto.response.UserAdminResponse;
import com.matchbar.entity.Bar;
import com.matchbar.security.UserPrincipal;
import com.matchbar.service.BarService;
import com.matchbar.service.IncidentService;
import com.matchbar.service.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final BarService barService;
    private final IncidentService incidentService;
    private final UserAdminService userAdminService;

    @GetMapping("/bars/pending")
    public ResponseEntity<List<PendingBarResponse>> pending() {
        return ResponseEntity.ok(barService.listPending());
    }

    @GetMapping("/bars/stats")
    public ResponseEntity<Map<String, Long>> stats() {
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
        BarService.LicenseFile license = barService.loadLicense(id);
        // El nombre lo eligió quien subió el fichero: ContentDisposition lo
        // codifica (RFC 5987) para que comillas o saltos de línea no rompan la cabecera.
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(license.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new InputStreamResource(license.resource().getInputStream()));
    }

    @GetMapping("/bars/all")
    public ResponseEntity<List<BarAdminResponse>> allBars() {
        return ResponseEntity.ok(barService.listAll());
    }

    @GetMapping("/bars/{id}")
    public ResponseEntity<BarAdminResponse> getBar(@PathVariable String id) {
        return ResponseEntity.ok(barService.getAdminDetail(id));
    }

    @PutMapping("/bars/{id}")
    public ResponseEntity<BarAdminResponse> updateBar(@PathVariable String id, @Valid @RequestBody BarAdminUpdateRequest req) {
        return ResponseEntity.ok(barService.adminUpdate(id, req));
    }

    @DeleteMapping("/bars/{id}")
    public ResponseEntity<Void> deleteBar(@PathVariable String id) {
        barService.deleteBar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bars/{barId}/photos/{fileId}")
    public ResponseEntity<BarAdminResponse> deleteBarPhoto(@PathVariable String barId, @PathVariable String fileId) {
        return ResponseEntity.ok(barService.adminRemovePhoto(barId, fileId));
    }

    @DeleteMapping("/bars/{barId}/menu/{fileId}")
    public ResponseEntity<BarAdminResponse> deleteBarMenu(@PathVariable String barId, @PathVariable String fileId) {
        return ResponseEntity.ok(barService.adminRemoveMenu(barId, fileId));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserAdminResponse>> allUsers() {
        return ResponseEntity.ok(userAdminService.listUsers());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserAdminResponse> getUser(@PathVariable String id) {
        return ResponseEntity.ok(userAdminService.get(id));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserAdminResponse> updateUser(@PathVariable String id,
                                                        @Valid @RequestBody UserAdminUpdateRequest req,
                                                        @AuthenticationPrincipal UserPrincipal me) {
        return ResponseEntity.ok(userAdminService.update(id, req, me.getId()));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id, @AuthenticationPrincipal UserPrincipal me) {
        userAdminService.delete(id, me.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/incidents")
    public ResponseEntity<List<IncidentResponse>> listIncidents() {
        return ResponseEntity.ok(incidentService.listAll());
    }

    @GetMapping("/incidents/{id}/photos/{fileId}")
    public ResponseEntity<InputStreamResource> incidentPhoto(@PathVariable String id,
                                                             @PathVariable String fileId) throws IOException {
        return ImageResponses.of(incidentService.loadPhoto(id, fileId));
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
