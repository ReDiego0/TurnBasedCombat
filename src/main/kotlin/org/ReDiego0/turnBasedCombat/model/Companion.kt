package org.ReDiego0.turnBasedCombat.model

import java.util.UUID

data class Companion(
    val id: Int,
    var ownerUuid: UUID,
    val speciesId: String,
    var nickname: String?,
    var level: Int,
    var xp: Double,
    val stats: CombatStats,
    val moves: MutableList<String>,
    var movePP: MutableMap<String, Int> = mutableMapOf(),
    var activeStatus: ActiveStatus? = null,
    var heldItem: String? = null
) {
    fun isFainted(): Boolean = stats.hp <= 0

    fun getRemainingPP(moveId: String, maxPP: Int): Int {
        return movePP.getOrDefault(moveId, maxPP)
    }
}