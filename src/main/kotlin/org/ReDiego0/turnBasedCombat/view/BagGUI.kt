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

class BagMainHolder : InventoryHolder { override fun getInventory(): Inventory = Bukkit.createInventory(this, 9) }
class BagActionHolder(val itemId: String) : InventoryHolder { override fun getInventory(): Inventory = Bukkit.createInventory(this, 9) }
class BagTargetHolder(val itemId: String) : InventoryHolder { override fun getInventory(): Inventory = Bukkit.createInventory(this, 9) }

class BagGUIs(private val plugin: TurnBasedCombat) {

    fun openMainMenu(player: Player, duelist: Duelist) {
        val holder = BagMainHolder()
        val inv = Bukkit.createInventory(holder, 27, Component.text("Bolsa de Ítems").color(NamedTextColor.GOLD))

        var slot = 0
        for ((itemId, amount) in duelist.bag) {
            val itemTemplate = plugin.itemManager.getItem(itemId) ?: continue
            val item = ItemStack(Material.POTION)
            val meta = item.itemMeta
            meta.displayName(Component.text("${itemTemplate.displayName} (x$amount)").color(NamedTextColor.AQUA))

            val lore = listOf(Component.text("ID: $itemId").color(NamedTextColor.DARK_GRAY))
            meta.lore(lore)

            item.itemMeta = meta
            inv.setItem(slot++, item)
        }
        player.openInventory(inv)
    }

    fun openActionMenu(player: Player, itemId: String) {
        val template = plugin.itemManager.getItem(itemId) ?: return
        val holder = BagActionHolder(itemId)
        val inv = Bukkit.createInventory(holder, 27, Component.text("Acción: ${template.displayName}").color(NamedTextColor.DARK_GRAY))

        val btnUse = ItemStack(Material.LIME_DYE)
        val metaUse = btnUse.itemMeta
        metaUse.displayName(Component.text("✓ USAR ÍTEM").color(NamedTextColor.GREEN))
        btnUse.itemMeta = metaUse
        inv.setItem(11, btnUse)

        val btnBack = ItemStack(Material.RED_DYE)
        val metaBack = btnBack.itemMeta
        metaBack.displayName(Component.text("✖ VOLVER").color(NamedTextColor.RED))
        btnBack.itemMeta = metaBack
        inv.setItem(15, btnBack)

        player.openInventory(inv)
    }

    fun openTargetMenu(player: Player, duelist: Duelist, itemId: String) {
        val holder = BagTargetHolder(itemId)
        val inv = Bukkit.createInventory(holder, 27, Component.text("¿En qué Companion usarlo?").color(NamedTextColor.DARK_PURPLE))

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