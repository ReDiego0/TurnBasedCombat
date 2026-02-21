package org.ReDiego0.turnBasedCombat.listener

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.model.MailType
import org.ReDiego0.turnBasedCombat.view.MailboxGUI
import org.ReDiego0.turnBasedCombat.view.MailboxHolder
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

            if (slot == 6) {
                if (currentPage > 0) PcGUI(plugin).openFor(player, duelist, currentPage - 1)
                return
            }

            if (slot == 8) {
                if (currentPage < 7) PcGUI(plugin).openFor(player, duelist, currentPage + 1)
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

        if (inventory.holder is MailboxHolder) {
            event.isCancelled = true
            val holder = inventory.holder as MailboxHolder
            val duelist = holder.duelist
            val slot = event.rawSlot
            val currentPage = holder.page
            val activeMail = holder.activeMessage

            if (slot == 45) {
                if (currentPage > 0) MailboxGUI(plugin).openFor(player, duelist, currentPage - 1, null)
                return
            }
            if (slot == 53) {
                val maxPage = (duelist.mailbox.size - 1) / 5
                if (currentPage < maxPage && maxPage > 0) {
                    MailboxGUI(plugin).openFor(player, duelist, currentPage + 1, null)
                }
                return
            }
            if (slot == 49) {
                val removed = duelist.mailbox.removeAll { it.isDeletable() }
                if (removed) {
                    player.sendMessage(Component.text("Has limpiado los correos antiguos del buzón.").color(NamedTextColor.GREEN))
                    MailboxGUI(plugin).openFor(player, duelist, 0, null)
                } else {
                    player.sendMessage(Component.text("No hay mensajes leídos/reclamados para borrar.").color(NamedTextColor.RED))
                }
                return
            }

            val isLeftPanel = (slot % 9) <= 4 && slot < 45
            if (isLeftPanel) {
                val rowIndex = slot / 9
                val mailIndex = (currentPage * 5) + rowIndex

                if (mailIndex < duelist.mailbox.size) {
                    val clickedMail = duelist.mailbox[mailIndex]
                    clickedMail.isRead = true
                    holder.activeMessage = clickedMail
                    MailboxGUI(plugin).render(inventory, duelist, currentPage, clickedMail)
                }
                return
            }

            if (activeMail != null) {
                if (slot == 42) {
                    when (activeMail.type) {
                        MailType.INFO -> {
                            holder.activeMessage = null
                            MailboxGUI(plugin).render(inventory, duelist, currentPage, null)
                        }
                        MailType.REWARD -> {
                            if (!activeMail.isClaimed) {
                                activeMail.attachedItems?.forEach { (itemId, amount) ->
                                    val currentAmount = duelist.bag[itemId] ?: 0
                                    duelist.bag[itemId] = currentAmount + amount
                                }
                                activeMail.isClaimed = true
                                player.sendMessage(Component.text("¡Recompensas transferidas a tu bóveda!").color(NamedTextColor.GREEN))
                                MailboxGUI(plugin).render(inventory, duelist, currentPage, activeMail)
                            }
                        }
                        MailType.TECHNIQUE -> {
                            if (!activeMail.isClaimed) {
                                val compId = activeMail.targetCompanionId
                                val comp = duelist.team.find { it.id == compId } ?: duelist.pcStorage.find { it.id == compId }
                                val techId = activeMail.techniqueId

                                if (comp != null && techId != null) {
                                    if (comp.moves.size < 4) {
                                        if (!comp.moves.contains(techId)) {
                                            comp.moves.add(techId)
                                            activeMail.isClaimed = true
                                            player.sendMessage(Component.text("¡${comp.nickname} aprendió una nueva técnica!").color(NamedTextColor.AQUA))
                                            MailboxGUI(plugin).render(inventory, duelist, currentPage, activeMail)
                                        } else {
                                            activeMail.isClaimed = true
                                            player.sendMessage(Component.text("¡${comp.nickname} ya conoce esta técnica!").color(NamedTextColor.GRAY))
                                            MailboxGUI(plugin).render(inventory, duelist, currentPage, activeMail)
                                        }
                                    } else {
                                        player.sendMessage(Component.text("${comp.nickname} ya conoce 4 movimientos. ¡Próximamente podrás reemplazar uno!").color(NamedTextColor.YELLOW))
                                    }
                                } else {
                                    player.sendMessage(Component.text("No se encontró el Companion (¿Fue liberado?).").color(NamedTextColor.RED))
                                    activeMail.isClaimed = true
                                    MailboxGUI(plugin).render(inventory, duelist, currentPage, activeMail)
                                }
                            }
                        }
                    }
                }

                if (slot == 43) {
                    if (activeMail.isDeletable()) {
                        duelist.mailbox.remove(activeMail)
                        holder.activeMessage = null
                        MailboxGUI(plugin).render(inventory, duelist, currentPage, null)
                    } else {
                        player.sendMessage(Component.text("Aún tienes recompensas por reclamar o entrenamientos pendientes.").color(NamedTextColor.RED))
                    }
                }
            }
        }
    }
}