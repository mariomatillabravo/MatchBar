package com.matchbar.service;

import com.matchbar.dto.response.MatchResponse;
import com.matchbar.entity.Broadcast;
import com.matchbar.entity.Match;
import com.matchbar.entity.Bar;
import com.matchbar.exception.ApiException;
import com.matchbar.repository.BarRepository;
import com.matchbar.repository.BroadcastRepository;
import com.matchbar.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final BroadcastRepository broadcastRepository;
    private final BarRepository barRepository;

    @Transactional(readOnly = true)
    public List<MatchResponse> search(Instant from, Instant to, Long competitionId, Long teamId) {
        return matchRepository.search(from, to, competitionId, teamId).stream()
                .map(MatchResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MatchResponse getById(Long id) {
        Match m = matchRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Partido no encontrado"));
        return MatchResponse.from(m);
    }

    @Transactional
    public void scheduleBroadcast(Long userId, Long matchId) {
        Bar bar = barRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No tienes ficha de bar creada"));
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Partido no encontrado"));
        if (broadcastRepository.existsByBarIdAndMatchId(bar.getId(), matchId)) {
            return; // ya programado, idempotente
        }
        broadcastRepository.save(Broadcast.builder().bar(bar).match(match).build());
    }

    @Transactional
    public void cancelBroadcast(Long userId, Long matchId) {
        Bar bar = barRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No tienes ficha de bar creada"));
        broadcastRepository.deleteByBarIdAndMatchId(bar.getId(), matchId);
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> listScheduledByBar(Long userId) {
        Bar bar = barRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No tienes ficha de bar creada"));
        return broadcastRepository.findByBarId(bar.getId()).stream()
                .map(b -> MatchResponse.from(b.getMatch()))
                .collect(Collectors.toList());
    }
}
