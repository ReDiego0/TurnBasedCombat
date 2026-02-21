package org.ReDiego0.turnBasedCombat.view

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.ReDiego0.turnBasedCombat.model.MailMessage
import org.ReDiego0.turnBasedCombat.model.MailType
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import kotlin.math.min

class MailboxGUI(private val plugin: TurnBasedCombat) {

    private val leftPanelSlots = listOf(
        0, 1, 2, 3, 4,
        9, 10, 11, 12, 13,
        18, 19, 20, 21, 22,
        27, 28, 29, 30, 31,
        36, 37, 38, 39, 40
    )

    private val rightPanelSlots = listOf(
        5, 6, 7, 8,
        14, 15, 16, 17,
        23, 24, 25, 26,
        32, 33, 34, 35
    )

    private val actionSlots = listOf(41, 42, 43, 44)

    fun openFor(player: Player, duelist: Duelist, page: Int = 0, activeMessage: MailMessage? = null) {
        val holder = MailboxHolder(duelist, page, activeMessage)
        val inventory = Bukkit.createInventory(holder, 54, Component.text("Buzón - Pág ${page + 1}").color(NamedTextColor.GOLD))
        holder.setInventory(inventory)

        render(inventory, duelist, page, activeMessage)
        player.openInventory(inventory)
    }

    fun render(inventory: Inventory, duelist: Duelist, page: Int, activeMessage: MailMessage?) {
        inventory.clear()

        val bgItem = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val bgMeta = bgItem.itemMeta
        bgMeta.displayName(Component.empty())
        bgItem.itemMeta = bgMeta

        rightPanelSlots.forEach { inventory.setItem(it, bgItem) }
        actionSlots.forEach { inventory.setItem(it, bgItem) }

        val startIndex = page * 5
        val endIndex = min(startIndex + 5, duelist.mailbox.size)

        if (startIndex < duelist.mailbox.size) {
            val pageMails = duelist.mailbox.subList(startIndex, endIndex)

            for ((index, mail) in pageMails.withIndex()) {
                val icon = buildMailIcon(mail, plugin)

                val rowStart = index * 5
                for (i in 0 until 5) {
                    inventory.setItem(leftPanelSlots[rowStart + i], icon)
                }
            }
        }

        if (activeMessage != null) {
            val paperItem = ItemStack(Material.PAPER)
            val paperMeta = paperItem.itemMeta
            paperMeta.displayName(Component.text(activeMessage.title).color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false))

            val lore = mutableListOf<Component>()
            lore.add(Component.text(activeMessage.body).color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false))

            if (activeMessage.type == MailType.REWARD && activeMessage.attachedItems != null) {
                lore.add(Component.empty())
                lore.add(Component.text("Adjuntos:").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                for ((itemId, amount) in activeMessage.attachedItems) {
                    val itemName = plugin.itemManager.getItem(itemId)?.displayName ?: itemId
                    lore.add(Component.text("- $amount x $itemName").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                }
            }
            paperMeta.lore(lore)
            paperItem.itemMeta = paperMeta

            inventory.setItem(rightPanelSlots[5], paperItem)

            val actionBtn = buildActionButton(activeMessage)
            inventory.setItem(42, actionBtn)

            val deleteBtn = ItemStack(Material.BARRIER)
            val delMeta = deleteBtn.itemMeta
            val delColor = if (activeMessage.isDeletable()) NamedTextColor.RED else NamedTextColor.GRAY
            delMeta.displayName(Component.text("Borrar Mensaje").color(delColor).decoration(TextDecoration.ITALIC, false))
            deleteBtn.itemMeta = delMeta
            inventory.setItem(43, deleteBtn)
        }

        val prevItem = ItemStack(Material.ARROW)
        val prevMeta = prevItem.itemMeta
        prevMeta.displayName(Component.text("<- Página Anterior").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
        prevItem.itemMeta = prevMeta
        inventory.setItem(45, prevItem)

        val clearItem = ItemStack(Material.LAVA_BUCKET)
        val clearMeta = clearItem.itemMeta
        clearMeta.displayName(Component.text("Borrar TODOS los leídos").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
        clearItem.itemMeta = clearMeta
        inventory.setItem(49, clearItem)

        val nextItem = ItemStack(Material.ARROW)
        val nextMeta = nextItem.itemMeta
        nextMeta.displayName(Component.text("Página Siguiente ->").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
        nextItem.itemMeta = nextMeta
        inventory.setItem(53, nextItem)
    }

    private fun buildMailIcon(mail: MailMessage, plugin: TurnBasedCombat): ItemStack {
        val mat = if (mail.isRead) Material.PAPER else Material.ENCHANTED_BOOK
        val item = ItemStack(mat)
        val meta = item.itemMeta

        val titleColor = if (mail.isRead) NamedTextColor.GRAY else NamedTextColor.GOLD
        val readTag = if (mail.isRead) " [Leído]" else ""

        meta.displayName(Component.text(mail.title + readTag).color(titleColor).decoration(TextDecoration.ITALIC, false))

        val lore = mutableListOf<Component>()
        if (mail.type == MailType.TECHNIQUE) {
            lore.add(Component.text("Haz clic para que aprenda la técnica.").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
        } else {
            lore.add(Component.text("Haz clic para revisar el mensaje.").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
        }
        meta.lore(lore)
        item.itemMeta = meta
        return item
    }

    private fun buildActionButton(mail: MailMessage): ItemStack {
        val item = ItemStack(Material.EMERALD_BLOCK)
        val meta = item.itemMeta

        when (mail.type) {
            MailType.INFO -> {
                item.type = Material.BARRIER
                meta.displayName(Component.text("Cerrar Panel").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
            }
            MailType.REWARD -> {
                val color = if (mail.isClaimed) NamedTextColor.GRAY else NamedTextColor.GREEN
                val txt = if (mail.isClaimed) "Recompensas Reclamadas" else "Reclamar Recompensas"
                meta.displayName(Component.text(txt).color(color).decoration(TextDecoration.ITALIC, false))
            }
            MailType.TECHNIQUE -> {
                val color = if (mail.isClaimed) NamedTextColor.GRAY else NamedTextColor.GREEN
                val txt = if (mail.isClaimed) "Técnica Entrenada" else "Aprender Técnica"
                meta.displayName(Component.text(txt).color(color).decoration(TextDecoration.ITALIC, false))
            }
        }

        item.itemMeta = meta
        return item
    }
}