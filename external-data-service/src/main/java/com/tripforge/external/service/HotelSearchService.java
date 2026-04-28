package com.tripforge.external.service;

import com.tripforge.external.dto.HotelCandidateDto;
import com.tripforge.external.dto.ProviderResponse;
import com.tripforge.external.provider.GeoapifyHotelProvider;
import com.tripforge.external.provider.GeoapifyLocationProvider;
import com.tripforge.external.provider.GooglePlacesProvider;
import com.tripforge.external.provider.NominatimProvider;
import com.tripforge.external.provider.OpenTripMapProvider;
import com.tripforge.external.provider.OverpassHotelProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Hotel search service — orchestrates provider fallback chain for hotel candidates.
 *
 * FINAL provider order (Phase 10E — Real Data Upgrade):
 *   1. Geoapify Places API (PRIMARY — free tier, real place names, global)
 *   2. Overpass API (OpenStreetMap) — free, no key, global coverage
 *   3. OpenTripMap (kinds=accomodations) — free tier, key required
 *   4. Google Places (type=lodging) — OPTIONAL, only if key configured
 *   5. Degraded — hotel-service uses CSV dataset or synthetic fallback
 *
 * Geoapify is preferred over Overpass because:
 *   - Better relevance ranking
 *   - Cleaner English names
 *   - More structured address data
 *   - Supports category filtering
 *
 * Nominatim/Geoapify is used for geocoding when lat/lon not provided.
 * Results are cached in Redis.
 *
 * Amadeus is NOT used — it requires OAuth2 and a billing account.
 * Aviationstack is NOT used — it is for flights, not hotels.
 */
@Service
public class HotelSearchService {

    private static final Logger log = LoggerFactory.getLogger(HotelSearchService.class);

    @Autowired private GeoapifyHotelProvider geoapify;
    @Autowired private GeoapifyLocationProvider geoapifyLocation;
    @Autowired private OverpassHotelProvider overpass;
    @Autowired private OpenTripMapProvider openTripMap;
    @Autowired private GooglePlacesProvider googlePlaces;
    @Autowired private NominatimProvider nominatim;

    /**
     * Search for hotel candidates for a destination.
     *
     * Provider chain:
     *   1. Geoapify Places (primary — free tier, real names, global)
     *   2. Overpass OSM (free, global, no key)
     *   3. OpenTripMap (free tier, key required)
     *   4. Google Places (optional, key required)
     *   5. Degraded (hotel-service falls back to CSV/synthetic)
     */
    @Cacheable(value = "hotel-search",
               key = "#city + '_' + (#lat != null ? #lat.toString() : 'null') + '_' + #currency + '_' + #preference",
               unless = "#result.data.isEmpty()")
    public ProviderResponse<List<HotelCandidateDto>> searchHotels(
            String city, Double lat, Double lng,
            Double budgetPerNight, String currency, String preference) {

        log.info("Searching hotels: city='{}' lat={} lng={} currency={} pref={}",
                city, lat, lng, currency, preference);

        // Resolve coordinates if not provided
        double resolvedLat = lat != null ? lat : 0;
        double resolvedLng = lng != null ? lng : 0;
        if (lat == null || lng == null) {
            // Try Geoapify geocoding first (better quality)
            double[] coords = geoapifyLocation.geocodeCity(city);
            if (coords != null) {
                resolvedLat = coords[0];
                resolvedLng = coords[1];
                log.debug("Geoapify geocoded '{}' → [{}, {}]", city, resolvedLat, resolvedLng);
            } else {
                // Fall back to Nominatim
                coords = nominatim.geocodeCity(city);
                if (coords != null) {
                    resolvedLat = coords[0];
                    resolvedLng = coords[1];
                    log.debug("Nominatim geocoded '{}' → [{}, {}]", city, resolvedLat, resolvedLng);
                }
            }
        }

        // ── 1. Geoapify Places (PRIMARY — free tier, real names) ──────────────
        if (resolvedLat != 0 && resolvedLng != 0) {
            List<HotelCandidateDto> geoapifyHotels = geoapify.searchHotels(
                    resolvedLat, resolvedLng, city, currency, budgetPerNight, preference);
            if (!geoapifyHotels.isEmpty()) {
                log.info("Geoapify returned {} hotel candidates for '{}'",
                        geoapifyHotels.size(), city);
                return ProviderResponse.of(geoapifyHotels, "geoapify");
            }
            log.debug("Geoapify returned empty for '{}' — trying Overpass", city);
        }

        // ── 2. Overpass API (OpenStreetMap) — free, global, no key ────────────
        if (resolvedLat != 0 && resolvedLng != 0) {
            List<HotelCandidateDto> overpassHotels = overpass.searchHotels(
                    resolvedLat, resolvedLng, city, currency, budgetPerNight, preference);
            if (!overpassHotels.isEmpty()) {
                log.info("Overpass OSM returned {} hotel candidates for '{}'",
                        overpassHotels.size(), city);
                return ProviderResponse.of(overpassHotels, "overpass_osm");
            }
            log.debug("Overpass returned empty for '{}' — trying OpenTripMap", city);
        }

        // ── 3. OpenTripMap (free tier, key required) ──────────────────────────
        List<HotelCandidateDto> otmHotels = openTripMap.searchHotels(
                city,
                resolvedLat != 0 ? resolvedLat : null,
                resolvedLng != 0 ? resolvedLng : null,
                currency);
        if (!otmHotels.isEmpty()) {
            log.info("OpenTripMap returned {} hotel candidates for '{}'", otmHotels.size(), city);
            return ProviderResponse.fallback(otmHotels, "opentripmap",
                    "Geoapify + Overpass returned empty — using OpenTripMap");
        }

        // ── 4. Google Places (optional, key required) ─────────────────────────
        List<HotelCandidateDto> googleHotels = googlePlaces.searchHotels(city,
                resolvedLat != 0 ? resolvedLat : null,
                resolvedLng != 0 ? resolvedLng : null,
                budgetPerNight, currency);
        if (!googleHotels.isEmpty()) {
            log.info("Google Places returned {} hotel candidates for '{}'",
                    googleHotels.size(), city);
            return ProviderResponse.fallback(googleHotels, "google_places",
                    "Geoapify + Overpass + OTM returned empty — using optional Google Places");
        }

        // ── 5. Degraded — hotel-service will use CSV/synthetic fallback ────────
        log.warn("All hotel providers returned empty for '{}' — hotel-service will use fallback",
                city);
        return ProviderResponse.degraded(List.of(), "none",
                "No live hotel provider available — hotel-service will use local CSV or synthetic fallback");
    }

    /**
     * Backward-compatible overload without preference parameter.
     */
    public ProviderResponse<List<HotelCandidateDto>> searchHotels(
            String city, Double lat, Double lng,
            Double budgetPerNight, String currency) {
        return searchHotels(city, lat, lng, budgetPerNight, currency, "STANDARD");
    }
}
