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
            val detailed = extractDetailedStats(statsData.stats)
            
            PlayerStats(
                uuid = uuid,
                name = playerName,
                stats = statsData.stats,
                summary = summary,
                detailedStats = detailed
            )
        } catch (e: Exception) {
            logger.error("Error loading stats for UUID $uuid: ${e.message}")
            null
        }
    }
    
    /**
     * Extract detailed stats from minecraft:custom category
     * Based on official Minecraft statistics: https://minecraft.wiki/w/Statistics
     */
    private fun extractDetailedStats(stats: StatsCategories): DetailedPlayerStats {
        val custom = stats.custom
        return DetailedPlayerStats(
            // Combat
            damageDealt = custom["minecraft:damage_dealt"] ?: 0L,
            damageTaken = custom["minecraft:damage_taken"] ?: 0L,
            damageBlocked = custom["minecraft:damage_blocked_by_shield"] ?: 0L,
            playerKills = custom["minecraft:player_kills"] ?: 0L,
            mobKills = custom["minecraft:mob_kills"] ?: 0L,
            // NEW: Detailed damage stats from official wiki
            damageDealtAbsorbed = custom["minecraft:damage_dealt_absorbed"] ?: 0L,
            damageDealtResisted = custom["minecraft:damage_dealt_resisted"] ?: 0L,
            damageAbsorbed = custom["minecraft:damage_absorbed"] ?: 0L,
            damageResisted = custom["minecraft:damage_resisted"] ?: 0L,

            // Movement (all in cm)
            walkDistance = custom["minecraft:walk_one_cm"] ?: 0L,
            sprintDistance = custom["minecraft:sprint_one_cm"] ?: 0L,
            swimDistance = custom["minecraft:swim_one_cm"] ?: 0L,
            climbDistance = custom["minecraft:climb_one_cm"] ?: 0L,
            flyDistance = custom["minecraft:fly_one_cm"] ?: 0L,
            boatDistance = custom["minecraft:boat_one_cm"] ?: 0L,
            horseDistance = custom["minecraft:horse_one_cm"] ?: 0L,
            pigDistance = custom["minecraft:pig_one_cm"] ?: 0L,
            striderDistance = custom["minecraft:strider_one_cm"] ?: 0L,
            elytraDistance = custom["minecraft:aviate_one_cm"] ?: 0L,
            fallDistance = custom["minecraft:fall_one_cm"] ?: 0L,
            crouchDistance = custom["minecraft:crouch_one_cm"] ?: 0L,
            walkOnWaterDistance = custom["minecraft:walk_on_water_one_cm"] ?: 0L,
            walkUnderWaterDistance = custom["minecraft:walk_under_water_one_cm"] ?: 0L,
            // NEW: From official wiki
            minecartDistance = custom["minecraft:minecart_one_cm"] ?: 0L,

            // Interactions - Containers (NEW from official wiki)
            chestsOpened = custom["minecraft:open_chest"] ?: 0L,
            enderChestsOpened = custom["minecraft:open_enderchest"] ?: 0L,
            barrelsOpened = custom["minecraft:open_barrel"] ?: 0L,
            shulkerBoxesOpened = custom["minecraft:open_shulker_box"] ?: 0L,
            trappedChestsTriggered = custom["minecraft:trigger_trapped_chest"] ?: 0L,

            // Interactions - Workstations
            craftingTableUses = custom["minecraft:interact_with_crafting_table"] ?: 0L,
            furnaceUses = custom["minecraft:interact_with_furnace"] ?: 0L,
            anvilUses = custom["minecraft:interact_with_anvil"] ?: 0L,
            enchantingTableUses = custom["minecraft:interact_with_enchanting_table"] ?: 0L,
            smithingTableUses = custom["minecraft:interact_with_smithing_table"] ?: 0L,
            brewingStandUses = custom["minecraft:interact_with_brewingstand"] ?: 0L,
            beaconUses = custom["minecraft:interact_with_beacon"] ?: 0L,
            stonecutterUses = custom["minecraft:interact_with_stonecutter"] ?: 0L,
            smokerUses = custom["minecraft:interact_with_smoker"] ?: 0L,
            blastFurnaceUses = custom["minecraft:interact_with_blast_furnace"] ?: 0L,
            // NEW workstations from official wiki
            campfireUses = custom["minecraft:interact_with_campfire"] ?: 0L,
            cartographyTableUses = custom["minecraft:interact_with_cartography_table"] ?: 0L,
            loomUses = custom["minecraft:interact_with_loom"] ?: 0L,
            grindstoneUses = custom["minecraft:interact_with_grindstone"] ?: 0L,
            lecternUses = custom["minecraft:interact_with_lectern"] ?: 0L,

            // Actions
            timesSlept = custom["minecraft:sleep_in_bed"] ?: 0L,
            sneakTime = custom["minecraft:sneak_time"] ?: 0L,
            fishCaught = custom["minecraft:fish_caught"] ?: 0L,
            animalsBreed = custom["minecraft:animals_bred"] ?: 0L,
            itemsEnchanted = custom["minecraft:enchant_item"] ?: 0L,
            recordsPlayed = custom["minecraft:play_record"] ?: 0L,
            bellsRung = custom["minecraft:bell_ring"] ?: 0L,
            raidWins = custom["minecraft:raid_win"] ?: 0L,
            raidTriggers = custom["minecraft:raid_trigger"] ?: 0L,
            targetsHit = custom["minecraft:target_hit"] ?: 0L,
            // NEW actions from official wiki
            noteBlocksPlayed = custom["minecraft:play_noteblock"] ?: 0L,
            noteBlocksTuned = custom["minecraft:tune_noteblock"] ?: 0L,
            cakeSlicesEaten = custom["minecraft:eat_cake_slice"] ?: 0L,
            cauldronsUsed = custom["minecraft:use_cauldron"] ?: 0L,
            cauldronsFilled = custom["minecraft:fill_cauldron"] ?: 0L,
            flowersPotted = custom["minecraft:pot_flower"] ?: 0L,
            armorCleaned = custom["minecraft:clean_armor"] ?: 0L,
            bannersCleaned = custom["minecraft:clean_banner"] ?: 0L,
            shulkerBoxesCleaned = custom["minecraft:clean_shulker_box"] ?: 0L,
            gamesLeft = custom["minecraft:leave_game"] ?: 0L,
            itemsDropped = custom["minecraft:drop"] ?: 0L,

            // Villagers
            villagersTraded = custom["minecraft:traded_with_villager"] ?: 0L,
            villagersTalked = custom["minecraft:talked_to_villager"] ?: 0L,

            // Time-based
            timeSinceRest = custom["minecraft:time_since_rest"] ?: 0L,
            timeSinceDeath = custom["minecraft:time_since_death"] ?: 0L,
            totalWorldTime = custom["minecraft:total_world_time"] ?: 0L
        )
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
                .map { it.copy(formattedValue = formatDistance(it.value)) },
            // Existing leaderboards
            mostItemsCrafted = buildLeaderboard(players, "items crafteados") { it.summary.totalItemsCrafted },
            mostDamageDealt = buildLeaderboard(players, "daño") { it.detailedStats?.damageDealt ?: 0L },
            mostJumps = buildLeaderboard(players, "saltos") { it.summary.jumps },
            mostFishCaught = buildLeaderboard(players, "peces") { it.detailedStats?.fishCaught ?: 0L },
            mostVillagerTrades = buildLeaderboard(players, "comercios") { it.detailedStats?.villagersTraded ?: 0L },
            // NEW leaderboards from official wiki stats
            mostAnimalsBreed = buildLeaderboard(players, "animales criados") { it.detailedStats?.animalsBreed ?: 0L },
            mostDamageBlocked = buildLeaderboard(players, "daño bloqueado") { it.detailedStats?.damageBlocked ?: 0L },
            mostEnderChestsOpened = buildLeaderboard(players, "ender chests") { it.detailedStats?.enderChestsOpened ?: 0L },
            mostMinecartDistance = buildLeaderboard(players, "") { it.detailedStats?.minecartDistance ?: 0L }
                .map { it.copy(formattedValue = formatDistance(it.value)) },
            mostCakeSlicesEaten = buildLeaderboard(players, "trozos de tarta") { it.detailedStats?.cakeSlicesEaten ?: 0L },
            mostRaidWins = buildLeaderboard(players, "redadas ganadas") { it.detailedStats?.raidWins ?: 0L },
            mostItemsEnchanted = buildLeaderboard(players, "items encantados") { it.detailedStats?.itemsEnchanted ?: 0L },
            mostItemsDropped = buildLeaderboard(players, "items dropeados") { it.detailedStats?.itemsDropped ?: 0L }
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
        val totalDamageDealt = players.sumOf { it.detailedStats?.damageDealt ?: 0L }
        val totalDistanceTraveled = players.sumOf {
            val d = it.detailedStats
            (d?.walkDistance ?: 0L) + (d?.sprintDistance ?: 0L) + (d?.swimDistance ?: 0L) +
            (d?.boatDistance ?: 0L) + (d?.horseDistance ?: 0L) + (d?.flyDistance ?: 0L) +
            (d?.minecartDistance ?: 0L) + (d?.elytraDistance ?: 0L)
        }
        val totalChestsOpened = players.sumOf { it.detailedStats?.chestsOpened ?: 0L }

        // Existing totals
        val totalJumps = players.sumOf { it.summary.jumps }
        val totalFishCaught = players.sumOf { it.detailedStats?.fishCaught ?: 0L }
        val totalAnimalsBred = players.sumOf { it.detailedStats?.animalsBreed ?: 0L }
        val totalVillagerTrades = players.sumOf { it.detailedStats?.villagersTraded ?: 0L }
        val totalTimesSlept = players.sumOf { it.detailedStats?.timesSlept ?: 0L }

        // NEW totals from official wiki
        val totalMobKills = players.sumOf { it.detailedStats?.mobKills ?: 0L }
        val totalEnderChestsOpened = players.sumOf { it.detailedStats?.enderChestsOpened ?: 0L }
        val totalBarrelsOpened = players.sumOf { it.detailedStats?.barrelsOpened ?: 0L }
        val totalShulkerBoxesOpened = players.sumOf { it.detailedStats?.shulkerBoxesOpened ?: 0L }
        val totalMinecartDistance = players.sumOf { it.detailedStats?.minecartDistance ?: 0L }
        val totalNoteBlocksPlayed = players.sumOf { it.detailedStats?.noteBlocksPlayed ?: 0L }
        val totalCakeSlicesEaten = players.sumOf { it.detailedStats?.cakeSlicesEaten ?: 0L }
        val totalItemsDropped = players.sumOf { it.detailedStats?.itemsDropped ?: 0L }
        val totalDamageBlocked = players.sumOf { it.detailedStats?.damageBlocked ?: 0L }
        val totalRaidWins = players.sumOf { it.detailedStats?.raidWins ?: 0L }
        val totalItemsEnchanted = players.sumOf { it.detailedStats?.itemsEnchanted ?: 0L }

        return ServerTotals(
            totalBlocksMined = totalBlocksMined,
            totalItemsCrafted = totalItemsCrafted,
            totalMobsKilled = totalMobsKilled,
            totalDeaths = totalDeaths,
            totalPlayTimeTicks = totalPlayTimeTicks,
            totalPlayTimeFormatted = formatPlayTime(totalPlayTimeTicks),
            totalDamageDealt = totalDamageDealt,
            totalDistanceTraveled = totalDistanceTraveled,
            totalChestsOpened = totalChestsOpened,
            totalJumps = totalJumps,
            totalFishCaught = totalFishCaught,
            totalAnimalsBred = totalAnimalsBred,
            totalVillagerTrades = totalVillagerTrades,
            totalTimesSlept = totalTimesSlept,
            // NEW totals
            totalMobKills = totalMobKills,
            totalEnderChestsOpened = totalEnderChestsOpened,
            totalBarrelsOpened = totalBarrelsOpened,
            totalShulkerBoxesOpened = totalShulkerBoxesOpened,
            totalMinecartDistance = totalMinecartDistance,
            totalNoteBlocksPlayed = totalNoteBlocksPlayed,
            totalCakeSlicesEaten = totalCakeSlicesEaten,
            totalItemsDropped = totalItemsDropped,
            totalDamageBlocked = totalDamageBlocked,
            totalRaidWins = totalRaidWins,
            totalItemsEnchanted = totalItemsEnchanted
        )
    }
}
