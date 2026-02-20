package org.ReDiego0.turnBasedCombat.game.state

import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.game.CombatSession
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.ReDiego0.turnBasedCombat.model.TechniqueCategory
import org.ReDiego0.turnBasedCombat.view.CombatRenderer
import org.bukkit.Bukkit
import kotlin.random.Random

class ExecuteTechniqueState(
    private val plugin: TurnBasedCombat,
    private val activePlayer: Duelist,
    private val techniqueId: String,
    private val renderer: CombatRenderer
) : CombatState {

    override fun onEnter(session: CombatSession) {
        val opponent = if (session.player1.uuid == activePlayer.uuid) session.player2 else session.player1
        val attackerMob = activePlayer.team.firstOrNull { !it.isFainted() }
        val defenderMob = opponent.team.firstOrNull { !it.isFainted() }

        if (attackerMob == null || defenderMob == null) {
            session.endCombat(if (attackerMob == null) opponent else activePlayer)
            return
        }

        val technique = plugin.techniqueManager.getTechnique(techniqueId)
        if (technique == null) {
            routeNextTurn(session, activePlayer)
            return
        }

        val isPhysical = technique.category == TechniqueCategory.PHYSICAL
        val attackStat = if (isPhysical) attackerMob.stats.attack else attackerMob.stats.attack
        val defenseStat = if (isPhysical) defenderMob.stats.defense else defenderMob.stats.defense

        val baseDamage = (((2.0 * attackerMob.level / 5.0) + 2.0) * technique.power * (attackStat.toDouble() / defenseStat.toDouble())) / 50.0 + 2.0

        val elementMultiplier = plugin.elementManager.calculateMultiplier(technique.elementId, defenderMob.speciesId)
        val randomFactor = Random.nextDouble(0.85, 1.0)

        val finalDamage = (baseDamage * elementMultiplier * randomFactor).toInt()

        defenderMob.stats.hp -= finalDamage
        if (defenderMob.stats.hp < 0.0) defenderMob.stats.hp = 0.0

        if (defenderMob.isFainted()) {

            val activeBukkitPlayer = Bukkit.getPlayer(activePlayer.uuid)
            plugin.experienceManager.awardXp(attackerMob, defenderMob, activeBukkitPlayer)

            if (opponent.hasValidTeam()) {
                session.transitionTo(SwitchCompanionState(plugin, opponent, renderer))
            } else {
                session.endCombat(activePlayer)
            }
        } else {
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