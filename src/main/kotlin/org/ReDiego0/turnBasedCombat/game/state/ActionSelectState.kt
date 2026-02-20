package org.ReDiego0.turnBasedCombat.game.state

import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.game.ActionType
import org.ReDiego0.turnBasedCombat.game.CombatSession
import org.ReDiego0.turnBasedCombat.game.TurnAction
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.ReDiego0.turnBasedCombat.view.CombatRenderer
import org.ReDiego0.turnBasedCombat.view.CombatInventoryGUIs
import org.bukkit.Bukkit

class ActionSelectionState(
    private val plugin: TurnBasedCombat,
    private val renderer: CombatRenderer
) : CombatState {

    override fun onEnter(session: CombatSession) {
        session.pendingActions.clear()

        if (Bukkit.getPlayer(session.player1.uuid) != null) {
            renderer.showMainMenu(session, session.player1)
        } else {
            queueAiAction(session, session.player1)
        }

        if (Bukkit.getPlayer(session.player2.uuid) != null) {
            renderer.showMainMenu(session, session.player2)
        } else {
            queueAiAction(session, session.player2)
        }

        checkResolution(session)
    }

    override fun onInput(session: CombatSession, player: Duelist, inputId: String) {
        val bukkitPlayer = Bukkit.getPlayer(player.uuid) ?: return

        if (session.pendingActions.any { it.duelistId == player.uuid }) return

        when {
            inputId == "menu_fight" -> renderer.showTechniquesMenu(session, player)
            inputId == "menu_main" -> renderer.showMainMenu(session, player)
            inputId == "menu_bag" -> CombatInventoryGUIs.openCombatBag(bukkitPlayer, player)
            inputId == "menu_team" -> CombatInventoryGUIs.openCombatTeam(bukkitPlayer, player)
            inputId == "menu_flee" -> {
                session.pendingActions.add(TurnAction(player.uuid, ActionType.FLEE, ""))
                renderer.clearMenu(player)
                checkResolution(session)
            }
            inputId.startsWith("tech_") -> {
                val techniqueId = inputId.removePrefix("tech_")
                session.pendingActions.add(TurnAction(player.uuid, ActionType.FIGHT, techniqueId))
                renderer.clearMenu(player)
                checkResolution(session)
            }
        }
    }

    private fun queueAiAction(session: CombatSession, aiDuelist: Duelist) {
        val activeCompanion = aiDuelist.team.firstOrNull { !it.isFainted() } ?: return
        if (activeCompanion.moves.isNotEmpty()) {
            val chosenMove = activeCompanion.moves.random()
            session.pendingActions.add(TurnAction(aiDuelist.uuid, ActionType.FIGHT, chosenMove))
        }
    }

    private fun checkResolution(session: CombatSession) {
        if (session.pendingActions.size >= 2) {
            session.transitionTo(TurnResolutionState(plugin, renderer))
        }
    }

    override fun onTick(session: CombatSession) {}
    override fun onExit(session: CombatSession) {
        renderer.clearMenu(session.player1)
        renderer.clearMenu(session.player2)
    }
}