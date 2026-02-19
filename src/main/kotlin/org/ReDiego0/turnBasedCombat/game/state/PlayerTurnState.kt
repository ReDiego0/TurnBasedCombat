package org.ReDiego0.turnBasedCombat.game.state

import org.ReDiego0.turnBasedCombat.game.CombatSession
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.ReDiego0.turnBasedCombat.view.CombatRenderer

class PlayerTurnState(
    private val activePlayer: Duelist,
    private val renderer: CombatRenderer
) : CombatState {

    override fun onEnter(session: CombatSession) {
        renderer.showMainMenu(session, activePlayer)
    }

    override fun onInput(session: CombatSession, player: Duelist, inputId: String) {
        if (player.uuid != activePlayer.uuid) return

        when {
            inputId == "menu_fight" -> {
                renderer.showTechniquesMenu(session, activePlayer)
            }
            inputId == "menu_main" -> {
                renderer.showMainMenu(session, activePlayer)
            }
            inputId == "menu_flee" -> {
                val opponent = getOpponent(session, activePlayer)
                session.endCombat(opponent)
            }
            inputId.startsWith("tech_") -> {
                val techniqueId = inputId.removePrefix("tech_")
                session.transitionTo(ExecuteTechniqueState(activePlayer, techniqueId, renderer))
            }
            inputId == "menu_bag" || inputId == "menu_team" -> {
            }
        }
    }

    override fun onTick(session: CombatSession) {
    }

    override fun onExit(session: CombatSession) {
        renderer.clearMenu(activePlayer)
    }

    private fun getOpponent(session: CombatSession, player: Duelist): Duelist {
        return if (session.player1.uuid == player.uuid) session.player2 else session.player1
    }
}