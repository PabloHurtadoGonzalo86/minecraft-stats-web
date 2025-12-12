package com.apptolast.minecraftstats.service

import com.apptolast.minecraftstats.config.MinecraftProperties
import com.apptolast.minecraftstats.model.Advancement
import com.apptolast.minecraftstats.model.PlayerAdvancements
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.io.File

/**
 * Service for reading player advancements (achievements)
 */
@Service
class AdvancementService(
    private val objectMapper: ObjectMapper,
    private val properties: MinecraftProperties
) {
    private val logger = LoggerFactory.getLogger(AdvancementService::class.java)
    
    // Map of advancement IDs to friendly names
    private val advancementNames = mapOf(
        // Story
        "minecraft:story/root" to "Minecraft",
        "minecraft:story/mine_stone" to "La edad de piedra",
        "minecraft:story/upgrade_tools" to "Mejorando",
        "minecraft:story/smelt_iron" to "¡Consigue hardware!",
        "minecraft:story/obtain_armor" to "Cubrirse de hierro",
        "minecraft:story/lava_bucket" to "Traficante de cubos calientes",
        "minecraft:story/iron_tools" to "¿No es hierro?",
        "minecraft:story/deflect_arrow" to "No hoy, gracias",
        "minecraft:story/form_obsidian" to "Ojo de hielo",
        "minecraft:story/mine_diamond" to "¡Diamantes!",
        "minecraft:story/enter_the_nether" to "Nos vamos al Infierno",
        "minecraft:story/shiny_gear" to "Cúbreme de brillos",
        "minecraft:story/enchant_item" to "Encantador",
        "minecraft:story/cure_zombie_villager" to "Doctor zombi",
        "minecraft:story/follow_ender_eye" to "Ojo avizor",
        "minecraft:story/enter_the_end" to "El Fin",
        
        // Nether
        "minecraft:nether/root" to "Nether",
        "minecraft:nether/return_to_sender" to "Devolver al remitente",
        "minecraft:nether/find_bastion" to "Aquellos buenos viejos tiempos",
        "minecraft:nether/obtain_ancient_debris" to "Escombros ocultos",
        "minecraft:nether/fast_travel" to "Submapa",
        "minecraft:nether/find_fortress" to "Una terrible fortaleza",
        "minecraft:nether/obtain_crying_obsidian" to "¿Quién está cortando cebollas?",
        "minecraft:nether/distract_piglin" to "¡Oooh, brillante!",
        "minecraft:nether/ride_strider" to "Este barco tiene patas",
        "minecraft:nether/uneasy_alliance" to "Alianza incómoda",
        "minecraft:nether/loot_bastion" to "Restos de guerra",
        "minecraft:nether/use_lodestone" to "País de las maravillas",
        "minecraft:nether/netherite_armor" to "Cúbrete de restos",
        "minecraft:nether/get_wither_skull" to "Vibraciones escalofriantes",
        "minecraft:nether/obtain_blaze_rod" to "En llamas",
        "minecraft:nether/charge_respawn_anchor" to "No es un sueño",
        "minecraft:nether/explore_nether" to "Verano caliente",
        "minecraft:nether/summon_wither" to "Marchitando alturas",
        "minecraft:nether/brew_potion" to "Boticario local",
        "minecraft:nether/create_beacon" to "Lleva una señal luminosa",
        "minecraft:nether/all_potions" to "Un viaje furioso",
        "minecraft:nether/create_full_beacon" to "Señalizadores",
        "minecraft:nether/all_effects" to "¿Cómo hemos llegado aquí?",
        
        // End
        "minecraft:end/root" to "El Fin",
        "minecraft:end/kill_dragon" to "Libera el Fin",
        "minecraft:end/dragon_egg" to "La próxima generación",
        "minecraft:end/enter_end_gateway" to "El Fin remoto",
        "minecraft:end/respawn_dragon" to "El Fin... Otra vez...",
        "minecraft:end/dragon_breath" to "Te hace falta unas mentitas",
        "minecraft:end/find_end_city" to "La ciudad del fin del camino",
        "minecraft:end/elytra" to "El cielo es el límite",
        "minecraft:end/levitate" to "¡Ahí arriba!",
        
        // Adventure
        "minecraft:adventure/root" to "Aventura",
        "minecraft:adventure/voluntary_exile" to "Exilio voluntario",
        "minecraft:adventure/kill_a_mob" to "Cazador de monstruos",
        "minecraft:adventure/trade" to "El arte del negocio",
        "minecraft:adventure/honey_block_slide" to "Desliz pegajoso",
        "minecraft:adventure/ol_betsy" to "La vieja Betsy",
        "minecraft:adventure/sleep_in_bed" to "Dulces sueños",
        "minecraft:adventure/hero_of_the_village" to "Héroe del pueblo",
        "minecraft:adventure/throw_trident" to "Un tiro de larga distancia",
        "minecraft:adventure/shoot_arrow" to "Apúntale a eso",
        "minecraft:adventure/kill_all_mobs" to "Monstruos cazados",
        "minecraft:adventure/totem_of_undying" to "Vida postmortem",
        "minecraft:adventure/summon_iron_golem" to "Poder contratado",
        "minecraft:adventure/two_birds_one_arrow" to "Dos pájaros de un tiro",
        "minecraft:adventure/whos_the_pillager_now" to "¿Quién es el saqueador ahora?",
        "minecraft:adventure/arbalistic" to "Arbalístico",
        "minecraft:adventure/adventuring_time" to "Hora de aventuras",
        "minecraft:adventure/very_very_frightening" to "Muy muy aterrador",
        "minecraft:adventure/sniper_duel" to "Duelo de francotiradores",
        "minecraft:adventure/bullseye" to "Diana",
        
        // Husbandry
        "minecraft:husbandry/root" to "Cría",
        "minecraft:husbandry/safely_harvest_honey" to "Cosecha segura de miel",
        "minecraft:husbandry/breed_an_animal" to "El encanto de la granja",
        "minecraft:husbandry/tame_an_animal" to "Los mejores amigos",
        "minecraft:husbandry/fishy_business" to "Negocio de pesca",
        "minecraft:husbandry/silk_touch_nest" to "Rescate total de abejas",
        "minecraft:husbandry/plant_seed" to "Un siembra-mundos",
        "minecraft:husbandry/bred_all_animals" to "Dos a dos",
        "minecraft:husbandry/complete_catalogue" to "Un catálogo completo",
        "minecraft:husbandry/tactical_fishing" to "Pesca táctica",
        "minecraft:husbandry/balanced_diet" to "Dieta equilibrada",
        "minecraft:husbandry/obtain_netherite_hoe" to "Dedicación seria",
        "minecraft:husbandry/wax_on" to "Dar cera",
        "minecraft:husbandry/wax_off" to "Quitar cera",
        "minecraft:husbandry/axolotl_in_a_bucket" to "El animal más bonito",
        "minecraft:husbandry/kill_axolotl_target" to "La amistad da asco",
        "minecraft:husbandry/ride_a_boat_with_a_goat" to "Sea lo que sea que flote en tu cabra"
    )
    
    @Cacheable("advancements")
    fun getPlayerAdvancements(uuid: String): PlayerAdvancements? {
        val advancementsPath = properties.statsPath.replace("/stats", "/advancements")
        val advancementFile = File(advancementsPath, "$uuid.json")
        
        if (!advancementFile.exists()) {
            logger.warn("Advancements file not found for UUID: $uuid")
            return null
        }
        
        return try {
            val rawData: Map<String, Any> = objectMapper.readValue(advancementFile)
            
            val advancements = rawData
                .filterKeys { it.startsWith("minecraft:") && !it.contains("recipes/") }
                .mapNotNull { (key, value) ->
                    @Suppress("UNCHECKED_CAST")
                    val data = value as? Map<String, Any> ?: return@mapNotNull null
                    val done = data["done"] as? Boolean ?: false
                    val criteria = data["criteria"] as? Map<String, String> ?: emptyMap()
                    
                    Advancement(
                        id = key,
                        name = advancementNames[key] ?: key.substringAfterLast("/").replace("_", " "),
                        done = done,
                        criteriaCount = criteria.size,
                        completedAt = if (done) criteria.values.firstOrNull() else null
                    )
                }
                .sortedByDescending { it.done }
            
            val completed = advancements.count { it.done }
            val total = advancements.size
            
            PlayerAdvancements(
                uuid = uuid,
                advancements = advancements,
                completedCount = completed,
                totalCount = total,
                completionPercentage = if (total > 0) (completed * 100 / total) else 0
            )
        } catch (e: Exception) {
            logger.error("Error reading advancements for UUID $uuid: ${e.message}")
            null
        }
    }
}
