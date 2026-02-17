package org.ReDiego0.turnBasedCombat.manager

import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.game.CombatSession
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.bukkit.Location
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CombatManager(private val plugin: TurnBasedCombat) {
    private val activeSessions = ConcurrentHashMap<UUID, CombatSession>()

    fun startDuel(player1: Duelist, player2: Duelist, location: Location) {
        if (isInCombat(player1.uuid) || isInCombat(player2.uuid)) {
            plugin.logger.warning("Intento de iniciar duelo con jugadores ocupados.")
            return
        }

        val session = CombatSession(plugin, player1, player2, location)

        activeSessions[player1.uuid] = session
        activeSessions[player2.uuid] = session

        session.start()
    }

    fun getSession(playerUuid: UUID): CombatSession? {
        return activeSessions[playerUuid]
    }

    fun isInCombat(playerUuid: UUID): Boolean {
        return activeSessions.containsKey(playerUuid)
    }

    fun removeSession(session: CombatSession) {
        activeSessions.remove(session.player1.uuid)
        activeSessions.remove(session.player2.uuid)
    }
}