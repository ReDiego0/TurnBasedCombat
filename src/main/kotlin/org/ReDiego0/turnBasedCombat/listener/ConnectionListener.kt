package org.ReDiego0.turnBasedCombat.listener

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.ReDiego0.turnBasedCombat.manager.DuelistManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class ConnectionListener(private val manager: DuelistManager) : Listener {

    @EventHandler
    fun onPreLogin(event: AsyncPlayerPreLoginEvent) {
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        manager.loadDuelist(player.uniqueId, player.name).thenAccept { duelist ->
            player.sendMessage(Component.text("¡Bienvenido, Entrenador! Datos cargados.")
                .color(NamedTextColor.GREEN))
        }.exceptionally { ex ->
            player.sendMessage(Component.text("Error cargando perfil. Reporta esto al admin.")
                .color(NamedTextColor.RED))
            ex.printStackTrace()
            null
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        manager.saveAndRemove(event.player.uniqueId)
    }
}