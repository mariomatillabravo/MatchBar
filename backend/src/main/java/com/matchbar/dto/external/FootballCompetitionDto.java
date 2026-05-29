package com.matchbar.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FootballCompetitionDto(Long id, String name, String code, String emblem) {}
