package org.ReDiego0.turnBasedCombat.listener

import org.ReDiego0.turnBasedCombat.view.TeamMenuHolder
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class MenuListener : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val inventory = event.clickedInventory ?: return

        if (inventory.holder is TeamMenuHolder || event.inventory.holder is TeamMenuHolder) {
            event.isCancelled = true
        }
    }
}