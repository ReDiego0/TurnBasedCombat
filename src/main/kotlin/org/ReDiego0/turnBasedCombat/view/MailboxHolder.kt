package org.ReDiego0.turnBasedCombat.view

import org.ReDiego0.turnBasedCombat.model.Duelist
import org.ReDiego0.turnBasedCombat.model.MailMessage
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class MailboxHolder(
    val duelist: Duelist,
    var page: Int = 0,
    var activeMessage: MailMessage? = null
) : InventoryHolder {
    private lateinit var inventory: Inventory

    override fun getInventory(): Inventory {
        return inventory
    }

    fun setInventory(inv: Inventory) {
        this.inventory = inv
    }
}