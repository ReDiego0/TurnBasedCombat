package org.ReDiego0.turnBasedCombat.listener

import net.kyori.adventure.text.Component
import org.ReDiego0.turnBasedCombat.manager.DuelistManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class ConnectionListener(private val manager: DuelistManager) : Listener {

    @EventHandler
    fun onPreLogin(event: AsyncPlayerPreLoginEvent) {
        val uuid = event.uniqueId
        val name = event.name

        try {
            manager.loadDuelistData(uuid, name).join()
        } catch (e: Exception) {
            event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                Component.text("Error cargando tus datos de perfil. Contacta a un admin.")
            )
            e.printStackTrace()
        }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val duelist = manager.getDuelist(event.player.uniqueId)

        if (duelist != null) {
            // Lógica futura: Mostrar el HUD inicial aquí
            event.player.sendMessage("¡Bienvenido duelista! [debug]")
        } else {
            event.player.kick(Component.text("Error crítico de sincronización."))
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        manager.saveDuelistData(event.player.uniqueId)
    }
}