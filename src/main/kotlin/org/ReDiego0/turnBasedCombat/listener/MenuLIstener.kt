package org.ReDiego0.turnBasedCombat.listener

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.view.PcGUI
import org.ReDiego0.turnBasedCombat.view.PcMenuHolder
import org.ReDiego0.turnBasedCombat.view.TeamMenuHolder
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class MenuListener(private val plugin: TurnBasedCombat) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val inventory = event.clickedInventory ?: return
        val player = event.whoClicked as? Player ?: return

        if (inventory.holder is TeamMenuHolder || event.inventory.holder is TeamMenuHolder) {
            event.isCancelled = true
        }

        if (inventory.holder is PcMenuHolder) {
            event.isCancelled = true
            val holder = inventory.holder as PcMenuHolder
            val duelist = holder.duelist
            val slot = event.rawSlot
            val currentPage = holder.page

            if (slot == 6) { // Flecha de Atrás
                if (currentPage > 0) {
                    PcGUI(plugin).openFor(player, duelist, currentPage - 1)
                }
                return
            }

            if (slot == 8) { // Flecha de Siguiente
                if (currentPage < 7) {
                    PcGUI(plugin).openFor(player, duelist, currentPage + 1)
                }
                return
            }

            if (slot in 0..5) {
                if (slot < duelist.team.size) {
                    if (duelist.team.size > 1) {
                        val comp = duelist.team.removeAt(slot)
                        duelist.pcStorage.add(comp)
                        PcGUI(plugin).render(event.inventory, duelist, currentPage)
                    } else {
                        player.sendMessage(Component.text("¡Debes tener al menos un Companion en tu equipo!").color(NamedTextColor.RED))
                    }
                }
            }
            else if (slot in 9..53) {
                val pcIndex = (slot - 9) + (currentPage * 45)

                if (pcIndex < duelist.pcStorage.size) {
                    if (duelist.team.size < 6) {
                        val comp = duelist.pcStorage.removeAt(pcIndex)
                        duelist.team.add(comp)
                        PcGUI(plugin).render(event.inventory, duelist, currentPage)
                    } else {
                        player.sendMessage(Component.text("¡Tu equipo ya está lleno! (Máx 6)").color(NamedTextColor.RED))
                    }
                }
            }
        }
    }
}