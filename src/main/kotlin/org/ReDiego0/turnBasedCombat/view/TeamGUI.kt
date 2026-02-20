package org.ReDiego0.turnBasedCombat.view

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class TeamGUI(private val plugin: TurnBasedCombat) {

    fun openFor(player: Player, duelist: Duelist) {
        val holder = TeamMenuHolder()
        val inventory = Bukkit.createInventory(holder, 27, Component.text("Tu Equipo de Companions").color(NamedTextColor.DARK_AQUA))
        holder.setInventory(inventory)

        for ((index, companion) in duelist.team.withIndex()) {
            val item = ItemStack(Material.NAME_TAG)
            val meta = item.itemMeta

            meta.displayName(Component.text("${companion.nickname} (Nv. ${companion.level})")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false))

            val species = plugin.speciesManager.getSpecies(companion.speciesId)
            val speciesName = species?.displayName ?: companion.speciesId

            val lore = mutableListOf<Component>()

            lore.add(Component.text("Especie: $speciesName").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
            lore.add(Component.empty())
            lore.add(Component.text("Estadísticas de Combate:").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false))
            lore.add(Component.text(" HP: ${companion.stats.hp.toInt()} / ${companion.stats.maxHp.toInt()}").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
            lore.add(Component.text(" Ataque: ${companion.stats.attack}").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
            lore.add(Component.text(" Defensa: ${companion.stats.defense}").color(NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false))
            lore.add(Component.text(" Velocidad: ${companion.stats.speed}").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))
            lore.add(Component.text(" EXP: ${companion.xp.toInt()}").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false))
            lore.add(Component.empty())
            lore.add(Component.text("Técnicas Aprendidas:").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))

            if (companion.moves.isEmpty()) {
                lore.add(Component.text(" Ninguna").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false))
            } else {
                for (moveId in companion.moves) {
                    val technique = plugin.techniqueManager.getTechnique(moveId)
                    val moveName = technique?.displayName ?: moveId
                    lore.add(Component.text(" - $moveName").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false))
                }
            }

            meta.lore(lore)
            item.itemMeta = meta

            val slot = 10 + index
            inventory.setItem(slot, item)
        }

        player.openInventory(inventory)
    }
}