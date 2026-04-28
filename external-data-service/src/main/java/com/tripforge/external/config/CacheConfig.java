package com.tripforge.external.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis cache configuration with per-cache TTL settings.
 *
 * Cache names and TTLs:
 *   places-search   → 30 min  (search results change infrequently)
 *   place-details   → 1 hour  (place details are stable)
 *   hotel-search    → 30 min  (prices can change)
 *   route-optimize  → 30 min  (routes are stable for a given set of places)
 *   fx-rates        → 6 hours (FX rates change slowly)
 *   provider-health → 1 min   (health checks should be fresh)
 */
@Configuration
public class CacheConfig {

    @Value("${cache.ttl.places-search:1800000}")
    private long placesSearchTtlMs;

    @Value("${cache.ttl.place-details:3600000}")
    private long placeDetailsTtlMs;

    @Value("${cache.ttl.hotel-search:1800000}")
    private long hotelSearchTtlMs;

    @Value("${cache.ttl.route-optimize:1800000}")
    private long routeOptimizeTtlMs;

    @Value("${cache.ttl.fx-rates:21600000}")
    private long fxRatesTtlMs;

    @Value("${cache.ttl.provider-health:60000}")
    private long providerHealthTtlMs;

    @Value("${cache.ttl.location-search:900000}")
    private long locationSearchTtlMs;

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues()
                .entryTtl(Duration.ofHours(1));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("places-search",  defaultConfig.entryTtl(Duration.ofMillis(placesSearchTtlMs)));
        cacheConfigs.put("place-details",  defaultConfig.entryTtl(Duration.ofMillis(placeDetailsTtlMs)));
        cacheConfigs.put("hotel-search",   defaultConfig.entryTtl(Duration.ofMillis(hotelSearchTtlMs)));
        cacheConfigs.put("route-optimize", defaultConfig.entryTtl(Duration.ofMillis(routeOptimizeTtlMs)));
        cacheConfigs.put("fx-rates",       defaultConfig.entryTtl(Duration.ofMillis(fxRatesTtlMs)));
        cacheConfigs.put("provider-health",defaultConfig.entryTtl(Duration.ofMillis(providerHealthTtlMs)));
        cacheConfigs.put("location-search",defaultConfig.entryTtl(Duration.ofMillis(locationSearchTtlMs)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
