package com.apptolast.minecraftstats.service

import com.apptolast.minecraftstats.model.*
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Service for analyzing player sessions and activity patterns from logs
 */
@Service
class SessionAnalysisService(
    private val logService: LogService
) {
    private val logger = LoggerFactory.getLogger(SessionAnalysisService::class.java)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
    private val timezone = ZoneId.of("Europe/Madrid")
    
    /**
     * Analyze sessions from historical logs
     */
    @Cacheable("sessions", unless = "#result == null")
    fun getSessionStats(days: Int = 30): SessionStats {
        val events = logService.getHistoricalEvents(days, 5000)
        val sessions = mutableListOf<PlayerSession>()
        
        // Track open sessions (join without leave yet)
        val openSessions = mutableMapOf<String, LogEntry>()
        
        for (event in events) {
            when (event.type) {
                LogEntryType.JOIN -> {
                    // Close any existing session for this player
                    openSessions[event.playerName]?.let { joinEvent ->
                        // Player disconnected without leave event, close session
                        sessions.add(createSession(joinEvent, event, closedByNewJoin = true))
                    }
                    openSessions[event.playerName!!] = event
                }
                LogEntryType.LEAVE -> {
                    openSessions.remove(event.playerName)?.let { joinEvent ->
                        sessions.add(createSession(joinEvent, event))
                    }
                }
                else -> {}
            }
        }
        
        // Close remaining open sessions as "still online" or estimate
        openSessions.forEach { (playerName, joinEvent) ->
            sessions.add(PlayerSession(
                playerName = playerName,
                playerUuid = null,
                joinTime = joinEvent.fullDateTime,
                joinTimestamp = parseTimestamp(joinEvent.fullDateTime),
                leaveTime = null,
                leaveTimestamp = null,
                durationMinutes = null,
                durationFormatted = "En línea"
            ))
        }
        
        // Calculate statistics
        val completedSessions = sessions.filter { it.durationMinutes != null && it.durationMinutes > 0 }
        val avgMinutes = if (completedSessions.isNotEmpty()) {
            completedSessions.mapNotNull { it.durationMinutes }.average().toLong()
        } else 0L
        
        val longestSession = completedSessions.maxByOrNull { it.durationMinutes ?: 0 }
        
        // Group sessions by player
        val sessionsByPlayer = sessions.groupBy { it.playerName }
        
        return SessionStats(
            totalSessions = sessions.size,
            averageSessionMinutes = avgMinutes,
            averageSessionFormatted = formatDuration(avgMinutes),
            longestSession = longestSession,
            recentSessions = sessions.takeLast(20).reversed(),
            sessionsByPlayer = sessionsByPlayer
        )
    }
    
    /**
     * Analyze activity patterns (hourly, daily, weekly)
     */
    @Cacheable("activity", unless = "#result == null")
    fun getActivityStats(days: Int = 30): ActivityStats {
        val events = logService.getHistoricalEvents(days, 10000)
        
        // Initialize hourly counts
        val hourlyActivity = (0..23).associateWith { 0 }.toMutableMap()
        val dailyActivity = mutableMapOf<String, Int>()
        val weekdayActivity = mutableMapOf(
            "Lunes" to 0, "Martes" to 0, "Miércoles" to 0,
            "Jueves" to 0, "Viernes" to 0, "Sábado" to 0, "Domingo" to 0
        )
        
        // Track concurrent players per timestamp
        val playerCountByDate = mutableMapOf<String, MutableSet<String>>()
        
        for (event in events) {
            try {
                val dateTime = LocalDateTime.parse(event.fullDateTime, dateTimeFormatter)
                val hour = dateTime.hour
                val date = event.date
                val dayOfWeek = getDayOfWeekSpanish(dateTime.dayOfWeek.value)
                
                hourlyActivity[hour] = hourlyActivity.getOrDefault(hour, 0) + 1
                dailyActivity[date] = dailyActivity.getOrDefault(date, 0) + 1
                weekdayActivity[dayOfWeek] = weekdayActivity.getOrDefault(dayOfWeek, 0) + 1
                
                // Track unique players per date for peak calculation
                if (event.type == LogEntryType.JOIN && event.playerName != null) {
                    playerCountByDate.getOrPut(date) { mutableSetOf() }.add(event.playerName)
                }
            } catch (e: Exception) {
                logger.debug("Error parsing event date: ${event.fullDateTime}")
            }
        }
        
        // Find peaks
        val mostActiveHour = hourlyActivity.maxByOrNull { it.value }?.key ?: 0
        val mostActiveDay = weekdayActivity.maxByOrNull { it.value }?.key ?: "Desconocido"
        val peakEntry = playerCountByDate.maxByOrNull { it.value.size }
        
        return ActivityStats(
            hourlyActivity = hourlyActivity.toMap(),
            dailyActivity = dailyActivity.toMap(),
            weekdayActivity = weekdayActivity.toMap(),
            mostActiveHour = mostActiveHour,
            mostActiveDay = mostActiveDay,
            peakPlayers = peakEntry?.value?.size ?: 0,
            peakPlayersDate = peakEntry?.key ?: "N/A"
        )
    }
    
    private fun createSession(joinEvent: LogEntry, endEvent: LogEntry, closedByNewJoin: Boolean = false): PlayerSession {
        val joinTimestamp = parseTimestamp(joinEvent.fullDateTime)
        val leaveTimestamp = parseTimestamp(endEvent.fullDateTime)
        val durationMinutes = if (leaveTimestamp > joinTimestamp) {
            (leaveTimestamp - joinTimestamp) / 60000
        } else 0L
        
        return PlayerSession(
            playerName = joinEvent.playerName ?: "Unknown",
            playerUuid = null,
            joinTime = joinEvent.fullDateTime,
            joinTimestamp = joinTimestamp,
            leaveTime = if (closedByNewJoin) null else endEvent.fullDateTime,
            leaveTimestamp = if (closedByNewJoin) null else leaveTimestamp,
            durationMinutes = durationMinutes,
            durationFormatted = formatDuration(durationMinutes)
        )
    }
    
    private fun parseTimestamp(dateTimeStr: String): Long {
        return try {
            LocalDateTime.parse(dateTimeStr, dateTimeFormatter)
                .atZone(timezone)
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    
    private fun formatDuration(minutes: Long): String {
        return when {
            minutes < 1 -> "< 1 min"
            minutes < 60 -> "$minutes min"
            else -> {
                val hours = minutes / 60
                val mins = minutes % 60
                if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
            }
        }
    }
    
    private fun getDayOfWeekSpanish(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "Lunes"
            2 -> "Martes"
            3 -> "Miércoles"
            4 -> "Jueves"
            5 -> "Viernes"
            6 -> "Sábado"
            7 -> "Domingo"
            else -> "Desconocido"
        }
    }
}
