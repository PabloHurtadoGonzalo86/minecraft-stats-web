package com.apptolast.minecraftstats.controller

import com.apptolast.minecraftstats.config.MinecraftProperties
import com.apptolast.minecraftstats.service.AdvancementService
import com.apptolast.minecraftstats.service.ServerStatusService
import com.apptolast.minecraftstats.service.StatsService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@Controller
class WebController(
    private val statsService: StatsService,
    private val advancementService: AdvancementService,
    private val serverStatusService: ServerStatusService,
    private val properties: MinecraftProperties
) {

    @GetMapping("/")
    fun index(model: Model): String {
        val serverStats = statsService.getServerStats()
        val serverStatus = serverStatusService.getServerStatus()
        model.addAttribute("serverName", properties.serverName)
        model.addAttribute("stats", serverStats)
        model.addAttribute("status", serverStatus)
        return "index"
    }

    @GetMapping("/player/{uuid}")
    fun playerDetail(@PathVariable uuid: String, model: Model): String {
        val playerStats = statsService.getPlayerStats(uuid)
        val advancements = advancementService.getPlayerAdvancements(uuid)
        model.addAttribute("serverName", properties.serverName)
        model.addAttribute("player", playerStats)
        model.addAttribute("advancements", advancements)
        return "player"
    }
}
