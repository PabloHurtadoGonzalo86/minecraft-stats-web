package com.apptolast.minecraftstats.service

import com.apptolast.minecraftstats.config.MinecraftProperties
import com.apptolast.minecraftstats.model.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.GZIPInputStream

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
    
    // Date pattern from log filenames: 2025-12-12-1.log.gz
    private val logFileDatePattern = Regex("""(\d{4}-\d{2}-\d{2})-\d+\.log\.gz""")
    
    private val spanishDayFormatter = DateTimeFormatter.ofPattern("EEEE", Locale("es", "ES"))
    private val fullDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
    private val dateOnlyFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val logFileDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    private fun getBasePath(): String {
        val statsPath = properties.statsPath
        return statsPath.substringBefore("/server_chavalda").substringBefore("/world")
    }
    
    /**
     * Get logs from the last N days (default 30 days = 1 month)
     */
    fun getHistoricalLogs(days: Int = 30, maxLines: Int = 5000): List<LogEntry> {
        val logsPath = "${getBasePath()}/logs"
        val logsDir = File(logsPath)
        
        if (!logsDir.exists() || !logsDir.isDirectory) {
            logger.warn("Logs directory not found: $logsPath")
            return emptyList()
        }
        
        val cutoffDate = LocalDate.now().minusDays(days.toLong())
        val allEntries = mutableListOf<LogEntry>()
        
        // Get all .gz log files from the last N days
        val gzFiles = logsDir.listFiles { file -> 
            file.name.endsWith(".log.gz") 
        }?.filter { file ->
            val match = logFileDatePattern.find(file.name)
            if (match != null) {
                val dateStr = match.groupValues[1]
                try {
                    val fileDate = LocalDate.parse(dateStr, logFileDateFormatter)
                    !fileDate.isBefore(cutoffDate)
                } catch (e: Exception) {
                    false
                }
            } else false
        }?.sortedBy { it.name } ?: emptyList()
        
        // Read each .gz file
        for (gzFile in gzFiles) {
            try {
                val fileDate = extractDateFromFilename(gzFile.name)
                val entries = readGzipLog(gzFile, fileDate)
                allEntries.addAll(entries)
            } catch (e: Exception) {
                logger.warn("Error reading ${gzFile.name}: ${e.message}")
            }
        }
        
        // Add latest.log entries
        allEntries.addAll(getRecentLogs(maxLines))
        
        return allEntries.takeLast(maxLines)
    }
    
    private fun extractDateFromFilename(filename: String): LocalDate {
        val match = logFileDatePattern.find(filename)
        return if (match != null) {
            LocalDate.parse(match.groupValues[1], logFileDateFormatter)
        } else {
            LocalDate.now()
        }
    }
    
    private fun readGzipLog(file: File, fileDate: LocalDate): List<LogEntry> {
        return try {
            GZIPInputStream(file.inputStream()).use { gzis ->
                BufferedReader(InputStreamReader(gzis)).use { reader ->
                    reader.readLines().mapNotNull { line -> 
                        parseLogLine(line, fileDate) 
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error reading gzip file ${file.name}: ${e.message}")
            emptyList()
        }
    }
    
    fun getRecentLogs(maxLines: Int = 100): List<LogEntry> {
        val logsPath = "${getBasePath()}/logs"
        val latestLog = File(logsPath, "latest.log")
        
        if (!latestLog.exists()) {
            logger.warn("Latest log not found: ${latestLog.absolutePath}")
            return emptyList()
        }
        
        val today = LocalDate.now()
        return try {
            latestLog.readLines()
                .takeLast(maxLines)
                .mapNotNull { parseLogLine(it, today) }
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
    
    /**
     * Get historical events from the last N days
     */
    fun getHistoricalEvents(days: Int = 30, maxEvents: Int = 500): List<LogEntry> {
        return getHistoricalLogs(days, 10000)
            .filter { it.type != LogEntryType.OTHER }
            .takeLast(maxEvents)
    }
    
    /**
     * Get historical chat from the last N days
     */
    fun getHistoricalChat(days: Int = 30, maxMessages: Int = 500): List<LogEntry> {
        return getHistoricalLogs(days, 10000)
            .filter { it.type == LogEntryType.CHAT }
            .takeLast(maxMessages)
    }
    
    private fun parseLogLine(line: String, logDate: LocalDate = LocalDate.now()): LogEntry? {
        val timestamp = timestampPattern.find(line)?.groupValues?.get(1) ?: return null
        
        // Create full date/time from log date + log timestamp
        val logTime = try {
            LocalTime.parse(timestamp, DateTimeFormatter.ofPattern("HH:mm:ss"))
        } catch (e: Exception) {
            LocalTime.now()
        }
        val fullDateTime = LocalDateTime.of(logDate, logTime)
        val fullDateTimeStr = fullDateTime.format(fullDateFormatter)
        val dateStr = logDate.format(dateOnlyFormatter)
        
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
