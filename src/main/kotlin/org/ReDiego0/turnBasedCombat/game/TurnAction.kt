package org.ReDiego0.turnBasedCombat.game

import java.util.UUID

enum class ActionType {
    FIGHT,
    ITEM,
    SWITCH,
    FLEE
}

data class TurnAction(
    val duelistId: UUID,
    val type: ActionType,
    val value: String
)