package com.apptolast.minecraftstats

import com.apptolast.minecraftstats.config.MinecraftProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(MinecraftProperties::class)
class MinecraftStatsWebApplication

fun main(args: Array<String>) {
	runApplication<MinecraftStatsWebApplication>(*args)
}
