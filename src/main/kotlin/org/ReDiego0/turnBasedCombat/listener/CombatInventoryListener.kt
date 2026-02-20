package org.ReDiego0.turnBasedCombat.listener

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.game.state.AITurnState
import org.ReDiego0.turnBasedCombat.game.state.AttemptCaptureState
import org.ReDiego0.turnBasedCombat.game.state.PlayerTurnState
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.ReDiego0.turnBasedCombat.model.ItemType
import org.ReDiego0.turnBasedCombat.view.CombatBagMainHolder
import org.ReDiego0.turnBasedCombat.view.CombatBagTargetHolder
import org.ReDiego0.turnBasedCombat.view.CombatInventoryGUIs
import org.ReDiego0.turnBasedCombat.view.CombatTeamHolder
import org.ReDiego0.turnBasedCombat.view.VanillaCombatRenderer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class CombatInventoryListener(private val plugin: TurnBasedCombat) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val inventory = event.clickedInventory ?: return
        val player = event.whoClicked as? Player ?: return
        val currentItem = event.currentItem ?: return

        if (inventory.holder is CombatTeamHolder) {
            event.isCancelled = true
            val session = plugin.combatManager.getSession(player.uniqueId) ?: return
            val duelist = plugin.duelistManager.getDuelist(player.uniqueId) ?: return

            if (event.rawSlot in 10..15) {
                val index = event.rawSlot - 10
                if (index == 0) {
                    player.sendMessage(Component.text("Ese Companion ya está en combate.").color(NamedTextColor.RED))
                    return
                }

                val targetCompanion = duelist.team.getOrNull(index) ?: return
                if (targetCompanion.isFainted()) {
                    player.sendMessage(Component.text("¡Ese Companion no puede luchar!").color(NamedTextColor.RED))
                    return
                }

                val active = duelist.team[0]
                duelist.team[0] = targetCompanion
                duelist.team[index] = active

                player.closeInventory()
                player.sendMessage(Component.text("¡Ve, ${targetCompanion.nickname}!").color(NamedTextColor.GREEN))

                val opponent = if (session.player1.uuid == duelist.uuid) session.player2 else session.player1
                passTurn(session, opponent)
            }
        }

        if (inventory.holder is CombatBagMainHolder) {
            event.isCancelled = true
            val session = plugin.combatManager.getSession(player.uniqueId) ?: return
            val duelist = plugin.duelistManager.getDuelist(player.uniqueId) ?: return

            val loreLines = currentItem.itemMeta?.lore() ?: return
            val plainText = PlainTextComponentSerializer.plainText().serialize(loreLines[0])
            val itemId = plainText.replace("ID: ", "")

            val template = plugin.itemManager.getItem(itemId) ?: return

            if (template.type == ItemType.CAPTURE_DEVICE) {
                player.closeInventory()
                val opponent = if (session.player1.uuid == duelist.uuid) session.player2 else session.player1
                val renderer = VanillaCombatRenderer(plugin)
                session.transitionTo(AttemptCaptureState(plugin, duelist, opponent, itemId, renderer))
            } else {
                CombatInventoryGUIs.openCombatBagTarget(player, duelist, itemId)
            }
        }

        if (inventory.holder is CombatBagTargetHolder) {
            event.isCancelled = true
            val holder = inventory.holder as CombatBagTargetHolder
            val session = plugin.combatManager.getSession(player.uniqueId) ?: return
            val duelist = plugin.duelistManager.getDuelist(player.uniqueId) ?: return

            if (event.rawSlot in 10..15) {
                val index = event.rawSlot - 10

                val success = plugin.itemManager.useItem(duelist, holder.itemId, index, player)

                if (success) {
                    player.closeInventory()
                    val opponent = if (session.player1.uuid == duelist.uuid) session.player2 else session.player1
                    passTurn(session, opponent)
                }
            }
        }
    }

    private fun passTurn(session: org.ReDiego0.turnBasedCombat.game.CombatSession, opponent: Duelist) {
        val renderer = VanillaCombatRenderer(plugin)
        if (Bukkit.getPlayer(opponent.uuid) == null) {
            session.transitionTo(AITurnState(plugin, opponent, renderer))
        } else {
            session.transitionTo(PlayerTurnState(opponent, renderer))
        }
    }
}