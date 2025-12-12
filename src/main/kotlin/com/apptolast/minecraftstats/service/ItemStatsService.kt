package com.apptolast.minecraftstats.service

import com.apptolast.minecraftstats.model.*
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

/**
 * Service for analyzing item statistics (mined, used, picked_up, killed, killed_by)
 */
@Service
class ItemStatsService(
    private val statsService: StatsService
) {
    private val logger = LoggerFactory.getLogger(ItemStatsService::class.java)
    
    /**
     * Get top mined blocks across all players
     */
    @Cacheable("topMined", unless = "#result.isEmpty()")
    fun getTopMinedBlocks(limit: Int = 20): ItemLeaderboard {
        val serverStats = statsService.getServerStats()
        val aggregated = mutableMapOf<String, Long>()
        
        serverStats.players.forEach { player ->
            player.stats.mined.forEach { (item, count) ->
                aggregated[item] = aggregated.getOrDefault(item, 0) + count
            }
        }
        
        val entries = aggregated.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { (item, count) ->
                ItemEntry(
                    itemId = item,
                    itemName = formatItemName(item),
                    count = count,
                    iconUrl = getItemIconUrl(item)
                )
            }
        
        return ItemLeaderboard(
            category = "mined",
            categoryDisplay = "Bloques Minados",
            entries = entries
        )
    }
    
    /**
     * Get top used items across all players
     */
    @Cacheable("topUsed", unless = "#result.isEmpty()")
    fun getTopUsedItems(limit: Int = 20): ItemLeaderboard {
        val serverStats = statsService.getServerStats()
        val aggregated = mutableMapOf<String, Long>()
        
        serverStats.players.forEach { player ->
            player.stats.used.forEach { (item, count) ->
                aggregated[item] = aggregated.getOrDefault(item, 0) + count
            }
        }
        
        val entries = aggregated.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { (item, count) ->
                ItemEntry(
                    itemId = item,
                    itemName = formatItemName(item),
                    count = count,
                    iconUrl = getItemIconUrl(item)
                )
            }
        
        return ItemLeaderboard(
            category = "used",
            categoryDisplay = "Items Usados",
            entries = entries
        )
    }
    
    /**
     * Get top picked up items across all players
     */
    @Cacheable("topPickedUp", unless = "#result.isEmpty()")
    fun getTopPickedUpItems(limit: Int = 20): ItemLeaderboard {
        val serverStats = statsService.getServerStats()
        val aggregated = mutableMapOf<String, Long>()
        
        serverStats.players.forEach { player ->
            player.stats.pickedUp.forEach { (item, count) ->
                aggregated[item] = aggregated.getOrDefault(item, 0) + count
            }
        }
        
        val entries = aggregated.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { (item, count) ->
                ItemEntry(
                    itemId = item,
                    itemName = formatItemName(item),
                    count = count,
                    iconUrl = getItemIconUrl(item)
                )
            }
        
        return ItemLeaderboard(
            category = "picked_up",
            categoryDisplay = "Items Recogidos",
            entries = entries
        )
    }
    
    /**
     * Get top killed mobs across all players
     */
    @Cacheable("topKilled", unless = "#result.isEmpty()")
    fun getTopKilledMobs(limit: Int = 20): ItemLeaderboard {
        val serverStats = statsService.getServerStats()
        val aggregated = mutableMapOf<String, Long>()
        
        serverStats.players.forEach { player ->
            player.stats.killed.forEach { (mob, count) ->
                aggregated[mob] = aggregated.getOrDefault(mob, 0) + count
            }
        }
        
        val entries = aggregated.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { (mob, count) ->
                ItemEntry(
                    itemId = mob,
                    itemName = formatMobName(mob),
                    count = count,
                    iconUrl = getMobIconUrl(mob)
                )
            }
        
        return ItemLeaderboard(
            category = "killed",
            categoryDisplay = "Mobs Eliminados",
            entries = entries
        )
    }
    
    /**
     * Get deaths by mob type across all players
     */
    @Cacheable("topKilledBy", unless = "#result.isEmpty()")
    fun getTopKilledByMobs(limit: Int = 20): ItemLeaderboard {
        val serverStats = statsService.getServerStats()
        val aggregated = mutableMapOf<String, Long>()
        
        serverStats.players.forEach { player ->
            player.stats.killedBy.forEach { (mob, count) ->
                aggregated[mob] = aggregated.getOrDefault(mob, 0) + count
            }
        }
        
        val entries = aggregated.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { (mob, count) ->
                ItemEntry(
                    itemId = mob,
                    itemName = formatMobName(mob),
                    count = count,
                    iconUrl = getMobIconUrl(mob)
                )
            }
        
        return ItemLeaderboard(
            category = "killed_by",
            categoryDisplay = "Muertes por Mob",
            entries = entries
        )
    }
    
    /**
     * Get top crafted items across all players
     */
    @Cacheable("topCrafted", unless = "#result.isEmpty()")
    fun getTopCraftedItems(limit: Int = 20): ItemLeaderboard {
        val serverStats = statsService.getServerStats()
        val aggregated = mutableMapOf<String, Long>()
        
        serverStats.players.forEach { player ->
            player.stats.crafted.forEach { (item, count) ->
                aggregated[item] = aggregated.getOrDefault(item, 0) + count
            }
        }
        
        val entries = aggregated.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { (item, count) ->
                ItemEntry(
                    itemId = item,
                    itemName = formatItemName(item),
                    count = count,
                    iconUrl = getItemIconUrl(item)
                )
            }
        
        return ItemLeaderboard(
            category = "crafted",
            categoryDisplay = "Items Crafteados",
            entries = entries
        )
    }
    
    /**
     * Get item stats for a specific player
     */
    fun getPlayerItemStats(uuid: String): PlayerItemStats? {
        val playerStats = statsService.getPlayerStats(uuid) ?: return null
        
        return PlayerItemStats(
            uuid = uuid,
            name = playerStats.name,
            topMined = playerStats.stats.mined.entries
                .sortedByDescending { it.value }
                .take(10)
                .map { ItemEntry(it.key, formatItemName(it.key), it.value, getItemIconUrl(it.key)) },
            topUsed = playerStats.stats.used.entries
                .sortedByDescending { it.value }
                .take(10)
                .map { ItemEntry(it.key, formatItemName(it.key), it.value, getItemIconUrl(it.key)) },
            topPickedUp = playerStats.stats.pickedUp.entries
                .sortedByDescending { it.value }
                .take(10)
                .map { ItemEntry(it.key, formatItemName(it.key), it.value, getItemIconUrl(it.key)) },
            topKilled = playerStats.stats.killed.entries
                .sortedByDescending { it.value }
                .take(10)
                .map { ItemEntry(it.key, formatMobName(it.key), it.value, getMobIconUrl(it.key)) },
            topKilledBy = playerStats.stats.killedBy.entries
                .sortedByDescending { it.value }
                .take(10)
                .map { ItemEntry(it.key, formatMobName(it.key), it.value, getMobIconUrl(it.key)) }
        )
    }
    
    /**
     * Get server records
     */
    @Cacheable("records", unless = "#result == null")
    fun getServerRecords(): ServerRecords {
        val serverStats = statsService.getServerStats()
        
        fun findRecord(selector: (PlayerStats) -> Long, name: String, formatter: (Long) -> String): RecordEntry? {
            return serverStats.players
                .maxByOrNull { selector(it) }
                ?.takeIf { selector(it) > 0 }
                ?.let { player ->
                    val value = selector(player)
                    RecordEntry(
                        playerName = player.name,
                        playerUuid = player.uuid,
                        value = value,
                        formattedValue = formatter(value),
                        recordName = name
                    )
                }
        }
        
        return ServerRecords(
            mostDiamondsMined = serverStats.players
                .maxByOrNull { it.stats.mined["minecraft:diamond_ore"] ?: 0 + (it.stats.mined["minecraft:deepslate_diamond_ore"] ?: 0) }
                ?.let { player ->
                    val diamonds = (player.stats.mined["minecraft:diamond_ore"] ?: 0) + 
                                   (player.stats.mined["minecraft:deepslate_diamond_ore"] ?: 0)
                    if (diamonds > 0) RecordEntry(player.name, player.uuid, diamonds, "$diamonds 💎", "Más Diamantes Minados") else null
                },
            longestBoatDistance = findRecord(
                { it.detailedStats?.boatDistance ?: 0 },
                "Mayor Distancia en Barco",
                { "${String.format("%.2f", it / 100000.0)} km" }
            ),
            mostMobsKilled = findRecord(
                { it.summary.totalMobsKilled },
                "Más Mobs Eliminados",
                { "$it mobs" }
            ),
            mostDeaths = findRecord(
                { it.summary.totalDeaths },
                "Más Muertes",
                { "$it muertes" }
            ),
            longestPlayTime = findRecord(
                { it.summary.playTimeTicks },
                "Más Tiempo Jugado",
                { it.let { ticks -> 
                    val hours = ticks / 20 / 3600
                    val minutes = (ticks / 20 % 3600) / 60
                    "${hours}h ${minutes}m"
                }}
            ),
            mostItemsCrafted = findRecord(
                { it.summary.totalItemsCrafted },
                "Más Items Crafteados",
                { "$it items" }
            ),
            mostBlocksMined = findRecord(
                { it.summary.totalBlocksMined },
                "Más Bloques Minados",
                { "$it bloques" }
            ),
            mostFishCaught = findRecord(
                { it.detailedStats?.fishCaught ?: 0 },
                "Más Peces Capturados",
                { "$it peces" }
            ),
            mostVillagerTrades = findRecord(
                { it.detailedStats?.villagersTraded ?: 0 },
                "Más Comercios con Aldeanos",
                { "$it comercios" }
            ),
            mostJumps = findRecord(
                { it.summary.jumps },
                "Más Saltos",
                { "$it saltos" }
            )
        )
    }
    
    /**
     * Format minecraft item ID to readable name
     */
    private fun formatItemName(itemId: String): String {
        return itemId
            .removePrefix("minecraft:")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }
    
    /**
     * Format minecraft mob ID to readable name
     */
    private fun formatMobName(mobId: String): String {
        return formatItemName(mobId)
    }
    
    /**
     * Get icon URL for item (using mc-heads or similar service)
     */
    private fun getItemIconUrl(itemId: String): String {
        val cleanId = itemId.removePrefix("minecraft:")
        return "https://mc.nerothe.com/img/1.21.1/$cleanId.png"
    }
    
    /**
     * Get icon URL for mob
     */
    private fun getMobIconUrl(mobId: String): String {
        val cleanId = mobId.removePrefix("minecraft:")
        return "https://mc.nerothe.com/img/1.21.1/${cleanId}_spawn_egg.png"
    }
}
