package com.apptolast.minecraftstats.service

import com.apptolast.minecraftstats.config.MinecraftProperties
import com.apptolast.minecraftstats.model.OnlinePlayer
import com.apptolast.minecraftstats.model.ServerStatus
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.util.Properties

/**
 * Service for monitoring server status in real-time
 */
@Service
class ServerStatusService(
    private val properties: MinecraftProperties,
    private val rconService: RconService,
    private val logService: LogService
) {
    private val logger = LoggerFactory.getLogger(ServerStatusService::class.java)
    
    @Volatile
    private var cachedStatus: ServerStatus? = null
    
    fun getServerStatus(): ServerStatus {
        return cachedStatus ?: refreshStatus()
    }
    
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    fun refreshStatus(): ServerStatus {
        val rconHost = getRconHost()
        val rconPort = getRconPort()
        val rconPassword = getRconPassword()
        
        val onlinePlayers = if (rconPassword != null) {
            getOnlinePlayersViaRcon(rconHost, rconPort, rconPassword)
        } else {
            getOnlinePlayersFromLogs()
        }
        
        val serverProperties = loadServerProperties()
        
        val status = ServerStatus(
            online = true,
            playerCount = onlinePlayers.size,
            maxPlayers = serverProperties["max-players"]?.toString()?.toIntOrNull() ?: 20,
            onlinePlayers = onlinePlayers,
            motd = serverProperties["motd"]?.toString() ?: "",
            version = getServerVersion(),
            lastUpdated = System.currentTimeMillis()
        )
        
        cachedStatus = status
        return status
    }
    
    private fun getOnlinePlayersViaRcon(host: String, port: Int, password: String): List<OnlinePlayer> {
        val response = rconService.executeCommand(host, port, password, "list")
        
        if (response == null) {
            logger.warn("Could not get online players via RCON")
            return getOnlinePlayersFromLogs()
        }
        
        // Parse response like: "There are 2 of a max of 10 players online: Player1, Player2"
        val playersMatch = Regex("""online: (.+)$""").find(response)
        if (playersMatch != null) {
            val playerNames = playersMatch.groupValues[1]
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            
            return playerNames.map { name ->
                OnlinePlayer(
                    name = name,
                    uuid = null, // We don't have UUID from RCON
                    joinedAt = null
                )
            }
        }
        
        // No players online
        return emptyList()
    }
    
    private fun getOnlinePlayersFromLogs(): List<OnlinePlayer> {
        val recentEvents = logService.getRecentLogs(200)
        val onlinePlayers = mutableMapOf<String, OnlinePlayer>()
        
        recentEvents.forEach { event ->
            when (event.type) {
                com.apptolast.minecraftstats.model.LogEntryType.JOIN -> {
                    event.playerName?.let { name ->
                        onlinePlayers[name] = OnlinePlayer(
                            name = name,
                            uuid = null,
                            joinedAt = event.timestamp
                        )
                    }
                }
                com.apptolast.minecraftstats.model.LogEntryType.LEAVE -> {
                    event.playerName?.let { onlinePlayers.remove(it) }
                }
                else -> {}
            }
        }
        
        return onlinePlayers.values.toList()
    }
    
    private fun loadServerProperties(): Properties {
        val props = Properties()
        val propsPath = properties.statsPath.replace("/world/stats", "/server.properties")
        val propsFile = File(propsPath)
        
        if (propsFile.exists()) {
            try {
                propsFile.inputStream().use { props.load(it) }
            } catch (e: Exception) {
                logger.error("Error loading server.properties: ${e.message}")
            }
        }
        
        return props
    }
    
    private fun getServerVersion(): String {
        // Try to get version from fabric manifest
        val manifestPath = properties.statsPath.replace("/world/stats", "/.fabric-manifest.json")
        val manifestFile = File(manifestPath)
        
        if (manifestFile.exists()) {
            try {
                val content = manifestFile.readText()
                val versionMatch = Regex(""""version"\s*:\s*"([^"]+)"""").find(content)
                if (versionMatch != null) {
                    return "Fabric ${versionMatch.groupValues[1]}"
                }
            } catch (e: Exception) {
                logger.warn("Could not read fabric manifest: ${e.message}")
            }
        }
        
        return "Unknown"
    }
    
    private fun getRconHost(): String = "minecraft.minecraft.svc.cluster.local"
    
    private fun getRconPort(): Int = 25575
    
    private fun getRconPassword(): String? {
        val propsPath = properties.statsPath.replace("/world/stats", "/.rcon-cli.env")
        val propsFile = File(propsPath)
        
        if (propsFile.exists()) {
            try {
                val content = propsFile.readText()
                val passwordMatch = Regex("""RCON_PASSWORD=(.+)""").find(content)
                return passwordMatch?.groupValues?.get(1)
            } catch (e: Exception) {
                logger.warn("Could not read RCON password: ${e.message}")
            }
        }
        
        return null
    }
}
