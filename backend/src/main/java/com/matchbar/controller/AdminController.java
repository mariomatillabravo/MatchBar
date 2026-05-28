package com.matchbar.controller;

import com.matchbar.dto.request.BarAdminUpdateRequest;
import com.matchbar.dto.request.UserAdminUpdateRequest;
import com.matchbar.dto.response.BarAdminResponse;
import com.matchbar.dto.response.BarResponse;
import com.matchbar.dto.response.IncidentResponse;
import com.matchbar.dto.response.PendingBarResponse;
import com.matchbar.dto.response.UserAdminResponse;
import com.matchbar.entity.Bar;
import com.matchbar.entity.User;
import com.matchbar.exception.ApiException;
import com.matchbar.repository.BarRepository;
import com.matchbar.repository.UserRepository;
import com.matchbar.service.BarService;
import com.matchbar.service.IncidentService;
import com.matchbar.service.LicenseDocService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final BarService barService;
    private final IncidentService incidentService;
    private final LicenseDocService licenseDocService;
    private final BarRepository barRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bar no encontrado"));
        if (bar.getLicenseDocFileId() == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Este bar no tiene licencia adjunta");
        }
        var resource = licenseDocService.load(bar.getLicenseDocFileId());
        String filename = bar.getLicenseDocFilename() != null ? bar.getLicenseDocFilename() : "licencia.pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(new InputStreamResource(resource.getInputStream()));
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
        barService.adminDelete(id, licenseDocService);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserAdminResponse>> allUsers() {
        List<UserAdminResponse> users = userRepository.findAll().stream()
                .filter(u -> u.getRole() != User.Role.BAR)
                .sorted(Comparator.comparing(User::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(u -> new UserAdminResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getCreatedAt()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserAdminResponse> getUser(@PathVariable String id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return ResponseEntity.ok(new UserAdminResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getCreatedAt()));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserAdminResponse> updateUser(@PathVariable String id, @Valid @RequestBody UserAdminUpdateRequest req) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (!u.getEmail().equalsIgnoreCase(req.email()) && userRepository.existsByEmail(req.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un usuario con ese email");
        }
        u.setName(req.name());
        u.setEmail(req.email());
        u.setRole(req.role());
        if (req.password() != null && !req.password().isBlank()) {
            u.setPassword(passwordEncoder.encode(req.password()));
        }
        u = userRepository.save(u);
        return ResponseEntity.ok(new UserAdminResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getCreatedAt()));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        if (!userRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
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
