package com.apptolast.minecraftstats.service

import com.apptolast.minecraftstats.model.LiveUpdate
import com.apptolast.minecraftstats.model.LogEntry
import com.apptolast.minecraftstats.model.LogEntryType
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Service for broadcasting real-time updates via WebSocket
 */
@Service
class LiveUpdateService(
    private val messagingTemplate: SimpMessagingTemplate,
    private val serverStatusService: ServerStatusService,
    private val logService: LogService,
    private val statsService: StatsService
) {
    private val logger = LoggerFactory.getLogger(LiveUpdateService::class.java)
    
    // Keep track of last seen log entries to detect new ones
    private var lastSeenLogTimestamp: String? = null
    private val recentBroadcasts = ConcurrentLinkedQueue<String>()
    
    /**
     * Broadcast server status every 30 seconds
     */
    @Scheduled(fixedRate = 30000)
    fun broadcastServerStatus() {
        try {
            val status = serverStatusService.getServerStatus()
            val update = LiveUpdate(
                type = "SERVER_STATUS",
                data = status
            )
            messagingTemplate.convertAndSend("/topic/status", update)
            logger.debug("Broadcasted server status: ${status.playerCount} players online")
        } catch (e: Exception) {
            logger.error("Error broadcasting server status: ${e.message}")
        }
    }
    
    /**
     * Check for new log events every 10 seconds
     */
    @Scheduled(fixedRate = 10000)
    fun broadcastNewEvents() {
        try {
            val recentEvents = logService.getRecentEvents(20)
            
            if (recentEvents.isEmpty()) return
            
            // Find new events since last check
            val newEvents = if (lastSeenLogTimestamp != null) {
                recentEvents.dropWhile { it.timestamp != lastSeenLogTimestamp }.drop(1)
            } else {
                // First run, just remember the last timestamp
                listOf()
            }
            
            lastSeenLogTimestamp = recentEvents.lastOrNull()?.timestamp
            
            newEvents.forEach { event ->
                // Avoid duplicate broadcasts
                val eventKey = "${event.timestamp}-${event.type}-${event.playerName}"
                if (!recentBroadcasts.contains(eventKey)) {
                    recentBroadcasts.add(eventKey)
                    if (recentBroadcasts.size > 100) recentBroadcasts.poll()
                    
                    val update = LiveUpdate(
                        type = event.type.name,
                        data = event
                    )
                    messagingTemplate.convertAndSend("/topic/events", update)
                    logger.debug("Broadcasted event: ${event.type} - ${event.playerName}")
                }
            }
        } catch (e: Exception) {
            logger.error("Error broadcasting events: ${e.message}")
        }
    }
    
    /**
     * Broadcast stats update every 5 minutes
     */
    @Scheduled(fixedRate = 300000)
    fun broadcastStatsUpdate() {
        try {
            val stats = statsService.getServerStats()
            val update = LiveUpdate(
                type = "STATS_UPDATE",
                data = mapOf(
                    "totalPlayers" to stats.totalPlayers,
                    "serverTotals" to stats.serverTotals,
                    "leaderboards" to stats.leaderboards
                )
            )
            messagingTemplate.convertAndSend("/topic/stats", update)
            logger.debug("Broadcasted stats update")
        } catch (e: Exception) {
            logger.error("Error broadcasting stats: ${e.message}")
        }
    }
}
