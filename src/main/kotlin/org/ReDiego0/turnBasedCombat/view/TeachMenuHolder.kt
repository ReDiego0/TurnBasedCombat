package org.ReDiego0.turnBasedCombat.view

import org.ReDiego0.turnBasedCombat.model.Companion
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.ReDiego0.turnBasedCombat.model.MailMessage
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class TeachMenuHolder(
    val duelist: Duelist,
    val companion: Companion,
    val newTechniqueId: String,
    val activeMail: MailMessage,
    var confirmMoveIndex: Int? = null
) : InventoryHolder {
    private lateinit var inventory: Inventory
    override fun getInventory(): Inventory = inventory
    fun setInventory(inv: Inventory) { this.inventory = inv }
}