package com.apptolast.minecraftstats.service

import com.apptolast.minecraftstats.config.MinecraftProperties
import com.apptolast.minecraftstats.model.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Service for parsing Minecraft server logs
 */
@Service
class LogService(
    private val properties: MinecraftProperties
) {
    private val logger = LoggerFactory.getLogger(LogService::class.java)
    
    // Regex patterns for parsing log entries
    private val timestampPattern = Regex("""\[(\d{2}:\d{2}:\d{2})\]""")
    private val chatPattern = Regex("""\[Server thread/INFO\]: \[Not Secure\] <(\w+)> (.+)""")
    private val joinPattern = Regex("""\[Server thread/INFO\]: (\w+)\[.+\] logged in""")
    private val leavePattern = Regex("""\[Server thread/INFO\]: (\w+) left the game""")
    private val deathPattern = Regex("""\[Server thread/INFO\]: (\w+) (was slain|was killed|drowned|fell|burned|starved|died|blew up|hit the ground|went up in flames|walked into|tried to swim|was shot|was pummeled|was fireballed|was impaled|was squashed|was skewered|was pricked|suffocated|experienced kinetic|was blown up|was struck|withered)""")
    private val advancementPattern = Regex("""\[Server thread/INFO\]: (\w+) has (made the advancement|completed the challenge|reached the goal) \[(.+)\]""")
    
    private val spanishDayFormatter = DateTimeFormatter.ofPattern("EEEE", Locale("es", "ES"))
    private val fullDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
    private val dateOnlyFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    
    private fun getBasePath(): String {
        val statsPath = properties.statsPath
        return statsPath.substringBefore("/server_chavalda").substringBefore("/world")
    }
    
    fun getRecentLogs(maxLines: Int = 100): List<LogEntry> {
        val logsPath = "${getBasePath()}/logs"
        val latestLog = File(logsPath, "latest.log")
        
        if (!latestLog.exists()) {
            logger.warn("Latest log not found: ${latestLog.absolutePath}")
            return emptyList()
        }
        
        return try {
            latestLog.readLines()
                .takeLast(maxLines)
                .mapNotNull { parseLogLine(it) }
        } catch (e: Exception) {
            logger.error("Error reading logs: ${e.message}")
            emptyList()
        }
    }
    
    fun getRecentEvents(maxEvents: Int = 50): List<LogEntry> {
        return getRecentLogs(500)
            .filter { it.type != LogEntryType.OTHER }
            .takeLast(maxEvents)
    }
    
    fun getRecentChat(maxMessages: Int = 30): List<LogEntry> {
        return getRecentLogs(500)
            .filter { it.type == LogEntryType.CHAT }
            .takeLast(maxMessages)
    }
    
    private fun parseLogLine(line: String): LogEntry? {
        val timestamp = timestampPattern.find(line)?.groupValues?.get(1) ?: return null
        
        // Create full date/time from today's date + log timestamp
        val today = LocalDate.now()
        val logTime = try {
            LocalTime.parse(timestamp, DateTimeFormatter.ofPattern("HH:mm:ss"))
        } catch (e: Exception) {
            LocalTime.now()
        }
        val fullDateTime = LocalDateTime.of(today, logTime)
        val fullDateTimeStr = fullDateTime.format(fullDateFormatter)
        val dateStr = today.format(dateOnlyFormatter)
        
        // Check for chat message
        chatPattern.find(line)?.let { match ->
            return LogEntry(
                timestamp = timestamp,
                fullDateTime = fullDateTimeStr,
                date = dateStr,
                type = LogEntryType.CHAT,
                playerName = match.groupValues[1],
                message = match.groupValues[2],
                rawLine = line
            )
        }
        
        // Check for player join
        joinPattern.find(line)?.let { match ->
            return LogEntry(
                timestamp = timestamp,
                fullDateTime = fullDateTimeStr,
                date = dateStr,
                type = LogEntryType.JOIN,
                playerName = match.groupValues[1],
                message = "${match.groupValues[1]} se ha conectado",
                rawLine = line
            )
        }
        
        // Check for player leave
        leavePattern.find(line)?.let { match ->
            return LogEntry(
                timestamp = timestamp,
                fullDateTime = fullDateTimeStr,
                date = dateStr,
                type = LogEntryType.LEAVE,
                playerName = match.groupValues[1],
                message = "${match.groupValues[1]} se ha desconectado",
                rawLine = line
            )
        }
        
        // Check for death
        deathPattern.find(line)?.let { match ->
            return LogEntry(
                timestamp = timestamp,
                fullDateTime = fullDateTimeStr,
                date = dateStr,
                type = LogEntryType.DEATH,
                playerName = match.groupValues[1],
                message = line.substringAfter("INFO]: "),
                rawLine = line
            )
        }
        
        // Check for advancement
        advancementPattern.find(line)?.let { match ->
            return LogEntry(
                timestamp = timestamp,
                fullDateTime = fullDateTimeStr,
                date = dateStr,
                type = LogEntryType.ADVANCEMENT,
                playerName = match.groupValues[1],
                message = "${match.groupValues[1]} ha conseguido [${match.groupValues[3]}]",
                rawLine = line
            )
        }
        
        return LogEntry(
            timestamp = timestamp,
            fullDateTime = fullDateTimeStr,
            date = dateStr,
            type = LogEntryType.OTHER,
            playerName = null,
            message = line.substringAfter("INFO]: ", line),
            rawLine = line
        )
    }
}
