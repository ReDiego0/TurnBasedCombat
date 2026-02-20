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

class PcGUI(private val plugin: TurnBasedCombat) {

    fun openFor(player: Player, duelist: Duelist) {
        val holder = PcMenuHolder(duelist)
        val inventory = Bukkit.createInventory(holder, 54, Component.text("Sistema de PC").color(NamedTextColor.DARK_AQUA))
        holder.setInventory(inventory)

        render(inventory, duelist)
        player.openInventory(inventory)
    }

    fun render(inventory: Inventory, duelist: Duelist) {
        inventory.clear()

        val glass = ItemStack(Material.BLACK_STAINED_GLASS_PANE)
        val meta = glass.itemMeta
        meta.displayName(Component.text(" "))
        glass.itemMeta = meta
        for (i in 6..8) inventory.setItem(i, glass)

        for ((index, companion) in duelist.team.withIndex()) {
            if (index > 5) break
            inventory.setItem(index, buildCompanionItem(companion))
        }

        for ((index, companion) in duelist.pcStorage.withIndex()) {
            val slot = index + 9
            if (slot > 53) break
            inventory.setItem(slot, buildCompanionItem(companion))
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