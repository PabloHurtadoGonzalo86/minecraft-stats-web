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
 * Based on official Minecraft statistics: https://minecraft.wiki/w/Statistics
 * Updated for Minecraft 1.21+
 */
data class DetailedPlayerStats(
    // Combat
    val damageDealt: Long = 0,
    val damageTaken: Long = 0,
    val damageBlocked: Long = 0,
    val playerKills: Long = 0,
    val mobKills: Long = 0,  // minecraft:mob_kills (different from killed category)
    // Detailed damage stats (NEW - from official wiki)
    val damageDealtAbsorbed: Long = 0,   // minecraft:damage_dealt_absorbed
    val damageDealtResisted: Long = 0,   // minecraft:damage_dealt_resisted
    val damageAbsorbed: Long = 0,        // minecraft:damage_absorbed
    val damageResisted: Long = 0,        // minecraft:damage_resisted

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
    // NEW distances from official wiki
    val minecartDistance: Long = 0,      // minecraft:minecart_one_cm

    // Interactions - Containers (NEW from official wiki)
    val chestsOpened: Long = 0,
    val enderChestsOpened: Long = 0,     // minecraft:open_enderchest
    val barrelsOpened: Long = 0,         // minecraft:open_barrel
    val shulkerBoxesOpened: Long = 0,    // minecraft:open_shulker_box
    val trappedChestsTriggered: Long = 0, // minecraft:trigger_trapped_chest

    // Interactions - Workstations
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
    // NEW workstations from official wiki
    val campfireUses: Long = 0,          // minecraft:interact_with_campfire
    val cartographyTableUses: Long = 0,  // minecraft:interact_with_cartography_table
    val loomUses: Long = 0,              // minecraft:interact_with_loom
    val grindstoneUses: Long = 0,        // minecraft:interact_with_grindstone
    val lecternUses: Long = 0,           // minecraft:interact_with_lectern

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
    // NEW actions from official wiki
    val noteBlocksPlayed: Long = 0,      // minecraft:play_noteblock
    val noteBlocksTuned: Long = 0,       // minecraft:tune_noteblock
    val cakeSlicesEaten: Long = 0,       // minecraft:eat_cake_slice
    val cauldronsUsed: Long = 0,         // minecraft:use_cauldron
    val cauldronsFilled: Long = 0,       // minecraft:fill_cauldron
    val flowersPotted: Long = 0,         // minecraft:pot_flower
    val armorCleaned: Long = 0,          // minecraft:clean_armor
    val bannersCleaned: Long = 0,        // minecraft:clean_banner
    val shulkerBoxesCleaned: Long = 0,   // minecraft:clean_shulker_box
    val gamesLeft: Long = 0,             // minecraft:leave_game
    val itemsDropped: Long = 0,          // minecraft:drop

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
    val mostDistanceWalked: List<LeaderboardEntry>,
    // New leaderboards
    val mostItemsCrafted: List<LeaderboardEntry> = emptyList(),
    val mostDamageDealt: List<LeaderboardEntry> = emptyList(),
    val mostJumps: List<LeaderboardEntry> = emptyList(),
    val mostFishCaught: List<LeaderboardEntry> = emptyList(),
    val mostVillagerTrades: List<LeaderboardEntry> = emptyList(),
    // NEW leaderboards from official wiki stats
    val mostAnimalsBreed: List<LeaderboardEntry> = emptyList(),
    val mostDamageBlocked: List<LeaderboardEntry> = emptyList(),
    val mostEnderChestsOpened: List<LeaderboardEntry> = emptyList(),
    val mostMinecartDistance: List<LeaderboardEntry> = emptyList(),
    val mostCakeSlicesEaten: List<LeaderboardEntry> = emptyList(),
    val mostRaidWins: List<LeaderboardEntry> = emptyList(),
    val mostItemsEnchanted: List<LeaderboardEntry> = emptyList(),
    val mostItemsDropped: List<LeaderboardEntry> = emptyList()
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
    val totalChestsOpened: Long = 0,
    // New totals
    val totalJumps: Long = 0,
    val totalFishCaught: Long = 0,
    val totalAnimalsBred: Long = 0,
    val totalVillagerTrades: Long = 0,
    val totalTimesSlept: Long = 0,
    // NEW totals from official wiki (minecraft:custom)
    val totalMobKills: Long = 0,
    val totalEnderChestsOpened: Long = 0,
    val totalBarrelsOpened: Long = 0,
    val totalShulkerBoxesOpened: Long = 0,
    val totalMinecartDistance: Long = 0,
    val totalNoteBlocksPlayed: Long = 0,
    val totalCakeSlicesEaten: Long = 0,
    val totalItemsDropped: Long = 0,
    val totalDamageBlocked: Long = 0,
    val totalRaidWins: Long = 0,
    val totalItemsEnchanted: Long = 0
)

