package com.apptolast.minecraftstats.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "minecraft")
data class MinecraftProperties(
    val statsPath: String = "/data/world/stats",
    val userCachePath: String = "/data/usercache.json",
    val serverName: String = "Minecraft Server"
)
