package org.ReDiego0.turnBasedCombat.model

enum class AIType {
    RANDOM,
    AGGRESSIVE,
    STRATEGIC
}

data class NpcCompanionSpec(
    val speciesId: String,
    val level: Int
)

data class NpcTemplate(
    val id: String,
    val displayName: String,
    val aiType: AIType,
    val teamSpecs: List<NpcCompanionSpec>
)