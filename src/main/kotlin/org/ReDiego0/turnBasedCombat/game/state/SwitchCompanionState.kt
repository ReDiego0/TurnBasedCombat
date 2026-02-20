package org.ReDiego0.turnBasedCombat.game.state

import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.game.CombatSession
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.ReDiego0.turnBasedCombat.view.CombatRenderer
import org.bukkit.Bukkit

class SwitchCompanionState(
    private val plugin: TurnBasedCombat,
    private val switchingPlayer: Duelist,
    private val renderer: CombatRenderer
) : CombatState {

    override fun onEnter(session: CombatSession) {
        val nextMob = switchingPlayer.team.firstOrNull { !it.isFainted() }

        if (nextMob == null) {
            val opponent = if (session.player1.uuid == switchingPlayer.uuid) session.player2 else session.player1
            session.endCombat(opponent)
            return
        }

        if (Bukkit.getPlayer(switchingPlayer.uuid) == null) {
            val opponent = if (session.player1.uuid == switchingPlayer.uuid) session.player2 else session.player1
            routeNextTurn(session, opponent)
        } else {
            val opponent = if (session.player1.uuid == switchingPlayer.uuid) session.player2 else session.player1
            routeNextTurn(session, opponent)
        }
    }

    private fun routeNextTurn(session: CombatSession, nextPlayer: Duelist) {
        if (Bukkit.getPlayer(nextPlayer.uuid) == null) {
            session.transitionTo(AITurnState(plugin, nextPlayer, renderer))
        } else {
            session.transitionTo(PlayerTurnState(nextPlayer, renderer))
        }
    }

    override fun onInput(session: CombatSession, player: Duelist, inputId: String) {}
    override fun onTick(session: CombatSession) {}
    override fun onExit(session: CombatSession) {}
}