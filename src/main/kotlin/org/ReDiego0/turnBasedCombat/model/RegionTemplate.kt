package org.ReDiego0.turnBasedCombat.model

import org.bukkit.Particle

data class RegionTemplate(
    val regionId: String,
    val particle: Particle,
    val encounterChance: Double,
    val minLevel: Int,
    val maxLevel: Int,
    val spawns: Map<String, Int>
) {
    val totalWeight: Int = spawns.values.sum()
}