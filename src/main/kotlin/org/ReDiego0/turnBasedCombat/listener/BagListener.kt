package org.ReDiego0.turnBasedCombat.listener

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.view.BagActionHolder
import org.ReDiego0.turnBasedCombat.view.BagGUIs
import org.ReDiego0.turnBasedCombat.view.BagMainHolder
import org.ReDiego0.turnBasedCombat.view.BagTargetHolder
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class BagListener(private val plugin: TurnBasedCombat) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val inventory = event.clickedInventory ?: return
        val currentItem = event.currentItem ?: return

        if (inventory.holder is BagMainHolder) {
            event.isCancelled = true

            val loreLines = currentItem.itemMeta?.lore() ?: return
            val plainText = PlainTextComponentSerializer.plainText().serialize(loreLines[0])
            val itemId = plainText.replace("ID: ", "")

            BagGUIs(plugin).openActionMenu(player, itemId)
        }

        if (inventory.holder is BagActionHolder) {
            event.isCancelled = true
            val holder = inventory.holder as BagActionHolder
            val duelist = plugin.duelistManager.getDuelist(player.uniqueId) ?: return

            when (event.rawSlot) {
                11 -> { // Botón Usar
                    BagGUIs(plugin).openTargetMenu(player, duelist, holder.itemId)
                }
                15 -> { // Botón Volver
                    BagGUIs(plugin).openMainMenu(player, duelist)
                }
            }
        }

        if (inventory.holder is BagTargetHolder) {
            event.isCancelled = true
            val holder = inventory.holder as BagTargetHolder
            val duelist = plugin.duelistManager.getDuelist(player.uniqueId) ?: return
            if (event.rawSlot in 10..15) {
                val companionIndex = event.rawSlot - 10

                if (companionIndex < duelist.team.size) {
                    val success = plugin.itemManager.useItem(duelist, holder.itemId, companionIndex, player)

                    if (success) {
                        player.closeInventory()
                    } else {
                        // Reproducir sonido de error
                    }
                }
            }
        }
    }
}