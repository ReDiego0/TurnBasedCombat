package org.ReDiego0.turnBasedCombat.model

data class Element(
    val id: String,
    val displayName: String,
    val weaknesses: Set<String>,
    val resistances: Set<String>,
    val immunities: Set<String>,
    val statusEffect: StatusEffectTemplate? = null
)