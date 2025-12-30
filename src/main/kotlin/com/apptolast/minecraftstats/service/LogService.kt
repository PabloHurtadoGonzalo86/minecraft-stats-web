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
 * Based on Minecraft server log format and death messages:
 * https://minecraft.fandom.com/wiki/Death_messages
 */
@Service
class LogService(
    private val properties: MinecraftProperties
) {
    private val logger = LoggerFactory.getLogger(LogService::class.java)

    // Regex patterns for parsing log entries
    private val timestampPattern = Regex("""\[(\d{2}:\d{2}:\d{2})\]""")
    // FIX: [Not Secure] is optional - some servers don't have it
    private val chatPattern = Regex("""\[Server thread/INFO\]: (?:\[Not Secure\] )?<(\w+)> (.+)""")
    private val joinPattern = Regex("""\[Server thread/INFO\]: (\w+)\[.+\] logged in""")
    private val leavePattern = Regex("""\[Server thread/INFO\]: (\w+) left the game""")
    private val advancementPattern = Regex("""\[Server thread/INFO\]: (\w+) has (made the advancement|completed the challenge|reached the goal) \[(.+)\]""")

    // List of known hostile mobs that can kill players (Minecraft 1.21.4 official list)
    // Source: https://minecraft.wiki/w/Mob
    private val knownMobs = setOf(
        // Hostile mobs (common)
        "Zombie", "Skeleton", "Creeper", "Spider", "Enderman", "Witch", "Slime",
        "Pillager", "Vindicator", "Evoker", "Ravager", "Vex", "Illusioner",
        "Warden", "Blaze", "Ghast", "Piglin", "Hoglin", "Zoglin",
        "Drowned", "Husk", "Stray", "Phantom", "Guardian", "Shulker", "Silverfish",
        "Endermite", "Breeze", "Bogged",
        // Cave Spider is two words but displays as "Cave Spider"
        "Cave Spider",
        // Magma Cube
        "Magma Cube", "Magma",
        // Neutral mobs that can attack
        "Wolf", "Bee", "Goat", "Llama", "Panda", "Dolphin", "Fox",
        "Polar Bear", "Polar",
        "Iron Golem", "Iron",
        "Snow Golem", "Snow",
        // Boss mobs
        "Ender Dragon", "Dragon",
        "Wither",
        "Elder Guardian", "Elder",
        "Giant",
        // Variants (prefixes/suffixes)
        "Baby Zombie", "Baby",
        "Charged Creeper", "Charged",
        "Zombified Piglin", "Zombified",
        "Zombie Pigman", "Pigman",
        "Zombie Villager",
        "Skeleton Horse",
        "Zombie Horse",
        // Wither Skeleton
        "Wither Skeleton",
        // Strider (neutral but can push into lava)
        "Strider",
        // Villager (cannot kill but appears in some messages)
        "Villager",
        // Named mobs (TNT, arrows, tridents)
        "TNT", "Arrow", "Trident", "Fireball", "Firework"
    )

    // NEW patterns for additional events
    private val commandPattern = Regex("""\[Server thread/INFO\]: (\w+) issued server command: /(.+)""")
    private val kickPattern = Regex("""\[Server thread/INFO\]: Kicked (\w+):?\s*(.*)""")
    private val banPattern = Regex("""\[Server thread/INFO\]: Banned (\w+):?\s*(.*)""")
    private val serverStartPattern = Regex("""\[Server thread/INFO\]: Done \([\d.]+s\)! For help, type "help"""")
    private val serverStopPattern = Regex("""\[Server thread/INFO\]: Stopping (the )?server""")
    private val worldSavePattern = Regex("""\[Server thread/INFO\]: Saved the (game|world)""")
    private val warningPattern = Regex("""\[Server thread/WARN\]: (.+)""")

    // PVP kill pattern - Player was slain/killed by another player
    private val pvpKillPattern = Regex("""\[Server thread/INFO\]: (\w+) was (slain|killed|shot) by (\w+)""")

    // Comprehensive death patterns based on https://minecraft.fandom.com/wiki/Death_messages
    private val deathPatterns = listOf(
        // Generic death
        Regex("""(\w+) died"""),
        Regex("""(\w+) was killed"""),

        // Combat - Melee
        Regex("""(\w+) was slain by (.+)"""),
        Regex("""(\w+) was killed by (.+)"""),
        Regex("""(\w+) got finished off by (.+)"""),

        // Combat - Ranged
        Regex("""(\w+) was shot by (.+)"""),
        Regex("""(\w+) was fireballed by (.+)"""),
        Regex("""(\w+) was pummeled by (.+)"""),
        Regex("""(\w+) was impaled by (.+)"""),
        Regex("""(\w+) was skewered by (.+)"""),

        // Fall damage
        Regex("""(\w+) hit the ground too hard"""),
        Regex("""(\w+) fell from a high place"""),
        Regex("""(\w+) fell off a ladder"""),
        Regex("""(\w+) fell off some vines"""),
        Regex("""(\w+) fell off scaffolding"""),
        Regex("""(\w+) fell off some weeping vines"""),
        Regex("""(\w+) fell off some twisting vines"""),
        Regex("""(\w+) fell while climbing"""),
        Regex("""(\w+) was doomed to fall"""),
        Regex("""(\w+) fell too far and was finished by (.+)"""),

        // Drowning
        Regex("""(\w+) drowned"""),
        Regex("""(\w+) drowned whilst trying to escape (.+)"""),

        // Fire/Lava
        Regex("""(\w+) went up in flames"""),
        Regex("""(\w+) burned to death"""),
        Regex("""(\w+) was burnt to a crisp whilst fighting (.+)"""),
        Regex("""(\w+) walked into fire whilst fighting (.+)"""),
        Regex("""(\w+) tried to swim in lava"""),
        Regex("""(\w+) tried to swim in lava to escape (.+)"""),

        // Explosions
        Regex("""(\w+) blew up"""),
        Regex("""(\w+) was blown up by (.+)"""),
        Regex("""(\w+) was killed by \[Intentional Game Design\]"""),

        // Suffocation/Crushing
        Regex("""(\w+) suffocated in a wall"""),
        Regex("""(\w+) was squished too much"""),
        Regex("""(\w+) was squashed by (.+)"""),

        // Environment
        Regex("""(\w+) was pricked to death"""),
        Regex("""(\w+) walked into a cactus whilst trying to escape (.+)"""),
        Regex("""(\w+) was struck by lightning"""),
        Regex("""(\w+) discovered the floor was lava"""),
        Regex("""(\w+) froze to death"""),
        Regex("""(\w+) was frozen to death by (.+)"""),
        Regex("""(\w+) was stung to death"""),
        Regex("""(\w+) was obliterated by a sonically-charged shriek"""),

        // Starvation
        Regex("""(\w+) starved to death"""),

        // Magic/Wither
        Regex("""(\w+) was killed by magic"""),
        Regex("""(\w+) was killed by (.+) using magic"""),
        Regex("""(\w+) withered away"""),
        Regex("""(\w+) withered away whilst fighting (.+)"""),

        // Kinetic energy (elytra crash)
        Regex("""(\w+) experienced kinetic energy"""),
        Regex("""(\w+) experienced kinetic energy whilst trying to escape (.+)"""),

        // Void
        Regex("""(\w+) fell out of the world"""),
        Regex("""(\w+) didn't want to live in the same world as (.+)"""),

        // Thorns
        Regex("""(\w+) was killed trying to hurt (.+)"""),

        // Anvil/Stalactite
        Regex("""(\w+) was squashed by a falling anvil"""),
        Regex("""(\w+) was squashed by a falling anvil whilst fighting (.+)"""),
        Regex("""(\w+) was skewered by a falling stalactite"""),
        Regex("""(\w+) was impaled on a stalagmite"""),

        // Warden
        Regex("""(\w+) was obliterated by a sonically-charged shriek whilst trying to escape (.+)"""),

        // Firework
        Regex("""(\w+) went off with a bang"""),
        Regex("""(\w+) went off with a bang due to a firework fired from (.+) by (.+)""")
    )
    
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
        // FIX: Read more logs to ensure we get enough chat messages
        return getRecentLogs(2000)
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

        // Check for PVP kill first (player killed by another player)
        pvpKillPattern.find(line)?.let { match ->
            val victim = match.groupValues[1]
            val killer = match.groupValues[3]
            // FIX: Check if killer is a player name (not a mob) using knownMobs set
            // Player names: start with letter, no spaces, no mob keywords
            val isMob = knownMobs.any { mobName ->
                killer.equals(mobName, ignoreCase = true) ||
                killer.startsWith(mobName, ignoreCase = true) ||
                killer.contains(mobName, ignoreCase = true)
            }
            if (!isMob && killer.first().isLetter() && !killer.contains(" ")) {
                return LogEntry(
                    timestamp = timestamp,
                    fullDateTime = fullDateTimeStr,
                    date = dateStr,
                    type = LogEntryType.PVP_KILL,
                    playerName = killer,
                    message = "$killer ha matado a $victim",
                    rawLine = line
                )
            }
        }

        // Check for death using comprehensive patterns
        for (pattern in deathPatterns) {
            pattern.find(line.substringAfter("INFO]: ", ""))?.let { match ->
                return LogEntry(
                    timestamp = timestamp,
                    fullDateTime = fullDateTimeStr,
                    date = dateStr,
                    type = LogEntryType.DEATH,
                    playerName = match.groupValues[1],
                    message = translateDeathMessage(line.substringAfter("INFO]: ")),
                    rawLine = line
                )
            }
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

        // Check for server command
        commandPattern.find(line)?.let { match ->
            return LogEntry(
                timestamp = timestamp,
                fullDateTime = fullDateTimeStr,
                date = dateStr,
                type = LogEntryType.COMMAND,
                playerName = match.groupValues[1],
                message = "${match.groupValues[1]} ejecuto /${match.groupValues[2]}",
                rawLine = line
            )
        }

        // Check for kick
        kickPattern.find(line)?.let { match ->
            return LogEntry(
                timestamp = timestamp,
                fullDateTime = fullDateTimeStr,
                date = dateStr,
                type = LogEntryType.KICK,
                playerName = match.groupValues[1],
                message = "${match.groupValues[1]} fue expulsado${if (match.groupValues[2].isNotBlank()) ": ${match.groupValues[2]}" else ""}",
                rawLine = line
            )
        }

        // Check for ban
        banPattern.find(line)?.let { match ->
            return LogEntry(
                timestamp = timestamp,
                fullDateTime = fullDateTimeStr,
                date = dateStr,
                type = LogEntryType.BAN,
                playerName = match.groupValues[1],
                message = "${match.groupValues[1]} fue baneado${if (match.groupValues[2].isNotBlank()) ": ${match.groupValues[2]}" else ""}",
                rawLine = line
            )
        }

        // Check for server start
        if (serverStartPattern.containsMatchIn(line)) {
            return LogEntry(
                timestamp = timestamp,
                fullDateTime = fullDateTimeStr,
                date = dateStr,
                type = LogEntryType.SERVER_START,
                playerName = null,
                message = "Servidor iniciado",
                rawLine = line
            )
        }

        // Check for server stop
        if (serverStopPattern.containsMatchIn(line)) {
            return LogEntry(
                timestamp = timestamp,
                fullDateTime = fullDateTimeStr,
                date = dateStr,
                type = LogEntryType.SERVER_STOP,
                playerName = null,
                message = "Servidor detenido",
                rawLine = line
            )
        }

        // Check for world save
        if (worldSavePattern.containsMatchIn(line)) {
            return LogEntry(
                timestamp = timestamp,
                fullDateTime = fullDateTimeStr,
                date = dateStr,
                type = LogEntryType.WORLD_SAVE,
                playerName = null,
                message = "Mundo guardado",
                rawLine = line
            )
        }

        // Check for warnings
        warningPattern.find(line)?.let { match ->
            return LogEntry(
                timestamp = timestamp,
                fullDateTime = fullDateTimeStr,
                date = dateStr,
                type = LogEntryType.WARNING,
                playerName = null,
                message = match.groupValues[1],
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

    /**
     * Translate death messages to Spanish
     * Source: https://minecraft.wiki/w/Death_messages (Minecraft 1.21.4)
     */
    private fun translateDeathMessage(message: String): String {
        return message
            // Combat - Melee
            .replace("was slain by", "fue asesinado por")
            .replace("was killed by", "fue matado por")
            .replace("got finished off by", "fue rematado por")
            .replace("was pummeled by", "fue golpeado por")
            // Combat - Ranged
            .replace("was shot by", "fue disparado por")
            .replace("was fireballed by", "fue boleado por")
            .replace("was impaled by", "fue empalado por")
            .replace("was skewered by", "fue ensartado por")
            // Fall damage
            .replace("hit the ground too hard", "golpeo el suelo muy fuerte")
            .replace("fell from a high place", "cayo desde un lugar alto")
            .replace("fell off a ladder", "cayo de una escalera")
            .replace("fell off some vines", "cayo de unas enredaderas")
            .replace("fell off scaffolding", "cayo del andamio")
            .replace("fell off some weeping vines", "cayo de enredaderas lloronas")
            .replace("fell off some twisting vines", "cayo de enredaderas retorcidas")
            .replace("fell while climbing", "cayo mientras escalaba")
            .replace("was doomed to fall", "estaba condenado a caer")
            .replace("fell too far and was finished by", "cayo muy lejos y fue rematado por")
            // Drowning
            .replace("drowned whilst trying to escape", "se ahogo intentando escapar de")
            .replace("drowned", "se ahogo")
            // Fire/Lava
            .replace("went up in flames", "se incendio")
            .replace("burned to death", "murio quemado")
            .replace("was burnt to a crisp whilst fighting", "se quemo luchando contra")
            .replace("walked into fire whilst fighting", "entro al fuego luchando contra")
            .replace("tried to swim in lava to escape", "intento nadar en lava para escapar de")
            .replace("tried to swim in lava", "intento nadar en lava")
            // Explosions
            .replace("was blown up by", "fue explotado por")
            .replace("blew up", "exploto")
            .replace("was killed by [Intentional Game Design]", "fue victima del [Diseno Intencional del Juego]")
            // Suffocation/Crushing
            .replace("suffocated in a wall", "se asfixio en una pared")
            .replace("was squished too much", "fue aplastado demasiado")
            .replace("was squashed by", "fue aplastado por")
            // Environment
            .replace("was pricked to death", "murio pinchado")
            .replace("walked into a cactus whilst trying to escape", "camino hacia un cactus escapando de")
            .replace("was struck by lightning", "fue alcanzado por un rayo")
            .replace("discovered the floor was lava", "descubrio que el suelo era lava")
            .replace("froze to death", "murio congelado")
            .replace("was frozen to death by", "murio congelado por")
            .replace("was stung to death", "murio por picaduras")
            .replace("was obliterated by a sonically-charged shriek", "fue destruido por un chillido sonico")
            // Starvation
            .replace("starved to death", "murio de hambre")
            // Magic/Wither
            .replace("was killed by magic", "fue matado por magia")
            .replace("was killed by", "fue matado por")
            .replace("using magic", "usando magia")
            .replace("withered away whilst fighting", "se marchito luchando contra")
            .replace("withered away", "se marchito")
            // Kinetic energy (elytra crash)
            .replace("experienced kinetic energy whilst trying to escape", "experimento energia cinetica escapando de")
            .replace("experienced kinetic energy", "experimento energia cinetica")
            // Void
            .replace("fell out of the world", "cayo al vacio")
            .replace("didn't want to live in the same world as", "no quiso vivir en el mismo mundo que")
            // Thorns
            .replace("was killed trying to hurt", "murio intentando danar a")
            // Anvil/Stalactite
            .replace("was squashed by a falling anvil whilst fighting", "fue aplastado por un yunque mientras luchaba contra")
            .replace("was squashed by a falling anvil", "fue aplastado por un yunque")
            .replace("was skewered by a falling stalactite", "fue ensartado por una estalactita")
            .replace("was impaled on a stalagmite", "fue empalado en una estalagmita")
            // Firework
            .replace("went off with a bang due to a firework fired from", "exploto por un cohete disparado desde")
            .replace("went off with a bang", "exploto con un estallido")
            // Generic
            .replace("died", "murio")
    }
}
