package com.apptolast.minecraftstats.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun cacheManager(): CacheManager {
        val cacheManager = CaffeineCacheManager(
            // Original caches
            "stats", "players", "serverStats", "advancements",
            // Item stats caches
            "topMined", "topUsed", "topPickedUp", "topKilled", "topKilledBy", "topCrafted",
            // Records cache
            "records",
            // Session & Activity caches
            "sessions", "activity"
        )
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(100)
        )
        return cacheManager
    }
}
