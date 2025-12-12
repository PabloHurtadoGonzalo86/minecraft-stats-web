package com.apptolast.minecraftstats.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Represents the root structure of a Minecraft player statistics JSON file.
 * Based on official Minecraft statistics format: https://minecraft.wiki/w/Statistics
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class MinecraftStatsFile(
    val stats: StatsCategories = StatsCategories(),
    @JsonProperty("DataVersion")
    val dataVersion: Int = 0
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class StatsCategories(
    @JsonProperty("minecraft:mined")
    val mined: Map<String, Long> = emptyMap(),
    
    @JsonProperty("minecraft:broken")
    val broken: Map<String, Long> = emptyMap(),
    
    @JsonProperty("minecraft:crafted")
    val crafted: Map<String, Long> = emptyMap(),
    
    @JsonProperty("minecraft:used")
    val used: Map<String, Long> = emptyMap(),
    
    @JsonProperty("minecraft:picked_up")
    val pickedUp: Map<String, Long> = emptyMap(),
    
    @JsonProperty("minecraft:dropped")
    val dropped: Map<String, Long> = emptyMap(),
    
    @JsonProperty("minecraft:killed")
    val killed: Map<String, Long> = emptyMap(),
    
    @JsonProperty("minecraft:killed_by")
    val killedBy: Map<String, Long> = emptyMap(),
    
    @JsonProperty("minecraft:custom")
    val custom: Map<String, Long> = emptyMap()
)

/**
 * Player information from usercache.json
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class PlayerCacheEntry(
    val uuid: String,
    val name: String,
    val expiresOn: String? = null
)

/**
 * Aggregated player statistics for display
 */
data class PlayerStats(
    val uuid: String,
    val name: String,
    val stats: StatsCategories,
    val summary: PlayerStatsSummary
)

data class PlayerStatsSummary(
    val totalBlocksMined: Long,
    val totalItemsCrafted: Long,
    val totalMobsKilled: Long,
    val totalDeaths: Long,
    val playTimeTicks: Long,
    val playTimeFormatted: String,
    val distanceWalkedCm: Long,
    val distanceWalkedFormatted: String,
    val jumps: Long
)

/**
 * Server-wide statistics summary
 */
data class ServerStats(
    val totalPlayers: Int,
    val players: List<PlayerStats>,
    val leaderboards: Leaderboards,
    val serverTotals: ServerTotals,
    val lastUpdated: String
)

data class Leaderboards(
    val mostBlocksMined: List<LeaderboardEntry>,
    val mostMobsKilled: List<LeaderboardEntry>,
    val mostPlayTime: List<LeaderboardEntry>,
    val mostDeaths: List<LeaderboardEntry>,
    val mostDistanceWalked: List<LeaderboardEntry>
)

data class LeaderboardEntry(
    val rank: Int,
    val playerName: String,
    val playerUuid: String,
    val value: Long,
    val formattedValue: String
)

data class ServerTotals(
    val totalBlocksMined: Long,
    val totalItemsCrafted: Long,
    val totalMobsKilled: Long,
    val totalDeaths: Long,
    val totalPlayTimeTicks: Long,
    val totalPlayTimeFormatted: String
)
