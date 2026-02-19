package org.ReDiego0.turnBasedCombat.listener

import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.manager.CombatManager
import org.ReDiego0.turnBasedCombat.manager.DuelistManager
import org.bukkit.NamespacedKey
import org.bukkit.entity.Interaction
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.persistence.PersistentDataType

class CombatInteractionListener(
    plugin: TurnBasedCombat,
    private val combatManager: CombatManager,
    private val duelistManager: DuelistManager
) : Listener {

    private val actionKey = NamespacedKey(plugin, "combat_action")

    @EventHandler
    fun onRightClick(event: PlayerInteractEntityEvent) {
        val entity = event.rightClicked
        if (entity !is Interaction) return

        val actionId = entity.persistentDataContainer.get(actionKey, PersistentDataType.STRING) ?: return
        event.isCancelled = true

        processInput(event.player, actionId)
    }

    @EventHandler
    fun onLeftClick(event: EntityDamageByEntityEvent) {
        val entity = event.entity
        val damager = event.damager

        if (entity !is Interaction || damager !is Player) return

        val actionId = entity.persistentDataContainer.get(actionKey, PersistentDataType.STRING) ?: return
        event.isCancelled = true

        processInput(damager, actionId)
    }

    private fun processInput(player: Player, actionId: String) {
        val session = combatManager.getSession(player.uniqueId) ?: return
        val duelist = duelistManager.getDuelist(player.uniqueId) ?: return

        session.currentState?.onInput(session, duelist, actionId)
    }
}