package org.ReDiego0.turnBasedCombat.game.state

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.game.ActionType
import org.ReDiego0.turnBasedCombat.game.CombatSession
import org.ReDiego0.turnBasedCombat.game.TurnAction
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.ReDiego0.turnBasedCombat.model.TechniqueCategory
import org.ReDiego0.turnBasedCombat.view.CombatRenderer
import org.bukkit.Bukkit
import kotlin.random.Random

class TurnResolutionState(
    private val plugin: TurnBasedCombat,
    private val renderer: CombatRenderer
) : CombatState {

    override fun onEnter(session: CombatSession) {
        val actions = session.pendingActions.toList()
        session.pendingActions.clear()

        val sortedActions = actions.sortedWith(Comparator { a1, a2 ->
            val priority1 = getActionPriority(a1)
            val priority2 = getActionPriority(a2)

            if (priority1 != priority2) {
                priority2.compareTo(priority1)
            } else {
                val speed1 = getActiveSpeed(session, a1.duelistId)
                val speed2 = getActiveSpeed(session, a2.duelistId)
                speed2.compareTo(speed1)
            }
        })

        executeActionsSequentially(session, sortedActions, 0)
    }

    private fun getActionPriority(action: TurnAction): Int {
        return when (action.type) {
            ActionType.FLEE -> 4
            ActionType.SWITCH -> 3
            ActionType.ITEM -> 2
            ActionType.FIGHT -> 1
        }
    }

    private fun getActiveSpeed(session: CombatSession, duelistId: java.util.UUID): Int {
        val duelist = if (session.player1.uuid == duelistId) session.player1 else session.player2
        return duelist.team.firstOrNull { !it.isFainted() }?.stats?.speed ?: 0
    }

    private fun executeActionsSequentially(session: CombatSession, actions: List<TurnAction>, currentIndex: Int) {
        if (currentIndex >= actions.size) {
            session.transitionTo(ActionSelectionState(plugin, renderer))
            return
        }

        val action = actions[currentIndex]
        val actor = if (session.player1.uuid == action.duelistId) session.player1 else session.player2
        val opponent = if (session.player1.uuid == action.duelistId) session.player2 else session.player1

        val actorCompanion = actor.team.firstOrNull { !it.isFainted() }
        var combatEndedOrSwitched = false

        if (actorCompanion == null && action.type == ActionType.FIGHT) {
            executeActionsSequentially(session, actions, currentIndex + 1)
            return
        }

        when (action.type) {
            ActionType.FIGHT -> {
                val technique = plugin.techniqueManager.getTechnique(action.value)
                val defenderCompanion = opponent.team.firstOrNull { !it.isFainted() }

                if (technique != null && actorCompanion != null && defenderCompanion != null) {

                    val currentPP = actorCompanion.getRemainingPP(technique.id, technique.maxPP)
                    if (currentPP <= 0) {
                        sendMessageToBoth(session, "¡${actorCompanion.nickname} intentó usar ${technique.displayName} pero no le quedan PP!")
                        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                            executeActionsSequentially(session, actions, currentIndex + 1)
                        }, 30L)
                        return
                    }
                    actorCompanion.movePP[technique.id] = currentPP - 1

                    var attackCancelled = false
                    if (actorCompanion.activeStatus?.mechanics?.contains("confused") == true) {
                        sendMessageToBoth(session, "¡${actorCompanion.nickname} está confundido!")
                        if (Random.nextDouble() < 0.5) {
                            val selfDamage = (actorCompanion.stats.maxHp * 0.1).toInt()
                            actorCompanion.stats.hp -= selfDamage
                            sendMessageToBoth(session, "¡Tan confundido que se hirió a sí mismo!")
                            attackCancelled = true

                            if (handleFaint(session, actorCompanion, actor, defenderCompanion, opponent)) {
                                return
                            }
                        }
                    }

                    if (!attackCancelled && !actorCompanion.isFainted()) {
                        val isPhysical = technique.category == TechniqueCategory.PHYSICAL
                        val attackStat = if (isPhysical) actorCompanion.stats.attack else actorCompanion.stats.attack
                        val defenseStat = if (isPhysical) defenderCompanion.stats.defense else defenderCompanion.stats.defense

                        val baseDamage = (((2.0 * actorCompanion.level / 5.0) + 2.0) * technique.power * (attackStat.toDouble() / defenseStat.toDouble())) / 50.0 + 2.0
                        val elementMultiplier = plugin.elementManager.calculateMultiplier(technique.elementId, defenderCompanion.speciesId)
                        val finalDamage = (baseDamage * elementMultiplier * Random.nextDouble(0.85, 1.0)).toInt()

                        defenderCompanion.stats.hp -= finalDamage
                        if (defenderCompanion.stats.hp < 0.0) defenderCompanion.stats.hp = 0.0

                        sendMessageToBoth(session, "¡${actorCompanion.nickname} usó ${technique.displayName}!")

                        if (!defenderCompanion.isFainted() && defenderCompanion.activeStatus == null) {
                            if (technique.applyStatusChance > 0.0 && Random.nextDouble() < technique.applyStatusChance) {
                                val attackElement = plugin.elementManager.getElement(technique.elementId)
                                val statusTemplate = attackElement?.statusEffect

                                if (statusTemplate != null) {
                                    defenderCompanion.activeStatus = org.ReDiego0.turnBasedCombat.model.ActiveStatus(
                                        id = statusTemplate.id,
                                        displayName = statusTemplate.displayName,
                                        mechanics = statusTemplate.mechanics,
                                        power = technique.statusPower,
                                        duration = technique.statusDuration
                                    )
                                    sendMessageToBoth(session, "¡${defenderCompanion.nickname} sufre de ${statusTemplate.displayName}!")
                                }
                            }
                        }

                        if (handleFaint(session, defenderCompanion, opponent, actorCompanion, actor)) {
                            return
                        }
                    }

                    if (!actorCompanion.isFainted() && actorCompanion.activeStatus != null) {
                        val status = actorCompanion.activeStatus!!

                        if (status.mechanics.contains("continuedDamage")) {
                            actorCompanion.stats.hp -= status.power
                            if (actorCompanion.stats.hp < 0.0) actorCompanion.stats.hp = 0.0
                            sendMessageToBoth(session, "¡${actorCompanion.nickname} sufre daño por ${status.displayName}!")

                            if (handleFaint(session, actorCompanion, actor, defenderCompanion, opponent)) {
                                return
                            }
                        }

                        status.duration -= 1
                        if (status.duration <= 0 && !actorCompanion.isFainted()) {
                            sendMessageToBoth(session, "¡${actorCompanion.nickname} se curó de ${status.displayName}!")
                            actorCompanion.activeStatus = null
                        }
                    }
                }
            }
            ActionType.FLEE -> {
                val bukkitPlayer = Bukkit.getPlayer(actor.uuid)

                if (bukkitPlayer != null) {
                    if (!opponent.isWild) {
                        bukkitPlayer.sendMessage(Component.text("¡No puedes huir de un combate contra otro duelista!").color(NamedTextColor.RED))
                    } else {
                        val activeCompanion = actor.team.firstOrNull { !it.isFainted() }
                        val oppCompanion = opponent.team.firstOrNull { !it.isFainted() }

                        if (activeCompanion != null && oppCompanion != null) {
                            val activeSpeed = activeCompanion.stats.speed.toDouble()
                            val oppSpeed = oppCompanion.stats.speed.toDouble()
                            val f = (activeSpeed * 128.0) / oppSpeed

                            if (f >= 255.0 || Random.nextDouble(0.0, 255.0) < f) {
                                bukkitPlayer.sendMessage(Component.text("¡Has escapado con éxito!").color(NamedTextColor.GREEN))
                                session.endCombat(null)
                                combatEndedOrSwitched = true
                            } else {
                                bukkitPlayer.sendMessage(Component.text("¡No pudiste escapar!").color(NamedTextColor.RED))
                            }
                        }
                    }
                }
            }
            else -> {}
        }

        if (!combatEndedOrSwitched) {
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                executeActionsSequentially(session, actions, currentIndex + 1)
            }, 30L)
        }
    }

    private fun handleFaint(
        session: CombatSession,
        faintedCompanion: org.ReDiego0.turnBasedCombat.model.Companion,
        faintedOwner: Duelist,
        victorCompanion: org.ReDiego0.turnBasedCombat.model.Companion,
        victorOwner: Duelist
    ): Boolean {
        if (!faintedCompanion.isFainted()) return false

        val activeBukkitPlayer = Bukkit.getPlayer(victorOwner.uuid)
        plugin.experienceManager.awardXp(victorCompanion, faintedCompanion, activeBukkitPlayer)

        if (faintedOwner.hasValidTeam()) {
            session.transitionTo(SwitchCompanionState(plugin, faintedOwner, renderer))
            return true
        } else {
            sendMessageToBoth(session, "¡${victorOwner.name} ha ganado el combate!")

            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                session.endCombat(victorOwner)
            }, 60L)
            return true
        }
    }

    private fun sendMessageToBoth(session: CombatSession, message: String) {
        val p1 = Bukkit.getPlayer(session.player1.uuid)
        val p2 = Bukkit.getPlayer(session.player2.uuid)
        val comp = Component.text(message).color(NamedTextColor.YELLOW)
        p1?.sendMessage(comp)
        p2?.sendMessage(comp)
    }

    override fun onInput(session: CombatSession, player: Duelist, inputId: String) {}
    override fun onTick(session: CombatSession) {}
    override fun onExit(session: CombatSession) {}
}