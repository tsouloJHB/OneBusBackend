package com.backend.onebus.service.geocoding;

import com.backend.onebus.dto.GeocodingResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * OpenStreetMap Nominatim implementation of GeocodingProvider
 * Free and open-source geocoding service
 * Uses database caching to reduce API calls and improve performance
 */
@Slf4j
@Service("nominatimGeocodingProvider")
public class NominatimGeocodingProvider implements GeocodingProvider {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final GeocodingCacheService cacheService;
    
    @Value("${geocoding.nominatim.base-url:https://nominatim.openstreetmap.org}")
    private String baseUrl;
    
    @Value("${geocoding.nominatim.email:admin@onebus.com}")
    private String email;
    
    public NominatimGeocodingProvider(RestTemplate restTemplate, ObjectMapper objectMapper, GeocodingCacheService cacheService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.cacheService = cacheService;
    }
    
    @Override
    public List<GeocodingResult> geocode(String query, Double userLat, Double userLon) {
        List<GeocodingResult> results = new ArrayList<>();
        
        try {
            // Generate cache key
            String cacheKey = cacheService.generateCacheKey(query, userLat, userLon);
            
            // Try to get from cache first
            Optional<GeocodingResult> cachedResult = cacheService.getCachedResult(cacheKey);
            if (cachedResult.isPresent()) {
                log.info("Cache hit for query: '{}' (avoiding Nominatim API call)", query);
                results.add(cachedResult.get());
                return results;
            }
            
            // Not in cache, query Nominatim
            results = queryNominatim(query, userLat, userLon);
            
            // Cache the first result if found
            if (!results.isEmpty()) {
                cacheService.cacheResult(cacheKey, query, results.get(0));
            }
            
        } catch (Exception e) {
            log.error("Error geocoding with Nominatim: {}", e.getMessage(), e);
        }
        
        return results;
    }
    
    /**
     * Query Nominatim with multiple query variations for robustness
     */
    private List<GeocodingResult> queryNominatim(String query, Double userLat, Double userLon) {
        List<GeocodingResult> results = new ArrayList<>();
        
        try {
            // Build list of queries to try (with variations)
            List<String> queries = new ArrayList<>();
            String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
            
            queries.add(query);
            queries.add(query + ", South Africa");
            queries.add(query + ", Johannesburg");  // Default to Johannesburg
            queries.add(query + ", Gauteng");       // Or Gauteng province
            
            // Nominatim requires a valid User-Agent and may rate limit or block requests without it
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, "OneBus/1.0 (" + email + ")");
            headers.set(HttpHeaders.ACCEPT_LANGUAGE, "en");
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            for (String searchQuery : queries) {
                UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/search")
                        .queryParam("q", searchQuery)
                        .queryParam("format", "json")
                        .queryParam("addressdetails", "1")
                        .queryParam("limit", "5")
                        .queryParam("countrycodes", "za")
                        .queryParam("email", email);
                
                // Add viewbox for bias if user location provided
                if (userLat != null && userLon != null) {
                    double radius = 0.5; // ~50km radius in degrees
                    builder.queryParam("viewbox", 
                        String.format("%f,%f,%f,%f", 
                            userLon - radius, userLat - radius, 
                            userLon + radius, userLat + radius))
                        .queryParam("bounded", "0");
                }
                
                String url = builder.toUriString();
                log.debug("Nominatim geocoding attempt: {}", url);
                
                try {
                    ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
                    
                    if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                        log.debug("Nominatim request failed for query: {} (status: {})", searchQuery, responseEntity.getStatusCode());
                        continue;
                    }
                    
                    String response = responseEntity.getBody();
                    if (response == null || response.isEmpty()) {
                        log.debug("Nominatim returned empty response for query: {}", searchQuery);
                        continue;
                    }
                    
                    JsonNode jsonArray = objectMapper.readTree(response);
                    
                    if (jsonArray.isArray() && jsonArray.size() > 0) {
                        for (JsonNode node : jsonArray) {
                            GeocodingResult result = GeocodingResult.builder()
                                    .latitude(node.get("lat").asDouble())
                                    .longitude(node.get("lon").asDouble())
                                    .displayName(node.get("display_name").asText())
                                    .type(node.has("type") ? node.get("type").asText() : "unknown")
                                    .confidence(node.has("importance") ? node.get("importance").asDouble() : 0.5)
                                    .build();
                            results.add(result);
                        }
                        log.info("Nominatim query '{}' returned {} results", searchQuery, results.size());
                        break;  // Success, don't try other query variations
                    }
                } catch (Exception e) {
                    log.debug("Error on Nominatim query variation '{}': {}", searchQuery, e.getMessage());
                    continue;  // Try next variation
                }
            }
            
            if (results.isEmpty()) {
                log.warn("All Nominatim queries failed for: '{}'", query);
            }
            
        } catch (Exception e) {
            log.error("Error querying Nominatim: {}", e.getMessage(), e);
        }
        
        return results;
    }
    
    @Override
    @Cacheable(value = "reverseGeocodingCache", key = "#latitude + '_' + #longitude")
    public GeocodingResult reverseGeocode(Double latitude, Double longitude) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/reverse")
                    .queryParam("lat", latitude)
                    .queryParam("lon", longitude)
                    .queryParam("format", "json")
                    .queryParam("addressdetails", "1")
                    .queryParam("email", email)
                    .toUriString();
            
            log.info("Nominatim reverse geocoding request: {}", url);
            
            String response = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(response);
            
            return GeocodingResult.builder()
                    .latitude(node.get("lat").asDouble())
                    .longitude(node.get("lon").asDouble())
                    .displayName(node.get("display_name").asText())
                    .type(node.has("type") ? node.get("type").asText() : "unknown")
                    .confidence(1.0)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error reverse geocoding with Nominatim: {}", e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public String getProviderName() {
        return "Nominatim (OpenStreetMap) with Database Caching";
    }
}
