package org.ReDiego0.turnBasedCombat.model

import java.util.UUID

data class Companion(
    val id: Int,
    val ownerUuid: UUID,
    val speciesId: String,
    var nickname: String?,
    var level: Int,
    var xp: Double,
    val stats: CombatStats,
    val moves: MutableList<String>,
    var heldItem: String? = null
) {
    fun isFainted(): Boolean = stats.hp <= 0
}
