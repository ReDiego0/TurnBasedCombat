package org.ReDiego0.turnBasedCombat.model

data class EvolutionRequirement(
    val level: Int,
    val item: String?
)

data class SpeciesTemplate(
    val id: String,
    val displayName: String,
    val elements: List<String>,
    val baseStats: CombatStats,
    val learnset: Map<Int, List<String>>,
    val modelId: String,
    val catchRate: Int = 255,
    val evolutions: Map<String, EvolutionRequirement> = emptyMap() // <-- NUEVO
)