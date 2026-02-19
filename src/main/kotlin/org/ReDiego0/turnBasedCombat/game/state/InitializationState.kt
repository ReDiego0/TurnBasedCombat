package org.ReDiego0.turnBasedCombat.game.state

import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.game.CombatSession
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.ReDiego0.turnBasedCombat.view.CombatRenderer

class InitializationState(
    private val plugin: TurnBasedCombat,
    private val renderer: CombatRenderer
) : CombatState {

    override fun onEnter(session: CombatSession) {
        val p1Companion = session.player1.team.firstOrNull { !it.isFainted() }
        val p2Companion = session.player2.team.firstOrNull { !it.isFainted() }

        if (p1Companion == null || p2Companion == null) {
            val winner = if (p1Companion != null) session.player1 else if (p2Companion != null) session.player2 else null
            session.endCombat(winner)
            return
        }

        val p1Speed = p1Companion.stats.speed
        val p2Speed = p2Companion.stats.speed

        val firstPlayer = if (p1Speed >= p2Speed) session.player1 else session.player2

        session.transitionTo(PlayerTurnState(firstPlayer, renderer))
    }

    override fun onInput(session: CombatSession, player: Duelist, inputId: String) {}

    override fun onTick(session: CombatSession) {}

    override fun onExit(session: CombatSession) {}
}