package com.backend.onebus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeocodingResult {
    private Double latitude;
    private Double longitude;
    private String displayName;
    private String type; // e.g., "city", "suburb", "street", etc.
    private Double confidence; // 0.0 to 1.0
}
