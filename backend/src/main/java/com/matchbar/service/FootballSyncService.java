package com.matchbar.service;

import com.matchbar.config.FootballProperties;
import com.matchbar.dto.external.*;
import com.matchbar.entity.Competition;
import com.matchbar.entity.Match;
import com.matchbar.entity.Team;
import com.matchbar.repository.CompetitionRepository;
import com.matchbar.repository.MatchRepository;
import com.matchbar.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class FootballSyncService {

    private final FootballProperties props;
    private final CompetitionRepository competitionRepository;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final RestTemplate footballRestTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        if (!props.isEnabled()) {
            log.info("[FootballSync] Deshabilitado. Se usaran datos del DataSeeder.");
            return;
        }
        syncAll();
    }

    @Scheduled(fixedRateString = "${matchbar.football.sync-interval-ms:43200000}",
               initialDelayString = "${matchbar.football.sync-interval-ms:43200000}")
    public void syncScheduled() {
        if (!props.isEnabled()) return;
        syncAll();
    }

    private static final int MAX_DAYS_PER_REQUEST = 10;

    private void syncAll() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        int totalDays = props.getSyncDaysAhead();

        log.info("[FootballSync] Sincronizando partidos de los proximos {} dias", totalDays);

        for (String code : props.getCompetitionCodes()) {
            LocalDate chunkStart = today;
            while (!chunkStart.isAfter(today.plusDays(totalDays - 1))) {
                LocalDate chunkEnd = chunkStart.plusDays(MAX_DAYS_PER_REQUEST);
                LocalDate limit = today.plusDays(totalDays);
                if (chunkEnd.isAfter(limit)) chunkEnd = limit;

                String dateFrom = chunkStart.format(DateTimeFormatter.ISO_LOCAL_DATE);
                String dateTo = chunkEnd.format(DateTimeFormatter.ISO_LOCAL_DATE);

                try {
                    syncCompetition(code, dateFrom, dateTo);
                } catch (Exception e) {
                    log.error("[FootballSync] Error al sincronizar {} ({} - {}): {}",
                            code, dateFrom, dateTo, e.getMessage());
                }

                try {
                    Thread.sleep(7000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }

                chunkStart = chunkEnd.plusDays(1);
            }
        }
    }

    private void syncCompetition(String code, String dateFrom, String dateTo) {
        String url = props.getBaseUrl()
                + "/competitions/" + code
                + "/matches?dateFrom=" + dateFrom
                + "&dateTo=" + dateTo
                + "&status=SCHEDULED";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Token", props.getApiKey());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<FootballMatchesResponse> response =
                footballRestTemplate.exchange(url, HttpMethod.GET, entity, FootballMatchesResponse.class);

        if (response.getBody() == null || response.getBody().matches() == null) {
            log.warn("[FootballSync] Respuesta vacia para {}", code);
            return;
        }

        int count = 0;
        for (FootballMatchDto dto : response.getBody().matches()) {
            upsertMatch(dto);
            count++;
        }
        log.info("[FootballSync] {} partidos sincronizados para {}", count, code);
    }

    private void upsertMatch(FootballMatchDto dto) {
        Competition comp = upsertCompetition(dto.competition());
        Team home = upsertTeam(dto.homeTeam(), comp.getId());
        Team away = upsertTeam(dto.awayTeam(), comp.getId());

        String extId = String.valueOf(dto.id());
        Match match = matchRepository.findByExternalId(extId)
                .orElse(Match.builder().externalId(extId).build());

        match.setCompetition(comp);
        match.setHomeTeam(home);
        match.setAwayTeam(away);
        match.setKickoffAt(dto.utcDate());
        match.setStatus(dto.status());

        matchRepository.save(match);
    }

    private Competition upsertCompetition(FootballCompetitionDto dto) {
        String extId = dto.code();
        return competitionRepository.findByExternalId(extId)
                .map(c -> {
                    c.setName(dto.name());
                    c.setLogoUrl(dto.emblem());
                    return competitionRepository.save(c);
                })
                .orElseGet(() -> competitionRepository.save(
                        Competition.builder()
                                .externalId(extId)
                                .name(dto.name())
                                .country(resolveCountry(extId))
                                .logoUrl(dto.emblem())
                                .build()
                ));
    }

    private Team upsertTeam(FootballTeamDto dto, String competitionId) {
        if (dto == null || dto.id() == null) return null;
        String extId = String.valueOf(dto.id());
        return teamRepository.findByExternalId(extId)
                .map(t -> {
                    t.setName(dto.name());
                    t.setLogoUrl(dto.crest());
                    return teamRepository.save(t);
                })
                .orElseGet(() -> teamRepository.save(
                        Team.builder()
                                .externalId(extId)
                                .name(dto.name())
                                .logoUrl(dto.crest())
                                .competitionId(competitionId)
                                .build()
                ));
    }

    private String resolveCountry(String code) {
        return switch (code) {
            case "PL"  -> "Inglaterra";
            case "PD"  -> "España";
            case "CL"  -> "Europa";
            case "BL1" -> "Alemania";
            case "SA"  -> "Italia";
            case "WC"  -> "Mundial";
            default    -> "Internacional";
        };
    }
}
