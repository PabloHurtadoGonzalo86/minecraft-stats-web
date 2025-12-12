package com.apptolast.minecraftstats

import com.apptolast.minecraftstats.config.MinecraftProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableConfigurationProperties(MinecraftProperties::class)
@EnableScheduling
class MinecraftStatsWebApplication

fun main(args: Array<String>) {
	runApplication<MinecraftStatsWebApplication>(*args)
}
