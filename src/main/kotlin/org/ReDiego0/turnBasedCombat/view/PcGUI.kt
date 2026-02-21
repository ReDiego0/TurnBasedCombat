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
import kotlin.math.min

class PcGUI(private val plugin: TurnBasedCombat) {

    fun openFor(player: Player, duelist: Duelist, page: Int = 0) {
        val holder = PcMenuHolder(duelist, page)
        val inventory = Bukkit.createInventory(holder, 54, Component.text("Sistema de PC - Pág ${page + 1}").color(NamedTextColor.DARK_AQUA))
        holder.setInventory(inventory)

        render(inventory, duelist, page)
        player.openInventory(inventory)
    }

    fun render(inventory: Inventory, duelist: Duelist, page: Int) {
        inventory.clear()

        for ((index, companion) in duelist.team.withIndex()) {
            if (index > 5) break
            inventory.setItem(index, buildCompanionItem(companion))
        }

        val prevItem = ItemStack(Material.ARROW)
        val prevMeta = prevItem.itemMeta
        prevMeta.displayName(Component.text("<- Página Anterior").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
        prevItem.itemMeta = prevMeta
        inventory.setItem(6, prevItem)

        val infoItem = ItemStack(Material.BOOK)
        val infoMeta = infoItem.itemMeta
        infoMeta.displayName(Component.text("Página ${page + 1} / 8").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false))
        infoItem.itemMeta = infoMeta
        inventory.setItem(7, infoItem)

        val nextItem = ItemStack(Material.ARROW)
        val nextMeta = nextItem.itemMeta
        nextMeta.displayName(Component.text("Página Siguiente ->").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
        nextItem.itemMeta = nextMeta
        inventory.setItem(8, nextItem)

        val startIndex = page * 45
        val endIndex = min(startIndex + 45, duelist.pcStorage.size)

        if (startIndex < duelist.pcStorage.size) {
            val pageCompanions = duelist.pcStorage.subList(startIndex, endIndex)
            for ((index, companion) in pageCompanions.withIndex()) {
                inventory.setItem(index + 9, buildCompanionItem(companion))
            }
        }
    }

    private fun buildCompanionItem(companion: Companion): ItemStack {
        val item = ItemStack(Material.NAME_TAG)
        val meta = item.itemMeta

        meta.displayName(Component.text("${companion.nickname} (Nv. ${companion.level})")
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false))

        val species = plugin.speciesManager.getSpecies(companion.speciesId)
        val speciesName = species?.displayName ?: companion.speciesId

        val lore = mutableListOf<Component>()
        lore.add(Component.text("Especie: $speciesName").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
        lore.add(Component.text("HP: ${companion.stats.hp.toInt()} / ${companion.stats.maxHp.toInt()}").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
        lore.add(Component.empty())
        lore.add(Component.text("¡Click para mover!").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))

        meta.lore(lore)
        item.itemMeta = meta
        return item
    }
}