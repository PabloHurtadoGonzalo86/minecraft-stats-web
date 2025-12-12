package com.apptolast.minecraftstats.controller

import com.apptolast.minecraftstats.model.PlayerStats
import com.apptolast.minecraftstats.model.ServerStats
import com.apptolast.minecraftstats.service.StatsService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class ApiController(
    private val statsService: StatsService
) {

    @GetMapping("/stats")
    fun getServerStats(): ResponseEntity<ServerStats> {
        return ResponseEntity.ok(statsService.getServerStats())
    }

    @GetMapping("/stats/player/{uuid}")
    fun getPlayerStats(@PathVariable uuid: String): ResponseEntity<PlayerStats> {
        val stats = statsService.getPlayerStats(uuid)
        return if (stats != null) {
            ResponseEntity.ok(stats)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "UP"))
    }
}
