package org.ReDiego0.turnBasedCombat.game.state

import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.game.CombatSession
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.ReDiego0.turnBasedCombat.view.CombatRenderer
import org.ReDiego0.turnBasedCombat.view.CompanionVisualSpawner
import org.bukkit.Bukkit

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

        val p1Player = Bukkit.getPlayer(session.player1.uuid)
        val p2Player = Bukkit.getPlayer(session.player2.uuid)

        if (p1Player != null && p2Player != null) {
            val visualSpawner = CompanionVisualSpawner(plugin)

            val p1MobLoc = p1Player.location.clone().add(p1Player.location.direction.multiply(2.0))
            val p2MobLoc = p2Player.location.clone().add(p2Player.location.direction.multiply(2.0))

            val p1Entity = visualSpawner.spawnCompanion(p1Companion, p1MobLoc, p1Player)
            val p2Entity = visualSpawner.spawnCompanion(p2Companion, p2MobLoc, p2Player)

            session.activeEntities.add(p1Entity)
            session.activeEntities.add(p2Entity)

            isolateEntity(p1Entity, p1Player, p2Player)
            isolateEntity(p2Entity, p1Player, p2Player)
        }

        session.transitionTo(ActionSelectionState(plugin, renderer))
    }

    private fun isolateEntity(entity: org.bukkit.entity.Entity, p1: org.bukkit.entity.Player, p2: org.bukkit.entity.Player) {
        for (onlinePlayer in Bukkit.getOnlinePlayers()) {
            if (onlinePlayer != p1 && onlinePlayer != p2) {
                onlinePlayer.hideEntity(plugin, entity)
            }
        }
    }

    override fun onInput(session: CombatSession, player: Duelist, inputId: String) {}
    override fun onTick(session: CombatSession) {}
    override fun onExit(session: CombatSession) {}
}