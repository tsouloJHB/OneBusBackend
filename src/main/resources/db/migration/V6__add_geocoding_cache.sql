-- V6: Add geocoding cache table for storing geocoding results
-- This replaces the need for hardcoded fallback results and improves performance

CREATE TABLE IF NOT EXISTS geocoding_cache (
    id BIGSERIAL PRIMARY KEY,
    query_key VARCHAR(500) NOT NULL UNIQUE,
    original_query VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    display_name VARCHAR(500),
    location_type VARCHAR(100),
    confidence DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP,
    access_count INTEGER DEFAULT 0
);

-- Create indexes for efficient lookups
CREATE INDEX idx_geocoding_cache_query_key ON geocoding_cache(query_key);
CREATE INDEX idx_geocoding_cache_created_at ON geocoding_cache(created_at);
CREATE INDEX idx_geocoding_cache_original_query ON geocoding_cache(original_query);

-- Comment on table and columns for documentation
COMMENT ON TABLE geocoding_cache IS 'Cache for geocoding results to reduce API calls to Nominatim';
COMMENT ON COLUMN geocoding_cache.query_key IS 'Normalized query key for cache lookup (format: "query:latitude:longitude")';
COMMENT ON COLUMN geocoding_cache.original_query IS 'Original search query before normalization';
COMMENT ON COLUMN geocoding_cache.access_count IS 'Number of times this cache entry has been used';
