package org.ReDiego0.turnBasedCombat.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DuelCommand(private val plugin: TurnBasedCombat) : CommandExecutor {

    private val pendingRequests = ConcurrentHashMap<UUID, UUID>()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) return true

        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Uso: /tbc duel <jugador> o /tbc accept").color(NamedTextColor.RED))
            return true
        }

        when (args[0].lowercase()) {
            "duel" -> handleDuelRequest(sender, args)
            "accept" -> handleAccept(sender)
            else -> sender.sendMessage(Component.text("Comando desconocido.").color(NamedTextColor.RED))
        }

        return true
    }

    private fun handleDuelRequest(sender: Player, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage(Component.text("Especifica un jugador: /tbc duel <jugador>").color(NamedTextColor.RED))
            return
        }

        val target = Bukkit.getPlayerExact(args[1])
        if (target == null || target == sender) {
            sender.sendMessage(Component.text("Jugador no encontrado o inválido.").color(NamedTextColor.RED))
            return
        }

        if (plugin.combatManager.isInCombat(sender.uniqueId) || plugin.combatManager.isInCombat(target.uniqueId)) {
            sender.sendMessage(Component.text("Uno de los jugadores ya está en combate.").color(NamedTextColor.RED))
            return
        }

        pendingRequests[target.uniqueId] = sender.uniqueId

        sender.sendMessage(Component.text("Petición de duelo enviada a ${target.name}.").color(NamedTextColor.GREEN))

        val acceptMessage = Component.text("¡${sender.name} te ha retado a un duelo! ")
            .color(NamedTextColor.YELLOW)
            .append(Component.text("[ACEPTAR]")
                .color(NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/tbc accept")))

        target.sendMessage(acceptMessage)
    }

    private fun handleAccept(sender: Player) {
        val requesterId = pendingRequests.remove(sender.uniqueId)
        if (requesterId == null) {
            sender.sendMessage(Component.text("No tienes peticiones pendientes.").color(NamedTextColor.RED))
            return
        }

        val requester = Bukkit.getPlayer(requesterId)
        if (requester == null) {
            sender.sendMessage(Component.text("El retador ya no está conectado.").color(NamedTextColor.RED))
            return
        }

        val senderDuelist = plugin.duelistManager.getDuelist(sender.uniqueId)
        val requesterDuelist = plugin.duelistManager.getDuelist(requesterId)

        if (senderDuelist == null || requesterDuelist == null) {
            sender.sendMessage(Component.text("Error cargando datos de los duelistas.").color(NamedTextColor.RED))
            return
        }

        val arenaLocation = sender.location.clone().apply { pitch = 0f }

        plugin.combatManager.startDuel(requesterDuelist, senderDuelist, arenaLocation)

        val startMsg = Component.text("¡El duelo ha comenzado!").color(NamedTextColor.AQUA)
        requester.sendMessage(startMsg)
        sender.sendMessage(startMsg)
    }
}