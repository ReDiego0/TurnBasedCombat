package org.ReDiego0.turnBasedCombat.model

enum class TechniqueCategory {
    PHYSICAL,
    MAGICAL,
    STATUS
}

data class Technique(
    val id: String,
    val displayName: String,
    val elementId: String,
    val category: TechniqueCategory,
    val power: Int,
    val accuracy: Int,
    val applyStatusChance: Double
)