// ============== Log & Events Models ==============

/**
 * Types of log events captured from Minecraft server logs
 * Based on Minecraft server log format
 */
enum class LogEntryType {
    CHAT,           // Player chat messages
    JOIN,           // Player joined the game
    LEAVE,          // Player left the game
    DEATH,          // Player death (all causes)
    ADVANCEMENT,    // Player got an advancement/goal/challenge
    COMMAND,        // Player issued a server command
    KICK,           // Player was kicked
    BAN,            // Player was banned
    WARNING,        // Server warnings
    SERVER_START,   // Server started
    SERVER_STOP,    // Server stopped/stopping
    WORLD_SAVE,     // World saved
    PVP_KILL,       // Player killed another player
    OTHER           // Other log entries
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

// ============== Item Statistics Models ==============

data class ItemLeaderboard(
    val category: String, // mined, used, picked_up, killed, killed_by
    val categoryDisplay: String,
    val entries: List<ItemEntry>
)

data class ItemEntry(
    val itemId: String, // minecraft:diamond
    val itemName: String, // Diamond
    val count: Long,
    val iconUrl: String? = null
)

data class PlayerItemStats(
    val uuid: String,
    val name: String,
    val topMined: List<ItemEntry>,
    val topUsed: List<ItemEntry>,
    val topPickedUp: List<ItemEntry>,
    val topKilled: List<ItemEntry>,
    val topKilledBy: List<ItemEntry>
)

// ============== Session Analysis Models ==============

data class PlayerSession(
    val playerName: String,
    val playerUuid: String?,
    val joinTime: String,
    val joinTimestamp: Long,
    val leaveTime: String?,
    val leaveTimestamp: Long?,
    val durationMinutes: Long?,
    val durationFormatted: String?
)

data class SessionStats(
    val totalSessions: Int,
    val averageSessionMinutes: Long,
    val averageSessionFormatted: String,
    val longestSession: PlayerSession?,
    val recentSessions: List<PlayerSession>,
    val sessionsByPlayer: Map<String, List<PlayerSession>>
)

// ============== Activity Analysis Models ==============

data class ActivityStats(
    val hourlyActivity: Map<Int, Int>, // Hour (0-23) -> event count
    val dailyActivity: Map<String, Int>, // Date -> event count
    val weekdayActivity: Map<String, Int>, // Monday-Sunday -> event count
    val mostActiveHour: Int,
    val mostActiveDay: String,
    val peakPlayers: Int,
    val peakPlayersDate: String
)

// ============== Advancement Timeline Models ==============

data class AdvancementTimeline(
    val uuid: String,
    val playerName: String,
    val timeline: List<AdvancementEvent>,
    val totalCompleted: Int,
    val firstAdvancement: AdvancementEvent?,
    val lastAdvancement: AdvancementEvent?
)

data class AdvancementEvent(
    val advancementId: String,
    val advancementName: String,
    val category: String, // story, adventure, nether, end, husbandry
    val completedAt: String,
    val timestamp: Long
)

// ============== Biome Exploration Models ==============

data class BiomeExploration(
    val uuid: String,
    val playerName: String,
    val visitedBiomes: List<BiomeVisit>,
    val totalBiomesVisited: Int,
    val totalBiomesInGame: Int,
    val explorationPercentage: Int
)

data class BiomeVisit(
    val biomeId: String, // minecraft:flower_forest
    val biomeName: String, // Flower Forest
    val visitedAt: String,
    val timestamp: Long
)

// ============== Server Records Models ==============

data class ServerRecords(
    val mostDiamondsMined: RecordEntry?,
    val longestBoatDistance: RecordEntry?,
    val mostMobsKilled: RecordEntry?,
    val mostDeaths: RecordEntry?,
    val longestPlayTime: RecordEntry?,
    val mostItemsCrafted: RecordEntry?,
    val mostBlocksMined: RecordEntry?,
    val mostFishCaught: RecordEntry?,
    val mostVillagerTrades: RecordEntry?,
    val mostJumps: RecordEntry?
)

data class RecordEntry(
    val playerName: String,
    val playerUuid: String,
    val value: Long,
    val formattedValue: String,
    val recordName: String
)

// ============== Diamond Statistics Models ==============

data class DiamondStats(
    val totalDiamondOreMined: Long,
    val totalDeepslateDiamondOreMined: Long,
    val totalDiamondsPickedUp: Long,
    val totalDiamondsDropped: Long,
    val toolsCrafted: Long,
    val armorCrafted: Long,
    val toolsBroken: Long,
    val leaderboard: List<DiamondLeaderboardEntry>
)

data class DiamondLeaderboardEntry(
    val playerName: String,
    val playerUuid: String,
    val total: Long
)

// ============== Watchdog / Surveillance System ==============

/**
 * Types of suspicious activity that can be detected
 */
enum class SuspiciousActivityType {
    PVP_KILL,              // Killed another player
    MASS_DEATHS,           // Many deaths in short time
    LAVA_DEATHS,           // Multiple lava deaths (possible trap)
    FALL_DEATHS,           // Multiple fall deaths (possible trap)
    HIGH_DAMAGE_DEALT,     // Dealt a lot of damage quickly
    LONG_SESSION,          // Very long play session
    KICK,                  // Player was kicked
    BAN,                   // Player was banned
    SUSPICIOUS_COMMAND     // Suspicious server command
}

enum class AlertSeverity {
    LOW,      // Informational
    MEDIUM,   // Worth monitoring
    HIGH,     // Action may be needed
    CRITICAL  // Immediate attention
}

/**
 * Represents a suspicious activity alert
 */
data class SuspiciousActivity(
    val type: SuspiciousActivityType,
    val playerName: String,
    val playerUuid: String?,
    val description: String,
    val timestamp: String,
    val severity: AlertSeverity,
    val details: Map<String, Any> = emptyMap()
)

/**
 * Player watch profile with risk metrics
 */
data class PlayerWatchProfile(
    val uuid: String,
    val name: String,
    val pvpKills: Long,
    val pvpDeaths: Long,
    val killDeathRatio: Double,
    val totalDeaths: Long,
    val lavaDeaths: Long,
    val fallDeaths: Long,
    val timesKicked: Long,
    val totalDamageDealt: Long,
    val totalItemsDropped: Long,
    val averageSessionMinutes: Long,
    val longestSessionMinutes: Long,
    val lastActivity: String,
    val riskScore: Int,  // 0-100
    val recentAlerts: List<SuspiciousActivity>
)

/**
 * Server-wide watchdog statistics
 */
data class ServerWatchStats(
    val totalPvpKills: Long,
    val pvpLeaderboard: List<PvpLeaderboardEntry>,
    val recentAlerts: List<SuspiciousActivity>,
    val highRiskPlayers: List<PlayerWatchProfile>,
    val deathsByCategory: Map<String, Long>,
    val totalKicks: Long,
    val totalBans: Long
)

/**
 * PVP leaderboard entry
 */
data class PvpLeaderboardEntry(
    val playerName: String,
    val playerUuid: String?,
    val kills: Long,
    val deaths: Long,
    val kd: Double
)

/**
 * Death analysis for surveillance
 */
data class DeathAnalysis(
    val totalDeaths: Long,
    val deathsByPlayer: Map<String, Long>,
    val deathsByCause: Map<String, Long>,
    val lavaDeaths: Long,
    val fallDeaths: Long,
    val pvpDeaths: Long,
    val mobDeaths: Long,
    val environmentDeaths: Long
)
