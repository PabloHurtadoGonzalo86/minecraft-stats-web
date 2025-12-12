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
    val summary: PlayerStatsSummary,
    val detailedStats: DetailedPlayerStats? = null
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
 * Detailed statistics extracted from minecraft:custom
 */
data class DetailedPlayerStats(
    // Combat
    val damageDealt: Long = 0,
    val damageTaken: Long = 0,
    val damageBlocked: Long = 0,
    val playerKills: Long = 0,
    
    // Movement (in cm, convert to km for display)
    val walkDistance: Long = 0,
    val sprintDistance: Long = 0,
    val swimDistance: Long = 0,
    val climbDistance: Long = 0,
    val flyDistance: Long = 0,
    val boatDistance: Long = 0,
    val horseDistance: Long = 0,
    val pigDistance: Long = 0,
    val striderDistance: Long = 0,
    val elytraDistance: Long = 0,
    val fallDistance: Long = 0,
    val crouchDistance: Long = 0,
    val walkOnWaterDistance: Long = 0,
    val walkUnderWaterDistance: Long = 0,
    
    // Interactions
    val chestsOpened: Long = 0,
    val craftingTableUses: Long = 0,
    val furnaceUses: Long = 0,
    val anvilUses: Long = 0,
    val enchantingTableUses: Long = 0,
    val smithingTableUses: Long = 0,
    val brewingStandUses: Long = 0,
    val beaconUses: Long = 0,
    val stonecutterUses: Long = 0,
    val smokerUses: Long = 0,
    val blastFurnaceUses: Long = 0,
    
    // Actions
    val timesSlept: Long = 0,
    val sneakTime: Long = 0,
    val fishCaught: Long = 0,
    val animalsBreed: Long = 0,
    val itemsEnchanted: Long = 0,
    val recordsPlayed: Long = 0,
    val bellsRung: Long = 0,
    val raidWins: Long = 0,
    val raidTriggers: Long = 0,
    val targetsHit: Long = 0,
    
    // Villagers
    val villagersTraded: Long = 0,
    val villagersTalked: Long = 0,
    
    // Time-based (in ticks, 20 ticks = 1 second)
    val timeSinceRest: Long = 0,
    val timeSinceDeath: Long = 0,
    val totalWorldTime: Long = 0
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
    val totalPlayTimeFormatted: String,
    // Additional server totals
    val totalDamageDealt: Long = 0,
    val totalDistanceTraveled: Long = 0,
    val totalChestsOpened: Long = 0
)

// ============== Log & Events Models ==============

enum class LogEntryType {
    CHAT, JOIN, LEAVE, DEATH, ADVANCEMENT, OTHER
}

data class LogEntry(
    val timestamp: String,
    val fullDateTime: String, // Full date with day/month/year hour:minute:second
    val date: String, // Just the date part (2025-12-12)
    val type: LogEntryType,
    val playerName: String?,
    val message: String,
    val rawLine: String
)

// ============== Server Status Models ==============

data class ServerStatus(
    val online: Boolean,
    val playerCount: Int,
    val maxPlayers: Int,
    val onlinePlayers: List<OnlinePlayer>,
    val motd: String,
    val version: String,
    val lastUpdated: Long,
    val lastUpdatedFormatted: String = "" // Human readable
)

data class OnlinePlayer(
    val name: String,
    val uuid: String?,
    val joinedAt: String?
)

// ============== Advancements Models ==============

data class PlayerAdvancements(
    val uuid: String,
    val advancements: List<Advancement>,
    val completedCount: Int,
    val totalCount: Int,
    val completionPercentage: Int
)

data class Advancement(
    val id: String,
    val name: String,
    val done: Boolean,
    val criteriaCount: Int,
    val completedAt: String?
)

// ============== WebSocket Messages ==============

data class LiveUpdate(
    val type: String,
    val data: Any,
    val timestamp: Long = System.currentTimeMillis(),
    val timestampFormatted: String = "", // ISO format with timezone
    val serverTime: ServerTime = ServerTime()
)

data class ServerTime(
    val timestamp: Long = System.currentTimeMillis(),
    val iso: String = "", // 2025-12-12T15:07:28.213Z
    val date: String = "", // 12/12/2025
    val time: String = "", // 15:07:28
    val dayOfWeek: String = "", // Jueves
    val timezone: String = "Europe/Madrid"
)

// ============== Real-time Stats ==============

data class RealTimePlayerStats(
    val uuid: String,
    val name: String,
    val playTimeTicks: Long,
    val playTimeSeconds: Long,
    val playTimeFormatted: String,
    val isOnline: Boolean = false,
    val lastSeen: String? = null
)
