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

/**
 * OpenStreetMap Nominatim implementation of GeocodingProvider
 * Free and open-source geocoding service
 */
@Slf4j
@Service("nominatimGeocodingProvider")
public class NominatimGeocodingProvider implements GeocodingProvider {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${geocoding.nominatim.base-url:https://nominatim.openstreetmap.org}")
    private String baseUrl;
    
    @Value("${geocoding.nominatim.email:admin@onebus.com}")
    private String email;
    
    public NominatimGeocodingProvider(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    // @Cacheable(value = "geocodingCache", key = "#query + '_' + #userLat + '_' + #userLon")
    public List<GeocodingResult> geocode(String query, Double userLat, Double userLon) {
        List<GeocodingResult> results = new ArrayList<>();
        
        try {
            // Add "South Africa" context if not already in query
            String enhancedQuery = query;
            if (!query.toLowerCase().contains("south africa") && !query.toLowerCase().contains(", za")) {
                enhancedQuery = query + ", South Africa";
            }
            
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/search")
                    .queryParam("q", enhancedQuery)
                    .queryParam("format", "json")
                    .queryParam("addressdetails", "1")
                    .queryParam("limit", "5")
                    .queryParam("countrycodes", "za")  // Limit to South Africa
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
            log.info("Nominatim geocoding request: {}", url);
            
            // Try with simpler approach - just use RestTemplate default behavior
            // Nominatim might be rate limiting or blocking based on default RestTemplate headers
            log.warn("Note: If getting empty results, Nominatim may be rate limiting. Consider using a commercial geocoding service.");
            
            String response = restTemplate.getForObject(url, String.class);
            log.debug("Nominatim raw response: {}", response);
            
            if (response == null || response.isEmpty()) {
                log.warn("Nominatim returned null or empty response for query: {}", enhancedQuery);
                return results;
            }
            
            JsonNode jsonArray = objectMapper.readTree(response);
            log.debug("Parsed JSON Array: {}", jsonArray);
            log.debug("Is Array: {}, Length: {}", jsonArray.isArray(), jsonArray.isArray() ? jsonArray.size() : "N/A");
            
            if (jsonArray.isArray()) {
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
            } else {
                log.warn("Response is not a JSON array for query: {}", enhancedQuery);
            }
            
            log.info("Nominatim returned {} results for query: {}", results.size(), enhancedQuery);
            
        } catch (Exception e) {
            log.error("Error geocoding with Nominatim: {}", e.getMessage(), e);
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
        return "Nominatim (OpenStreetMap)";
    }
}
