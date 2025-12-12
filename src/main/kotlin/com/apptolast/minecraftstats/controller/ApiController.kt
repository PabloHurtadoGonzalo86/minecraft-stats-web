package com.apptolast.minecraftstats.controller

import com.apptolast.minecraftstats.model.*
import com.apptolast.minecraftstats.service.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class ApiController(
    private val statsService: StatsService,
    private val serverStatusService: ServerStatusService,
    private val logService: LogService,
    private val advancementService: AdvancementService
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
    
    @GetMapping("/status")
    fun getServerStatus(): ResponseEntity<ServerStatus> {
        return ResponseEntity.ok(serverStatusService.getServerStatus())
    }
    
    @GetMapping("/events")
    fun getRecentEvents(@RequestParam(defaultValue = "50") limit: Int): ResponseEntity<List<LogEntry>> {
        return ResponseEntity.ok(logService.getRecentEvents(limit))
    }
    
    @GetMapping("/chat")
    fun getRecentChat(@RequestParam(defaultValue = "30") limit: Int): ResponseEntity<List<LogEntry>> {
        return ResponseEntity.ok(logService.getRecentChat(limit))
    }
    
    /**
     * Get historical events from the last N days (default 30 days = 1 month)
     */
    @GetMapping("/events/history")
    fun getHistoricalEvents(
        @RequestParam(defaultValue = "30") days: Int,
        @RequestParam(defaultValue = "500") limit: Int
    ): ResponseEntity<List<LogEntry>> {
        return ResponseEntity.ok(logService.getHistoricalEvents(days.coerceIn(1, 90), limit.coerceIn(1, 2000)))
    }
    
    /**
     * Get historical chat from the last N days (default 30 days = 1 month)
     */
    @GetMapping("/chat/history")
    fun getHistoricalChat(
        @RequestParam(defaultValue = "30") days: Int,
        @RequestParam(defaultValue = "500") limit: Int
    ): ResponseEntity<List<LogEntry>> {
        return ResponseEntity.ok(logService.getHistoricalChat(days.coerceIn(1, 90), limit.coerceIn(1, 2000)))
    }
    
    @GetMapping("/advancements/{uuid}")
    fun getPlayerAdvancements(@PathVariable uuid: String): ResponseEntity<PlayerAdvancements> {
        val advancements = advancementService.getPlayerAdvancements(uuid)
        return if (advancements != null) {
            ResponseEntity.ok(advancements)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "UP"))
    }
}
