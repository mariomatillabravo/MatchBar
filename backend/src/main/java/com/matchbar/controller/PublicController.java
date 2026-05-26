package com.matchbar.controller;

import com.matchbar.entity.Competition;
import com.matchbar.entity.Team;
import com.matchbar.repository.CompetitionRepository;
import com.matchbar.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final CompetitionRepository competitionRepository;
    private final TeamRepository teamRepository;

    @GetMapping("/competitions")
    public ResponseEntity<List<Competition>> competitions() {
        return ResponseEntity.ok(competitionRepository.findAll());
    }

    @GetMapping("/teams")
    public ResponseEntity<List<Team>> teams(@RequestParam(required = false) String competitionId) {
        return ResponseEntity.ok(competitionId != null
                ? teamRepository.findByCompetitionId(competitionId)
                : teamRepository.findAll());
    }
}
