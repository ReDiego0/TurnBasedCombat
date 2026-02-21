package org.ReDiego0.turnBasedCombat.model

data class StatusEffectTemplate(
    val id: String,
    val displayName: String,
    val mechanics: List<String>
)

data class ActiveStatus(
    val id: String,
    val displayName: String,
    val mechanics: List<String>,
    val power: Int,
    var duration: Int
)