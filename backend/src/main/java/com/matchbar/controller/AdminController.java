package com.matchbar.controller;

import com.matchbar.dto.response.BarResponse;
import com.matchbar.entity.Bar;
import com.matchbar.service.BarService;
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

    @GetMapping("/bars/pending")
    public ResponseEntity<List<BarResponse>> pending() {
        return ResponseEntity.ok(barService.listPending());
    }

    @PatchMapping("/bars/{id}/approve")
    public ResponseEntity<BarResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(barService.setStatus(id, Bar.Status.APPROVED));
    }

    @PatchMapping("/bars/{id}/reject")
    public ResponseEntity<BarResponse> reject(@PathVariable Long id) {
        return ResponseEntity.ok(barService.setStatus(id, Bar.Status.REJECTED));
    }
}
