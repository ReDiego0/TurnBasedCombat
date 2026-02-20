package org.ReDiego0.turnBasedCombat.model

import java.util.UUID

data class Duelist(
    val uuid: UUID,
    val name: String,
    var currency: Int = 0,
    val team: MutableList<Companion> = mutableListOf(),
    val bag: MutableMap<String, Int> = mutableMapOf(),
    val pcStorage: MutableList<Companion> = mutableListOf()
) {
    fun hasValidTeam(): Boolean {
        return team.any { !it.isFainted() }
    }

    fun addCompanion(companion: Companion) {
        if (team.size < 6) {
            team.add(companion)
        } else {
            pcStorage.add(companion)
        }
    }
}
