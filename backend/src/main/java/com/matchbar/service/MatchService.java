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
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final BroadcastRepository broadcastRepository;
    private final BarRepository barRepository;
    private final MongoTemplate mongoTemplate;

    public List<MatchResponse> search(Instant from, Instant to, String competitionId, String teamId) {
        List<Criteria> parts = new ArrayList<>();
        if (from != null) parts.add(Criteria.where("kickoffAt").gte(from));
        if (to != null) parts.add(Criteria.where("kickoffAt").lte(to));
        if (competitionId != null) parts.add(Criteria.where("competition.$id").is(new ObjectId(competitionId)));
        if (teamId != null) {
            ObjectId tid = new ObjectId(teamId);
            parts.add(new Criteria().orOperator(
                    Criteria.where("homeTeam.$id").is(tid),
                    Criteria.where("awayTeam.$id").is(tid)
            ));
        }

        Query query = new Query();
        if (!parts.isEmpty()) query.addCriteria(new Criteria().andOperator(parts.toArray(new Criteria[0])));
        query.with(Sort.by("kickoffAt").ascending());

        return mongoTemplate.find(query, Match.class).stream()
                .map(MatchResponse::from)
                .collect(Collectors.toList());
    }

    public MatchResponse getById(String id) {
        Match m = matchRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Partido no encontrado"));
        return MatchResponse.from(m);
    }

    public void scheduleBroadcast(String userId, String matchId) {
        Bar bar = barRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No tienes ficha de bar creada"));
        matchRepository.findById(matchId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Partido no encontrado"));
        if (broadcastRepository.existsByBarIdAndMatchId(bar.getId(), matchId)) return;
        broadcastRepository.save(Broadcast.builder().barId(bar.getId()).matchId(matchId).build());
    }

    public void cancelBroadcast(String userId, String matchId) {
        Bar bar = barRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No tienes ficha de bar creada"));
        broadcastRepository.deleteByBarIdAndMatchId(bar.getId(), matchId);
    }

    public List<MatchResponse> listScheduledByBar(String userId) {
        Bar bar = barRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No tienes ficha de bar creada"));
        List<String> matchIds = broadcastRepository.findByBarId(bar.getId()).stream()
                .map(Broadcast::getMatchId)
                .collect(Collectors.toList());
        if (matchIds.isEmpty()) return List.of();
        return matchRepository.findAllById(matchIds).stream()
                .map(MatchResponse::from)
                .collect(Collectors.toList());
    }
}
