package org.ReDiego0.turnBasedCombat.view

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

class CombatTeamHolder : InventoryHolder { override fun getInventory(): Inventory = Bukkit.createInventory(this, 9) }
class CombatBagMainHolder : InventoryHolder { override fun getInventory(): Inventory = Bukkit.createInventory(this, 9) }
class CombatBagTargetHolder(val itemId: String) : InventoryHolder { override fun getInventory(): Inventory = Bukkit.createInventory(this, 9) }

object CombatInventoryGUIs {

    fun openCombatTeam(player: Player, duelist: Duelist) {
        val holder = CombatTeamHolder()
        val inv = Bukkit.createInventory(holder, 27, Component.text("Elegir Relevo").color(NamedTextColor.DARK_GREEN))

        for ((index, companion) in duelist.team.withIndex()) {
            val item = ItemStack(if (companion.isFainted()) Material.SKELETON_SKULL else Material.NAME_TAG)
            val meta = item.itemMeta
            meta.displayName(Component.text("${companion.nickname} (HP: ${companion.stats.hp.toInt()}/${companion.stats.maxHp.toInt()})")
                .color(if (companion.isFainted()) NamedTextColor.RED else NamedTextColor.GREEN))

            val lore = listOf(Component.text(if (index == 0) "¡Actualmente en combate!" else "Click para enviar al combate").color(NamedTextColor.GRAY))
            meta.lore(lore)
            item.itemMeta = meta
            inv.setItem(10 + index, item)
        }
        player.openInventory(inv)
    }

    fun openCombatBag(player: Player, duelist: Duelist) {
        val holder = CombatBagMainHolder()
        val plugin = TurnBasedCombat.instance
        val inv = Bukkit.createInventory(holder, 27, Component.text("Bolsa (Combate)").color(NamedTextColor.GOLD))

        var slot = 0
        for ((itemId, amount) in duelist.bag) {
            val template = plugin.itemManager.getItem(itemId) ?: continue
            val item = ItemStack(Material.POTION)
            val meta = item.itemMeta
            meta.displayName(Component.text("${template.displayName} (x$amount)").color(NamedTextColor.AQUA))
            meta.lore(listOf(Component.text("ID: $itemId").color(NamedTextColor.DARK_GRAY)))
            item.itemMeta = meta
            inv.setItem(slot++, item)
        }
        player.openInventory(inv)
    }

    fun openCombatBagTarget(player: Player, duelist: Duelist, itemId: String) {
        val holder = CombatBagTargetHolder(itemId)
        val inv = Bukkit.createInventory(holder, 27, Component.text("Usar Ítem en...").color(NamedTextColor.DARK_PURPLE))

        for ((index, companion) in duelist.team.withIndex()) {
            val item = ItemStack(Material.NAME_TAG)
            val meta = item.itemMeta
            meta.displayName(Component.text("${companion.nickname} (HP: ${companion.stats.hp.toInt()}/${companion.stats.maxHp.toInt()})").color(NamedTextColor.GOLD))
            item.itemMeta = meta
            inv.setItem(10 + index, item)
        }
        player.openInventory(inv)
    }
}