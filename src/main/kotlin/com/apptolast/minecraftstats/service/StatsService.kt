package com.apptolast.minecraftstats.service

import com.apptolast.minecraftstats.config.MinecraftProperties
import com.apptolast.minecraftstats.model.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class StatsService(
    private val objectMapper: ObjectMapper,
    private val properties: MinecraftProperties
) {
    private val logger = LoggerFactory.getLogger(StatsService::class.java)

    @Cacheable("serverStats")
    fun getServerStats(): ServerStats {
        logger.info("Loading server statistics from: ${properties.statsPath}")
        
        val playerCache = loadPlayerCache()
        val playerStats = loadAllPlayerStats(playerCache)
        val leaderboards = buildLeaderboards(playerStats)
        val serverTotals = calculateServerTotals(playerStats)
        
        return ServerStats(
            totalPlayers = playerStats.size,
            players = playerStats,
            leaderboards = leaderboards,
            serverTotals = serverTotals,
            lastUpdated = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
    }

    @Cacheable("players")
    fun getPlayerStats(uuid: String): PlayerStats? {
        val playerCache = loadPlayerCache()
        return loadPlayerStats(uuid, playerCache)
    }

    private fun loadPlayerCache(): Map<String, String> {
        val cacheFile = File(properties.userCachePath)
        return if (cacheFile.exists()) {
            try {
                val entries: List<PlayerCacheEntry> = objectMapper.readValue(cacheFile)
                entries.associate { it.uuid to it.name }
            } catch (e: Exception) {
                logger.warn("Could not load user cache: ${e.message}")
                emptyMap()
            }
        } else {
            logger.warn("User cache file not found: ${properties.userCachePath}")
            emptyMap()
        }
    }

    private fun loadAllPlayerStats(playerCache: Map<String, String>): List<PlayerStats> {
        val statsDir = File(properties.statsPath)
        if (!statsDir.exists() || !statsDir.isDirectory) {
            logger.error("Stats directory not found: ${properties.statsPath}")
            return emptyList()
        }

        return statsDir.listFiles { file -> file.extension == "json" }
            ?.mapNotNull { file ->
                val uuid = file.nameWithoutExtension
                loadPlayerStats(uuid, playerCache)
            }
            ?: emptyList()
    }

    private fun loadPlayerStats(uuid: String, playerCache: Map<String, String>): PlayerStats? {
        val statsFile = File(properties.statsPath, "$uuid.json")
        if (!statsFile.exists()) {
            logger.warn("Stats file not found for UUID: $uuid")
            return null
        }

        return try {
            val statsData: MinecraftStatsFile = objectMapper.readValue(statsFile)
            val playerName = playerCache[uuid] ?: uuid
            val summary = calculateSummary(statsData.stats)
            
            PlayerStats(
                uuid = uuid,
                name = playerName,
                stats = statsData.stats,
                summary = summary
            )
        } catch (e: Exception) {
            logger.error("Error loading stats for UUID $uuid: ${e.message}")
            null
        }
    }

    private fun calculateSummary(stats: StatsCategories): PlayerStatsSummary {
        val totalBlocksMined = stats.mined.values.sum()
        val totalItemsCrafted = stats.crafted.values.sum()
        val totalMobsKilled = stats.killed.values.sum()
        
        // Custom stats keys from Minecraft wiki
        val totalDeaths = stats.custom["minecraft:deaths"] ?: 0L
        val playTimeTicks = stats.custom["minecraft:play_time"] 
            ?: stats.custom["minecraft:play_one_minute"]?.times(1200) // Fallback for older versions
            ?: 0L
        val jumps = stats.custom["minecraft:jump"] ?: 0L
        
        // Distance walked in centimeters
        val distanceWalkedCm = stats.custom["minecraft:walk_one_cm"] ?: 0L
        
        return PlayerStatsSummary(
            totalBlocksMined = totalBlocksMined,
            totalItemsCrafted = totalItemsCrafted,
            totalMobsKilled = totalMobsKilled,
            totalDeaths = totalDeaths,
            playTimeTicks = playTimeTicks,
            playTimeFormatted = formatPlayTime(playTimeTicks),
            distanceWalkedCm = distanceWalkedCm,
            distanceWalkedFormatted = formatDistance(distanceWalkedCm),
            jumps = jumps
        )
    }

    private fun formatPlayTime(ticks: Long): String {
        val totalSeconds = ticks / 20
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return "${hours}h ${minutes}m"
    }

    private fun formatDistance(cm: Long): String {
        val meters = cm / 100.0
        return when {
            meters >= 1000 -> String.format("%.2f km", meters / 1000)
            else -> String.format("%.0f m", meters)
        }
    }

    private fun buildLeaderboards(players: List<PlayerStats>): Leaderboards {
        return Leaderboards(
            mostBlocksMined = buildLeaderboard(players, "bloques minados") { it.summary.totalBlocksMined },
            mostMobsKilled = buildLeaderboard(players, "mobs eliminados") { it.summary.totalMobsKilled },
            mostPlayTime = buildLeaderboard(players, "") { it.summary.playTimeTicks }
                .map { it.copy(formattedValue = formatPlayTime(it.value)) },
            mostDeaths = buildLeaderboard(players, "muertes") { it.summary.totalDeaths },
            mostDistanceWalked = buildLeaderboard(players, "") { it.summary.distanceWalkedCm }
                .map { it.copy(formattedValue = formatDistance(it.value)) }
        )
    }

    private fun buildLeaderboard(
        players: List<PlayerStats>,
        suffix: String,
        valueExtractor: (PlayerStats) -> Long
    ): List<LeaderboardEntry> {
        return players
            .sortedByDescending(valueExtractor)
            .take(10)
            .mapIndexed { index, player ->
                val value = valueExtractor(player)
                LeaderboardEntry(
                    rank = index + 1,
                    playerName = player.name,
                    playerUuid = player.uuid,
                    value = value,
                    formattedValue = if (suffix.isNotEmpty()) "$value $suffix" else value.toString()
                )
            }
    }

    private fun calculateServerTotals(players: List<PlayerStats>): ServerTotals {
        val totalBlocksMined = players.sumOf { it.summary.totalBlocksMined }
        val totalItemsCrafted = players.sumOf { it.summary.totalItemsCrafted }
        val totalMobsKilled = players.sumOf { it.summary.totalMobsKilled }
        val totalDeaths = players.sumOf { it.summary.totalDeaths }
        val totalPlayTimeTicks = players.sumOf { it.summary.playTimeTicks }

        return ServerTotals(
            totalBlocksMined = totalBlocksMined,
            totalItemsCrafted = totalItemsCrafted,
            totalMobsKilled = totalMobsKilled,
            totalDeaths = totalDeaths,
            totalPlayTimeTicks = totalPlayTimeTicks,
            totalPlayTimeFormatted = formatPlayTime(totalPlayTimeTicks)
        )
    }
}
