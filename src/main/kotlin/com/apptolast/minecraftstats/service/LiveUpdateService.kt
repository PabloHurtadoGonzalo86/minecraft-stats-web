package com.apptolast.minecraftstats.service

import com.apptolast.minecraftstats.model.LiveUpdate
import com.apptolast.minecraftstats.model.LogEntryType
import com.apptolast.minecraftstats.model.RealTimePlayerStats
import com.apptolast.minecraftstats.model.ServerTime
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Service for broadcasting real-time updates via WebSocket
 * Based on: https://docs.spring.io/spring-framework/reference/web/websocket/stomp.html
 */
@Service
class LiveUpdateService(
    private val messagingTemplate: SimpMessagingTemplate,
    private val serverStatusService: ServerStatusService,
    private val logService: LogService,
    private val statsService: StatsService,
    private val itemStatsService: ItemStatsService
) {
    private val logger = LoggerFactory.getLogger(LiveUpdateService::class.java)
    
    // Use fullDateTime (includes date) to properly track seen events
    private var lastSeenEventKey: String? = null
    private var lastSeenChatKey: String? = null
    private val recentEventBroadcasts = ConcurrentLinkedDeque<String>()
    private val recentChatBroadcasts = ConcurrentLinkedDeque<String>()
    
    private val timezone = ZoneId.of("Europe/Madrid")
    private val isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    
    private fun createServerTime(): ServerTime {
        val now = LocalDateTime.now(timezone)
        val zonedNow = now.atZone(timezone)
        return ServerTime(
            timestamp = System.currentTimeMillis(),
            iso = zonedNow.format(isoFormatter),
            date = now.format(dateFormatter),
            time = now.format(timeFormatter),
            dayOfWeek = now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es", "ES"))
                .replaceFirstChar { it.uppercase() },
            timezone = "Europe/Madrid"
        )
    }
    
    private fun createLiveUpdate(type: String, data: Any): LiveUpdate {
        val serverTime = createServerTime()
        return LiveUpdate(
            type = type,
            data = data,
            timestamp = serverTime.timestamp,
            timestampFormatted = serverTime.iso,
            serverTime = serverTime
        )
    }
    
    /**
     * Broadcast server time every 1 second for real-time clock
     */
    @Scheduled(fixedRate = 1000)
    fun broadcastServerTime() {
        try {
            val update = createLiveUpdate("SERVER_TIME", createServerTime())
            messagingTemplate.convertAndSend("/topic/time", update)
        } catch (e: Exception) {
            logger.error("Error broadcasting server time: ${e.message}")
        }
    }
    
    /**
     * Broadcast server status every 1 second (real-time)
     */
    @Scheduled(fixedRate = 1000)
    fun broadcastServerStatus() {
        try {
            val status = serverStatusService.getServerStatus()
            val update = createLiveUpdate("SERVER_STATUS", status)
            messagingTemplate.convertAndSend("/topic/status", update)
        } catch (e: Exception) {
            logger.error("Error broadcasting server status: ${e.message}")
        }
    }
    
    /**
     * Broadcast player play times every 1 second (real-time counters)
     */
    @Scheduled(fixedRate = 1000)
    fun broadcastPlayerTimes() {
        try {
            val stats = statsService.getServerStats()
            val playerTimes = stats.players.map { player ->
                RealTimePlayerStats(
                    uuid = player.uuid,
                    name = player.name,
                    playTimeTicks = player.summary.playTimeTicks,
                    playTimeSeconds = player.summary.playTimeTicks / 20,
                    playTimeFormatted = player.summary.playTimeFormatted,
                    isOnline = serverStatusService.getServerStatus().onlinePlayers
                        .any { it.name == player.name }
                )
            }
            val update = createLiveUpdate("PLAYER_TIMES", playerTimes)
            messagingTemplate.convertAndSend("/topic/player-times", update)
        } catch (e: Exception) {
            logger.error("Error broadcasting player times: ${e.message}")
        }
    }
    
    /**
     * Check for new log events every 1 second for real-time updates
     */
    @Scheduled(fixedRate = 1000)
    fun broadcastNewEvents() {
        try {
            val recentEvents = logService.getRecentEvents(50)
            
            if (recentEvents.isEmpty()) return
            
            // Find new events using fullDateTime for uniqueness
            recentEvents.forEach { event ->
                val eventKey = "${event.fullDateTime}-${event.type}-${event.playerName}-${event.message?.hashCode()}"
                
                if (!recentEventBroadcasts.contains(eventKey)) {
                    recentEventBroadcasts.addLast(eventKey)
                    // Keep only last 200 to avoid memory issues
                    while (recentEventBroadcasts.size > 200) recentEventBroadcasts.removeFirst()
                    
                    val update = createLiveUpdate(event.type.name, event)
                    messagingTemplate.convertAndSend("/topic/events", update)
                    
                    // Also send chat to dedicated chat topic
                    if (event.type == LogEntryType.CHAT) {
                        messagingTemplate.convertAndSend("/topic/chat", update)
                    }
                    
                    logger.debug("Broadcasted event: ${event.type} - ${event.playerName} at ${event.fullDateTime}")
                }
            }
        } catch (e: Exception) {
            logger.error("Error broadcasting events: ${e.message}")
        }
    }
    
    /**
     * Broadcast item stats every 1 second (real-time)
     */
    @Scheduled(fixedRate = 1000)
    fun broadcastItemStats() {
        try {
            val items = mapOf(
                "mined" to itemStatsService.getTopMinedBlocks(10),
                "used" to itemStatsService.getTopUsedItems(10),
                "crafted" to itemStatsService.getTopCraftedItems(10),
                "killed" to itemStatsService.getTopKilledMobs(10),
                "killed_by" to itemStatsService.getTopKilledByMobs(10),
                "picked_up" to itemStatsService.getTopPickedUpItems(10)
            )
            val update = createLiveUpdate("ITEM_STATS", items)
            messagingTemplate.convertAndSend("/topic/items", update)
            logger.debug("Broadcasted item stats")
        } catch (e: Exception) {
            logger.error("Error broadcasting item stats: ${e.message}")
        }
    }
    
    /**
     * Broadcast stats update every 1 second (real-time)
     */
    @Scheduled(fixedRate = 1000)
    fun broadcastStatsUpdate() {
        try {
            val stats = statsService.getServerStats()
            val update = createLiveUpdate("STATS_UPDATE", mapOf(
                "totalPlayers" to stats.totalPlayers,
                "serverTotals" to stats.serverTotals,
                "leaderboards" to stats.leaderboards,
                "players" to stats.players.map { p ->
                    mapOf(
                        "uuid" to p.uuid,
                        "name" to p.name,
                        "summary" to p.summary,
                        "detailedStats" to p.detailedStats
                    )
                }
            ))
            messagingTemplate.convertAndSend("/topic/stats", update)
            logger.debug("Broadcasted stats update")
        } catch (e: Exception) {
            logger.error("Error broadcasting stats: ${e.message}")
        }
    }
}
