package com.apptolast.minecraftstats.service

import com.apptolast.minecraftstats.model.*
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

/**
 * Service for monitoring player activity and detecting suspicious behavior
 * Analyzes logs, statistics, and session data to generate alerts
 */
@Service
class WatchdogService(
    private val logService: LogService,
    private val statsService: StatsService,
    private val sessionAnalysisService: SessionAnalysisService
) {
    private val logger = LoggerFactory.getLogger(WatchdogService::class.java)

    /**
     * Get comprehensive watchdog statistics
     */
    @Cacheable("watchdogStats")
    fun getWatchdogStats(days: Int = 30): ServerWatchStats {
        val events = logService.getHistoricalEvents(days, 5000)
        val serverStats = statsService.getServerStats()

        // Count PVP kills from logs
        val pvpEvents = events.filter { it.type == LogEntryType.PVP_KILL }
        val pvpKillsByPlayer = pvpEvents.groupBy { it.playerName ?: "Unknown" }
            .mapValues { it.value.size.toLong() }

        // Build PVP leaderboard
        val pvpLeaderboard = buildPvpLeaderboard(pvpKillsByPlayer, serverStats)

        // Get alerts
        val recentAlerts = generateAlerts(events, serverStats, days)

        // Get high risk players
        val highRiskPlayers = identifyHighRiskPlayers(serverStats, events, days)

        // Count deaths by category
        val deathsByCategory = categorizeDeaths(events)

        // Count kicks and bans
        val totalKicks = events.count { it.type == LogEntryType.KICK }.toLong()
        val totalBans = events.count { it.type == LogEntryType.BAN }.toLong()

        return ServerWatchStats(
            totalPvpKills = pvpKillsByPlayer.values.sum(),
            pvpLeaderboard = pvpLeaderboard,
            recentAlerts = recentAlerts.take(50),
            highRiskPlayers = highRiskPlayers,
            deathsByCategory = deathsByCategory,
            totalKicks = totalKicks,
            totalBans = totalBans
        )
    }

    /**
     * Get PVP statistics
     */
    fun getPvpStats(days: Int = 30): List<PvpLeaderboardEntry> {
        val events = logService.getHistoricalEvents(days, 5000)
        val serverStats = statsService.getServerStats()
        val pvpEvents = events.filter { it.type == LogEntryType.PVP_KILL }
        val pvpKillsByPlayer = pvpEvents.groupBy { it.playerName ?: "Unknown" }
            .mapValues { it.value.size.toLong() }
        return buildPvpLeaderboard(pvpKillsByPlayer, serverStats)
    }

    /**
     * Get death analysis
     */
    fun getDeathAnalysis(days: Int = 30): DeathAnalysis {
        val events = logService.getHistoricalEvents(days, 5000)
        val deathEvents = events.filter { it.type == LogEntryType.DEATH }

        val deathsByPlayer = deathEvents.groupBy { it.playerName ?: "Unknown" }
            .mapValues { it.value.size.toLong() }

        val deathsByCause = categorizeDeaths(events)

        // Known mobs (order matters: compound names first)
        val knownMobs = listOf(
            "zombified piglin", "zombie villager", "wither skeleton", "magma cube",
            "piglin brute", "elder guardian", "cave spider", "ender dragon", "iron golem",
            "polar bear", "zombie", "skeleton", "creeper", "spider", "enderman", "witch",
            "slime", "phantom", "drowned", "husk", "stray", "blaze", "ghast",
            "piglin", "hoglin", "zoglin", "pillager", "vindicator", "evoker", "ravager",
            "vex", "guardian", "shulker", "endermite", "silverfish", "warden", "breeze",
            "bogged", "bee", "wolf", "llama", "panda", "dolphin", "goat", "fox", "wither"
        )
        fun containsMob(msg: String): Boolean = knownMobs.any { mob -> msg.contains(mob) }

        // Calculate deaths - MUTUALLY EXCLUSIVE categories
        val lavaDeaths = deathEvents.count { it.message.contains("lava", ignoreCase = true) }.toLong()

        val fallDeaths = deathEvents.count { death ->
            val msg = death.message.lowercase()
            !msg.contains("lava") &&
            (msg.contains("fell") || msg.contains("hit the ground") ||
             msg.contains("cayo") || msg.contains("caída") || msg.contains("condenado a caer"))
        }.toLong()

        // Mob deaths: attack phrase + known mob name
        val mobDeaths = deathEvents.count { death ->
            val msg = death.message.lowercase()
            if (msg.contains("lava")) return@count false
            val hasAttackPhrase = msg.contains("slain by") || msg.contains("killed by") ||
                                  msg.contains("shot by") || msg.contains("asesinado por") ||
                                  msg.contains("disparado por") || msg.contains("matado por") ||
                                  msg.contains("explotado por")
            hasAttackPhrase && containsMob(msg)
        }.toLong()

        // PVP deaths: attack phrase but NO known mob (killed by player)
        val pvpDeaths = deathEvents.count { death ->
            val msg = death.message.lowercase()
            if (msg.contains("lava")) return@count false
            val hasAttackPhrase = msg.contains("slain by") || msg.contains("killed by") ||
                                  msg.contains("shot by") || msg.contains("asesinado por") ||
                                  msg.contains("disparado por") || msg.contains("matado por")
            hasAttackPhrase && !containsMob(msg) && !msg.contains("explotado")
        }.toLong()

        // Environment deaths is the remainder
        val categorizedDeaths = lavaDeaths + fallDeaths + pvpDeaths + mobDeaths
        val environmentDeaths = (deathEvents.size.toLong() - categorizedDeaths).coerceAtLeast(0)

        return DeathAnalysis(
            totalDeaths = deathEvents.size.toLong(),
            deathsByPlayer = deathsByPlayer,
            deathsByCause = deathsByCause,
            lavaDeaths = lavaDeaths,
            fallDeaths = fallDeaths,
            pvpDeaths = pvpDeaths,
            mobDeaths = mobDeaths,
            environmentDeaths = environmentDeaths
        )
    }

    /**
     * Get player watch profile
     */
    fun getPlayerWatchProfile(uuid: String): PlayerWatchProfile? {
        val serverStats = statsService.getServerStats()
        val player = serverStats.players.find { it.uuid == uuid } ?: return null
        val events = logService.getHistoricalEvents(30, 5000)
        val sessions = sessionAnalysisService.getSessionStats(30)

        val playerEvents = events.filter { it.playerName == player.name }
        val pvpKills = playerEvents.count { it.type == LogEntryType.PVP_KILL }.toLong()
        val deathEvents = playerEvents.filter { it.type == LogEntryType.DEATH }

        val lavaDeaths = deathEvents.count { it.message.contains("lava", ignoreCase = true) }.toLong()
        val fallDeaths = deathEvents.count {
            it.message.contains("fell", ignoreCase = true) ||
            it.message.contains("hit the ground", ignoreCase = true)
        }.toLong()
        val timesKicked = playerEvents.count { it.type == LogEntryType.KICK }.toLong()

        val playerSessions = sessions.sessionsByPlayer[player.name] ?: emptyList()
        val avgSession = if (playerSessions.isNotEmpty()) {
            playerSessions.mapNotNull { it.durationMinutes }.average().toLong()
        } else 0L
        val longestSession = playerSessions.mapNotNull { it.durationMinutes }.maxOrNull() ?: 0L

        val riskScore = calculateRiskScore(pvpKills, lavaDeaths, fallDeaths, timesKicked, longestSession)
        val recentAlerts = generatePlayerAlerts(player, playerEvents, sessions)

        return PlayerWatchProfile(
            uuid = uuid,
            name = player.name,
            pvpKills = pvpKills,
            pvpDeaths = 0, // Would need to track deaths by PVP separately
            killDeathRatio = if (player.summary.totalDeaths > 0) pvpKills.toDouble() / player.summary.totalDeaths else pvpKills.toDouble(),
            totalDeaths = player.summary.totalDeaths,
            lavaDeaths = lavaDeaths,
            fallDeaths = fallDeaths,
            timesKicked = timesKicked,
            totalDamageDealt = player.detailedStats?.damageDealt ?: 0L,
            totalItemsDropped = player.detailedStats?.itemsDropped ?: 0L,
            averageSessionMinutes = avgSession,
            longestSessionMinutes = longestSession,
            lastActivity = playerEvents.lastOrNull()?.fullDateTime ?: "Unknown",
            riskScore = riskScore,
            recentAlerts = recentAlerts
        )
    }

    private fun buildPvpLeaderboard(
        pvpKillsByPlayer: Map<String, Long>,
        serverStats: ServerStats
    ): List<PvpLeaderboardEntry> {
        // Get list of known player names from server stats
        val knownPlayerNames = serverStats.players.map { it.name }.toSet()

        return pvpKillsByPlayer.entries
            // FIX: Filter out "Unknown" and validate against known players
            .filter { (name, _) ->
                name != "Unknown" &&
                name.isNotBlank() &&
                knownPlayerNames.contains(name)
            }
            .sortedByDescending { it.value }
            .take(10)
            .map { (name, kills) ->
                val player = serverStats.players.find { it.name == name }
                val deaths = player?.summary?.totalDeaths ?: 1L
                PvpLeaderboardEntry(
                    playerName = name,
                    playerUuid = player?.uuid,
                    kills = kills,
                    deaths = deaths,
                    kd = if (deaths > 0) kills.toDouble() / deaths else kills.toDouble()
                )
            }
    }

    private fun generateAlerts(
        events: List<LogEntry>,
        serverStats: ServerStats,
        days: Int
    ): List<SuspiciousActivity> {
        val alerts = mutableListOf<SuspiciousActivity>()

        // PVP Kills
        events.filter { it.type == LogEntryType.PVP_KILL }.forEach { event ->
            alerts.add(SuspiciousActivity(
                type = SuspiciousActivityType.PVP_KILL,
                playerName = event.playerName ?: "Unknown",
                playerUuid = null,
                description = event.message,
                timestamp = event.fullDateTime,
                severity = AlertSeverity.MEDIUM
            ))
        }

        // Kicks
        events.filter { it.type == LogEntryType.KICK }.forEach { event ->
            alerts.add(SuspiciousActivity(
                type = SuspiciousActivityType.KICK,
                playerName = event.playerName ?: "Unknown",
                playerUuid = null,
                description = event.message,
                timestamp = event.fullDateTime,
                severity = AlertSeverity.HIGH
            ))
        }

        // Bans
        events.filter { it.type == LogEntryType.BAN }.forEach { event ->
            alerts.add(SuspiciousActivity(
                type = SuspiciousActivityType.BAN,
                playerName = event.playerName ?: "Unknown",
                playerUuid = null,
                description = event.message,
                timestamp = event.fullDateTime,
                severity = AlertSeverity.CRITICAL
            ))
        }

        // Multiple lava deaths for same player
        val deathsByPlayerByType = events
            .filter { it.type == LogEntryType.DEATH }
            .groupBy { it.playerName }
            .mapValues { (_, deaths) ->
                deaths.filter { it.message.contains("lava", ignoreCase = true) }
            }

        deathsByPlayerByType.forEach { (player, lavaDeaths) ->
            if (lavaDeaths.size >= 3) {
                alerts.add(SuspiciousActivity(
                    type = SuspiciousActivityType.LAVA_DEATHS,
                    playerName = player ?: "Unknown",
                    playerUuid = null,
                    description = "$player ha muerto ${lavaDeaths.size} veces en lava",
                    timestamp = lavaDeaths.lastOrNull()?.fullDateTime ?: "",
                    severity = AlertSeverity.MEDIUM,
                    details = mapOf("count" to lavaDeaths.size)
                ))
            }
        }

        return alerts.sortedByDescending { it.timestamp }
    }

    private fun generatePlayerAlerts(
        player: PlayerStats,
        events: List<LogEntry>,
        sessions: SessionStats
    ): List<SuspiciousActivity> {
        val alerts = mutableListOf<SuspiciousActivity>()

        // Check for long sessions
        val playerSessions = sessions.sessionsByPlayer[player.name] ?: emptyList()
        playerSessions.filter { (it.durationMinutes ?: 0) > 480 }.forEach { session -> // > 8 hours
            alerts.add(SuspiciousActivity(
                type = SuspiciousActivityType.LONG_SESSION,
                playerName = player.name,
                playerUuid = player.uuid,
                description = "Sesion de ${session.durationFormatted}",
                timestamp = session.joinTime,
                severity = AlertSeverity.LOW
            ))
        }

        // PVP kills
        events.filter { it.type == LogEntryType.PVP_KILL }.take(10).forEach { event ->
            alerts.add(SuspiciousActivity(
                type = SuspiciousActivityType.PVP_KILL,
                playerName = player.name,
                playerUuid = player.uuid,
                description = event.message,
                timestamp = event.fullDateTime,
                severity = AlertSeverity.MEDIUM
            ))
        }

        return alerts.take(20)
    }

    private fun identifyHighRiskPlayers(
        serverStats: ServerStats,
        events: List<LogEntry>,
        days: Int
    ): List<PlayerWatchProfile> {
        return serverStats.players
            .mapNotNull { player -> getPlayerWatchProfile(player.uuid) }
            .filter { it.riskScore >= 30 }
            .sortedByDescending { it.riskScore }
            .take(10)
    }

    private fun categorizeDeaths(events: List<LogEntry>): Map<String, Long> {
        val deaths = events.filter { it.type == LogEntryType.DEATH }

        // Known mobs from Minecraft wiki (lowercase for case-insensitive comparison)
        // Order matters: check compound names first (e.g., "zombie villager" before "zombie")
        val knownMobs = listOf(
            // Compound names first
            "zombified piglin", "zombie villager", "wither skeleton", "magma cube",
            "piglin brute", "elder guardian", "cave spider", "ender dragon", "iron golem",
            "polar bear",
            // Single names
            "zombie", "skeleton", "creeper", "spider", "enderman", "witch", "slime",
            "phantom", "drowned", "husk", "stray", "blaze", "ghast",
            "piglin", "hoglin", "zoglin",
            "pillager", "vindicator", "evoker", "ravager", "vex", "guardian",
            "shulker", "endermite", "silverfish",
            "warden", "breeze", "bogged", "bee", "wolf",
            "llama", "panda", "dolphin", "goat", "fox", "wither"
        )

        // Helper to check if message contains a mob
        fun containsMob(msg: String): Boolean = knownMobs.any { mob -> msg.contains(mob) }

        // Count each category separately - MUTUALLY EXCLUSIVE
        val lavaCount = deaths.count { it.message.contains("lava", ignoreCase = true) }.toLong()

        val fallCount = deaths.count { death ->
            val msg = death.message.lowercase()
            !msg.contains("lava") &&
            (msg.contains("fell") || msg.contains("hit the ground") ||
             msg.contains("cayo") || msg.contains("caída") || msg.contains("condenado a caer"))
        }.toLong()

        val drownCount = deaths.count { death ->
            val msg = death.message.lowercase()
            !msg.contains("lava") &&
            (msg.contains("drowned") || msg.contains("ahogo") || msg.contains("se ahogo"))
        }.toLong()

        val fireCount = deaths.count { death ->
            val msg = death.message.lowercase()
            !msg.contains("lava") &&
            (msg.contains("fire") || msg.contains("burned") || msg.contains("flames") ||
             msg.contains("incendio") || msg.contains("quemado") || msg.contains("murio quemado"))
        }.toLong()

        // Explosions: "blew up", "blown up", "explotado", "fue explotado por"
        val explosionCount = deaths.count { death ->
            val msg = death.message.lowercase()
            !msg.contains("lava") &&
            (msg.contains("blew up") || msg.contains("blown up") ||
             msg.contains("exploto") || msg.contains("explotado"))
        }.toLong()

        // Suffocation: "suffocated", "asfixio"
        val suffocationCount = deaths.count { death ->
            val msg = death.message.lowercase()
            msg.contains("suffocated") || msg.contains("asfixio") || msg.contains("se asfixio")
        }.toLong()

        // Mob deaths: message contains attack phrase AND a known mob name
        // Spanish: "fue asesinado por", "fue disparado por", "fue explotado por"
        // English: "slain by", "killed by", "shot by"
        val mobCount = deaths.count { death ->
            val msg = death.message.lowercase()
            if (msg.contains("lava")) return@count false
            val hasAttackPhrase = msg.contains("slain by") || msg.contains("killed by") ||
                                  msg.contains("shot by") || msg.contains("asesinado por") ||
                                  msg.contains("disparado por") || msg.contains("matado por") ||
                                  msg.contains("explotado por")
            hasAttackPhrase && containsMob(msg)
        }.toLong()

        // PVP deaths: has attack phrase but NO known mob (killed by another player)
        val pvpCount = deaths.count { death ->
            val msg = death.message.lowercase()
            if (msg.contains("lava")) return@count false
            val hasAttackPhrase = msg.contains("slain by") || msg.contains("killed by") ||
                                  msg.contains("shot by") || msg.contains("asesinado por") ||
                                  msg.contains("disparado por") || msg.contains("matado por")
            // Exclude "explotado por" as that's explosion category
            hasAttackPhrase && !containsMob(msg) && !msg.contains("explotado")
        }.toLong()

        // "Otros" is the remainder after all categorized deaths
        val categorizedCount = lavaCount + fallCount + drownCount + fireCount + explosionCount + suffocationCount + pvpCount + mobCount
        val otrosCount = (deaths.size.toLong() - categorizedCount).coerceAtLeast(0)

        return mapOf(
            "Lava" to lavaCount,
            "Caida" to fallCount,
            "Ahogamiento" to drownCount,
            "Fuego" to fireCount,
            "Explosion" to explosionCount,
            "Asfixia" to suffocationCount,
            "PVP" to pvpCount,
            "Mobs" to mobCount,
            "Otros" to otrosCount
        )
    }

    private fun calculateRiskScore(
        pvpKills: Long,
        lavaDeaths: Long,
        fallDeaths: Long,
        timesKicked: Long,
        longestSession: Long
    ): Int {
        var score = 0

        // PVP kills contribute to risk
        score += (pvpKills * 5).toInt().coerceAtMost(30)

        // Multiple lava deaths might indicate being trapped
        if (lavaDeaths >= 3) score += 10
        if (lavaDeaths >= 5) score += 10

        // Multiple fall deaths might indicate being pushed
        if (fallDeaths >= 5) score += 10
        if (fallDeaths >= 10) score += 10

        // Kicks are a red flag
        score += (timesKicked * 15).toInt().coerceAtMost(30)

        // Very long sessions might indicate AFK farming
        if (longestSession > 720) score += 10 // > 12 hours

        return score.coerceIn(0, 100)
    }
}
