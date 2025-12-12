package com.apptolast.minecraftstats.controller

import com.apptolast.minecraftstats.config.MinecraftProperties
import com.apptolast.minecraftstats.service.StatsService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@Controller
class WebController(
    private val statsService: StatsService,
    private val properties: MinecraftProperties
) {

    @GetMapping("/")
    fun index(model: Model): String {
        val serverStats = statsService.getServerStats()
        model.addAttribute("serverName", properties.serverName)
        model.addAttribute("stats", serverStats)
        return "index"
    }

    @GetMapping("/player/{uuid}")
    fun playerDetail(@PathVariable uuid: String, model: Model): String {
        val playerStats = statsService.getPlayerStats(uuid)
        model.addAttribute("serverName", properties.serverName)
        model.addAttribute("player", playerStats)
        return "player"
    }
}
