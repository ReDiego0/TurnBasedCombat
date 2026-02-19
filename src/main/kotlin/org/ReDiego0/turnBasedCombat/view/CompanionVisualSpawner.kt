package org.ReDiego0.turnBasedCombat.view

import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.model.Companion
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

class CompanionVisualSpawner(private val plugin: TurnBasedCombat) {

    fun spawnCompanion(companion: Companion, location: Location, owner: Player): org.bukkit.entity.Entity {
        val species = plugin.speciesManager.getSpecies(companion.speciesId)
        val modelId = species?.modelId ?: "vanilla_wolf"

        return if (modelId.startsWith("vanilla_")) {
            spawnVanillaEntity(modelId.removePrefix("vanilla_"), location)
        } else {
            spawnCustomModel(modelId, location)
        }
    }

    private fun spawnVanillaEntity(typeStr: String, location: Location): org.bukkit.entity.Entity {
        val entityType = runCatching { EntityType.valueOf(typeStr.uppercase()) }
            .getOrDefault(EntityType.WOLF)

        val world = location.world ?: throw IllegalStateException("World cannot be null")

        val entity = world.spawnEntity(location, entityType) as? LivingEntity
            ?: world.spawnEntity(location, EntityType.WOLF) as LivingEntity

        entity.setAI(false)
        entity.isInvulnerable = true
        entity.isSilent = true
        entity.equipment?.clear()
        entity.isCustomNameVisible = false

        return entity
    }

    private fun spawnCustomModel(modelId: String, location: Location): org.bukkit.entity.Entity {
        return spawnVanillaEntity("WOLF", location)
    }
}