package com.matchbar.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FootballMatchesResponse(List<FootballMatchDto> matches) {}
