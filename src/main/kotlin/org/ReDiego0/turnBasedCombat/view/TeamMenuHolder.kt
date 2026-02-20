package org.ReDiego0.turnBasedCombat.view

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class TeamMenuHolder : InventoryHolder {
    private lateinit var inventory: Inventory

    override fun getInventory(): Inventory {
        return inventory
    }

    fun setInventory(inv: Inventory) {
        this.inventory = inv
    }
}