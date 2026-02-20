package org.ReDiego0.turnBasedCombat.game.state

import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.game.CombatSession
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.ReDiego0.turnBasedCombat.view.CombatRenderer
import org.ReDiego0.turnBasedCombat.view.CompanionVisualSpawner
import org.bukkit.Bukkit

class SwitchCompanionState(
    private val plugin: TurnBasedCombat,
    private val switchingPlayer: Duelist,
    private val renderer: CombatRenderer
) : CombatState {

    override fun onEnter(session: CombatSession) {
        val nextMob = switchingPlayer.team.firstOrNull { !it.isFainted() }

        val opponent = if (session.player1.uuid == switchingPlayer.uuid) session.player2 else session.player1

        if (nextMob == null) {
            session.endCombat(opponent)
            return
        }

        val isPlayer1 = session.player1.uuid == switchingPlayer.uuid
        val playerLoc = if (isPlayer1) session.arenaCenter.clone().add(3.0, 0.0, 0.0).apply { yaw = 90f } else session.arenaCenter.clone().add(-3.0, 0.0, 0.0).apply { yaw = -90f }
        val player = Bukkit.getPlayer(switchingPlayer.uuid)

        val oldEntity = if (isPlayer1) session.p1Entity else session.p2Entity
        oldEntity?.remove()
        session.activeEntities.remove(oldEntity)

        val visualSpawner = CompanionVisualSpawner(plugin)
        val newEntity = visualSpawner.spawnCompanion(nextMob, playerLoc, player)

        if (isPlayer1) session.p1Entity = newEntity else session.p2Entity = newEntity
        session.activeEntities.add(newEntity)

        val p1Player = Bukkit.getPlayer(session.player1.uuid)
        val p2Player = Bukkit.getPlayer(session.player2.uuid)

        for (onlinePlayer in Bukkit.getOnlinePlayers()) {
            if (onlinePlayer != p1Player && onlinePlayer != p2Player) {
                onlinePlayer.hideEntity(plugin, newEntity)
            }
        }

        routeNextTurn(session, opponent)
    }

    private fun routeNextTurn(session: CombatSession, nextPlayer: Duelist) {
        session.transitionTo(ActionSelectionState(plugin, renderer))
    }

    override fun onInput(session: CombatSession, player: Duelist, inputId: String) {}
    override fun onTick(session: CombatSession) {}
    override fun onExit(session: CombatSession) {}
}