package org.ReDiego0.turnBasedCombat.model

data class SpeciesTemplate(
    val id: String,
    val displayName: String,
    val elements: List<String>,
    val baseStats: CombatStats,
    val learnset: Map<Int, List<String>>,
    val modelId: String,
    val catchRate: Int = 255
)