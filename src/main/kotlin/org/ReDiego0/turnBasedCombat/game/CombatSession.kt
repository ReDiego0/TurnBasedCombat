package org.ReDiego0.turnBasedCombat.game

import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.game.state.CombatState
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.bukkit.Location
import org.bukkit.scheduler.BukkitTask

class CombatSession(
    private val plugin: TurnBasedCombat,
    val player1: Duelist,
    val player2: Duelist,
    val centerLocation: Location
) {

    var currentState: CombatState? = null
        private set

    private var tickTask: BukkitTask? = null
    var turnCounter: Int = 0

    val activeEntities = mutableListOf<org.bukkit.entity.Entity>()

    fun start() {
        tickTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            currentState?.onTick(this)
        }, 0L, 1L)

        val renderer = org.ReDiego0.turnBasedCombat.view.VanillaCombatRenderer(plugin)
        transitionTo(org.ReDiego0.turnBasedCombat.game.state.InitializationState(plugin, renderer))
    }

    fun transitionTo(newState: CombatState) {
        currentState?.onExit(this)
        currentState = newState
        plugin.logger.info("Combate P1 vs P2 -> Estado: ${newState::class.simpleName}")
        currentState?.onEnter(this)
    }

    fun endCombat(winner: Duelist?) {
        tickTask?.cancel()
        currentState?.onExit(this)

        activeEntities.forEach { it.remove() }
        activeEntities.clear()

        plugin.logger.info("Combate finalizado. Ganador: ${winner?.name ?: "Nadie"}")
        // llamar al CombatManager para destruir esta sesión
    }
}