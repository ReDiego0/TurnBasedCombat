package org.ReDiego0.turnBasedCombat.view

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.model.Companion
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class TeachGUI(private val plugin: TurnBasedCombat) {

    fun openFor(player: Player, duelist: Duelist, companion: Companion, newTechId: String, mail: org.ReDiego0.turnBasedCombat.model.MailMessage) {
        val holder = TeachMenuHolder(duelist, companion, newTechId, mail)
        val inventory = Bukkit.createInventory(holder, 54, Component.text("Aprender nueva técnica").color(NamedTextColor.DARK_PURPLE))
        holder.setInventory(inventory)

        render(inventory, holder, duelist, companion, newTechId)
        player.openInventory(inventory)
    }

    fun render(inventory: Inventory, holder: TeachMenuHolder, duelist: Duelist, companion: Companion, newTechId: String) {
        inventory.clear()

        val compItem = ItemStack(Material.NAME_TAG)
        val compMeta = compItem.itemMeta
        val speciesName = plugin.speciesManager.getSpecies(companion.speciesId)?.displayName ?: "Desconocido"
        compMeta.displayName(Component.text("${companion.nickname} (Nv. ${companion.level})").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
        compMeta.lore(listOf(
            Component.text("Especie: $speciesName").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("HP: ${companion.stats.maxHp.toInt()} | ATK: ${companion.stats.attack}").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
        ))
        compItem.itemMeta = compMeta
        inventory.setItem(13, compItem)

        val backItem = ItemStack(Material.ARROW)
        val backMeta = backItem.itemMeta
        backMeta.displayName(Component.text("Volver al buzón").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
        backItem.itemMeta = backMeta
        inventory.setItem(45, backItem)

        if (holder.confirmMoveIndex == null) {
            val moveSlots = listOf(28, 30, 32, 34)

            for (i in 0 until 4) {
                if (i < companion.moves.size) {
                    val techId = companion.moves[i]
                    inventory.setItem(moveSlots[i], buildTechItem(techId, false))
                }
            }

            inventory.setItem(49, buildTechItem(newTechId, true))
        }
        else {
            val oldTechId = companion.moves[holder.confirmMoveIndex!!]

            inventory.setItem(30, buildTechItem(oldTechId, false))
            inventory.setItem(32, buildTechItem(newTechId, true))

            val acceptItem = ItemStack(Material.GREEN_STAINED_GLASS_PANE)
            val accMeta = acceptItem.itemMeta
            accMeta.displayName(Component.text("SÍ, OLVIDAR Y APRENDER").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))
            acceptItem.itemMeta = accMeta
            listOf(20, 21, 22, 29).forEach { inventory.setItem(it, acceptItem) } // Dejamos el 30 para el item viejo

            val cancelItem = ItemStack(Material.RED_STAINED_GLASS_PANE)
            val canMeta = cancelItem.itemMeta
            canMeta.displayName(Component.text("NO, CANCELAR").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
            cancelItem.itemMeta = canMeta
            listOf(24, 25, 26, 33, 35).forEach { inventory.setItem(it, cancelItem) } // Dejamos el 34 libre por estética
        }
    }

    private fun buildTechItem(techId: String, isNew: Boolean): ItemStack {
        val tech = plugin.techniqueManager.getTechnique(techId)
        val item = ItemStack(if (isNew) Material.ENCHANTED_BOOK else Material.BOOK)
        val meta = item.itemMeta

        val color = if (isNew) NamedTextColor.GREEN else NamedTextColor.AQUA
        val titlePrefix = if (isNew) "¡NUEVA! " else ""

        meta.displayName(Component.text(titlePrefix + (tech?.displayName ?: techId)).color(color).decoration(TextDecoration.ITALIC, false))

        if (tech != null) {
            meta.lore(listOf(
                Component.text("Poder: ${tech.power} | Precisión: ${tech.accuracy}%").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text(if (isNew) "Esta es la técnica que quieres aprender." else "Haz clic para olvidar esta técnica.").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
            ))
        }
        item.itemMeta = meta
        return item
    }
}