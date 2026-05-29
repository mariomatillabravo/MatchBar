package com.matchbar.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "matchbar.football")
@Getter
@Setter
public class FootballProperties {
    private boolean enabled;
    private String apiKey;
    private String baseUrl;
    private List<String> competitionCodes;
    private int syncDaysAhead = 7;
    private long syncIntervalMs = 43200000L;
}
