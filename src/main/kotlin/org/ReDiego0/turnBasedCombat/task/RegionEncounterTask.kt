package org.ReDiego0.turnBasedCombat.task

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.scheduler.BukkitRunnable
import kotlin.random.Random

class RegionEncounterTask(private val plugin: TurnBasedCombat) : BukkitRunnable() {

    override fun run() {
        val regionContainer = WorldGuard.getInstance().platform.regionContainer
        val query = regionContainer.createQuery()

        for (player in Bukkit.getOnlinePlayers()) {
            if (plugin.combatManager.isInCombat(player.uniqueId)) continue

            val loc = BukkitAdapter.adapt(player.location)
            val applicableRegions = query.getApplicableRegions(loc)

            for (wgRegion in applicableRegions) {
                val tbcRegion = plugin.regionManager.getRegion(wgRegion.id) ?: continue

                spawnAmbientParticles(player.location, tbcRegion.particle)

                if (Random.nextDouble(100.0) < tbcRegion.encounterChance) {
                    plugin.regionManager.createWildEncounter(player, tbcRegion)
                    break
                }
            }
        }
    }

    private fun spawnAmbientParticles(center: Location, particle: org.bukkit.Particle) {
        val world = center.world ?: return
        for (i in 0 until 5) {
            val offsetX = Random.nextDouble(-4.0, 4.0)
            val offsetY = Random.nextDouble(0.0, 2.0)
            val offsetZ = Random.nextDouble(-4.0, 4.0)

            val particleLoc = center.clone().add(offsetX, offsetY, offsetZ)
            world.spawnParticle(particle, particleLoc, 1, 0.0, 0.0, 0.0, 0.0)
        }
    }
}