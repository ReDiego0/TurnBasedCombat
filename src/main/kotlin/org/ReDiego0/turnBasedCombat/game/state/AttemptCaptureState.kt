package org.ReDiego0.turnBasedCombat.game.state

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.game.CombatSession
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.ReDiego0.turnBasedCombat.view.CombatRenderer
import org.bukkit.Bukkit
import kotlin.random.Random

class AttemptCaptureState(
    private val plugin: TurnBasedCombat,
    private val activePlayer: Duelist,
    private val targetDuelist: Duelist,
    private val itemId: String,
    private val renderer: CombatRenderer
) : CombatState {

    override fun onEnter(session: CombatSession) {
        val player = Bukkit.getPlayer(activePlayer.uuid)
        val captureItem = plugin.itemManager.getItem(itemId)
        val wildCompanion = targetDuelist.team.firstOrNull { !it.isFainted() }

        if (player == null || captureItem == null || wildCompanion == null) {
            session.transitionTo(ActionSelectionState(plugin, renderer))
            return
        }

        if (!targetDuelist.isWild) {
            player.sendMessage(Component.text("¡No puedes robar el Companion de otro duelista!").color(NamedTextColor.RED))
            session.transitionTo(ActionSelectionState(plugin, renderer))
            return
        }

        val count = activePlayer.bag[itemId] ?: 0
        if (count > 1) activePlayer.bag[itemId] = count - 1 else activePlayer.bag.remove(itemId)

        player.sendMessage(Component.text("¡Has lanzado un ${captureItem.displayName}!").color(NamedTextColor.YELLOW))

        val species = plugin.speciesManager.getSpecies(wildCompanion.speciesId)
        val catchRate = species?.catchRate ?: 255

        val maxHp = wildCompanion.stats.maxHp
        val currentHp = wildCompanion.stats.hp
        val itemModifier = captureItem.power
        val statusModifier = 1.0

        val a = (((3.0 * maxHp - 2.0 * currentHp) * catchRate * itemModifier) / (3.0 * maxHp)) * statusModifier

        val captured = if (a >= 255.0) {
            true
        } else {
            val probability = a / 255.0
            Random.nextDouble() < probability
        }

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (captured) {
                player.sendMessage(Component.text("¡Has capturado a ${wildCompanion.nickname}!").color(NamedTextColor.GREEN))

                wildCompanion.ownerUuid = activePlayer.uuid

                if (activePlayer.team.size < 6) {
                    activePlayer.team.add(wildCompanion)
                    player.sendMessage(Component.text("Se ha añadido a tu equipo.").color(NamedTextColor.AQUA))
                } else {
                    activePlayer.pcStorage.add(wildCompanion)
                    player.sendMessage(Component.text("Tu equipo está lleno. Se ha enviado al PC.").color(NamedTextColor.GOLD))
                }

                session.endCombat(activePlayer)
            } else {
                player.sendMessage(Component.text("¡Oh no! ¡El Companion salvaje se ha liberado!").color(NamedTextColor.RED))
                session.transitionTo(ActionSelectionState(plugin, renderer))
            }
        }, 40L)
    }

    override fun onInput(session: CombatSession, player: Duelist, inputId: String) {}
    override fun onTick(session: CombatSession) {}
    override fun onExit(session: CombatSession) {}
}