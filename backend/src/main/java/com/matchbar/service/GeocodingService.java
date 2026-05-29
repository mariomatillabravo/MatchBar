package com.matchbar.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.matchbar.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Convierte una dirección de texto (calle, número, ciudad) en coordenadas
 * geográficas usando Nominatim (OpenStreetMap). Gratuito y sin API key.
 *
 * El usuario final nunca ve latitud/longitud: solo escribe la dirección y
 * aquí se resuelve el punto exacto del mapa.
 */
@Slf4j
@Service
public class GeocodingService {

    private final RestTemplate geocodingRestTemplate;
    private final String baseUrl;
    private final String userAgent;
    private final String countryCodes;

    public GeocodingService(
            RestTemplate geocodingRestTemplate,
            @Value("${matchbar.geocoding.base-url}") String baseUrl,
            @Value("${matchbar.geocoding.user-agent}") String userAgent,
            @Value("${matchbar.geocoding.country-codes:}") String countryCodes) {
        this.geocodingRestTemplate = geocodingRestTemplate;
        this.baseUrl = baseUrl;
        this.userAgent = userAgent;
        this.countryCodes = countryCodes;
    }

    /** Coordenadas resueltas para una dirección. */
    public record Coordinates(double latitude, double longitude) {}

    /**
     * Geocodifica una dirección. Lanza {@link ApiException} (422) si no se
     * encuentra ninguna ubicación, para que el dueño del bar corrija la dirección.
     */
    public Coordinates geocode(String address) {
        if (address == null || address.isBlank()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "La dirección no puede estar vacía");
        }

        UriComponentsBuilder uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("q", address.trim())
                .queryParam("format", "jsonv2")
                .queryParam("limit", 1)
                .queryParam("addressdetails", 0);
        if (countryCodes != null && !countryCodes.isBlank()) {
            uri.queryParam("countrycodes", countryCodes);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, userAgent);
        headers.set(HttpHeaders.ACCEPT_LANGUAGE, "es");

        try {
            ResponseEntity<NominatimResult[]> response = geocodingRestTemplate.exchange(
                    uri.encode().build().toUri(),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    NominatimResult[].class);

            NominatimResult[] results = response.getBody();
            if (results == null || results.length == 0
                    || results[0].lat() == null || results[0].lon() == null) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "No hemos podido localizar esa dirección. Revisa que la calle, el número y la ciudad sean correctos.");
            }
            return new Coordinates(
                    Double.parseDouble(results[0].lat()),
                    Double.parseDouble(results[0].lon()));
        } catch (RestClientException e) {
            log.error("[Geocoding] Error consultando Nominatim para '{}': {}", address, e.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "No se pudo verificar la dirección ahora mismo. Inténtalo de nuevo en unos segundos.");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NominatimResult(String lat, String lon, String display_name) {}
}
