package com.matchbar.controller;

import com.matchbar.dto.response.MatchResponse;
import com.matchbar.security.UserPrincipal;
import com.matchbar.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @GetMapping
    public ResponseEntity<List<MatchResponse>> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String competitionId,
            @RequestParam(required = false) String teamId) {
        return ResponseEntity.ok(matchService.search(from, to, competitionId, teamId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchResponse> get(@PathVariable String id) {
        return ResponseEntity.ok(matchService.getById(id));
    }

    @PostMapping("/{id}/schedule")
    @PreAuthorize("hasRole('BAR')")
    public ResponseEntity<Void> schedule(@PathVariable String id,
                                         @AuthenticationPrincipal UserPrincipal me) {
        matchService.scheduleBroadcast(me.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/schedule")
    @PreAuthorize("hasRole('BAR')")
    public ResponseEntity<Void> cancel(@PathVariable String id,
                                       @AuthenticationPrincipal UserPrincipal me) {
        matchService.cancelBroadcast(me.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/scheduled")
    @PreAuthorize("hasRole('BAR')")
    public ResponseEntity<List<MatchResponse>> scheduled(@AuthenticationPrincipal UserPrincipal me) {
        return ResponseEntity.ok(matchService.listScheduledByBar(me.getId()));
    }
}
