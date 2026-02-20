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
            "team" -> handleTeam(sender)
            "iaduel" -> {
                if (sender.hasPermission("tbc.admin")) handleIaDuel(sender, args)
            }

            "heal" -> {
                if (sender.hasPermission("tbc.admin")) handleHeal(sender, args)
            }

            "pc" -> {
                if (sender.hasPermission("tbc.admin")) handlePc(sender, args)
            }
            else -> sender.sendMessage(Component.text("Comando desconocido.").color(NamedTextColor.RED))
        }

        return true
    }

    private fun handlePc(sender: CommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso correcto: /tbc pc <jugador>").color(NamedTextColor.RED))
            return
        }

        val targetPlayer = org.bukkit.Bukkit.getPlayerExact(args[1])
        if (targetPlayer == null) {
            sender.sendMessage(Component.text("Jugador no encontrado o desconectado.").color(NamedTextColor.RED))
            return
        }

        val duelist = plugin.duelistManager.getDuelist(targetPlayer.uniqueId)
        if (duelist == null) {
            sender.sendMessage(Component.text("Error al cargar los datos del duelista.").color(NamedTextColor.RED))
            return
        }

        if (plugin.combatManager.isInCombat(targetPlayer.uniqueId)) {
            sender.sendMessage(Component.text("El jugador no puede acceder al PC mientras está en combate.").color(NamedTextColor.RED))
            return
        }

        val gui = org.ReDiego0.turnBasedCombat.view.PcGUI(plugin)
        gui.openFor(targetPlayer, duelist)

        if (sender != targetPlayer) {
            sender.sendMessage(Component.text("Abriendo el PC para ${targetPlayer.name}.").color(NamedTextColor.GREEN))
        }
    }

    private fun handleTeam(sender: Player) {
        val duelist = plugin.duelistManager.getDuelist(sender.uniqueId)
        if (duelist == null) {
            sender.sendMessage(Component.text("Error al cargar los datos de tu perfil.").color(NamedTextColor.RED))
            return
        }

        val gui = org.ReDiego0.turnBasedCombat.view.TeamGUI(plugin)
        gui.openFor(sender, duelist)
    }

    private fun handleHeal(sender: CommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage(Component.text("Uso correcto: /tbc heal <jugador>").color(NamedTextColor.RED))
            return
        }

        val targetPlayer = Bukkit.getPlayerExact(args[1])
        if (targetPlayer == null) {
            sender.sendMessage(Component.text("Jugador no encontrado o desconectado.").color(NamedTextColor.RED))
            return
        }

        val duelist = plugin.duelistManager.getDuelist(targetPlayer.uniqueId)
        if (duelist == null) {
            sender.sendMessage(Component.text("Error al cargar los datos del duelista.").color(NamedTextColor.RED))
            return
        }

        if (plugin.combatManager.isInCombat(targetPlayer.uniqueId)) {
            sender.sendMessage(Component.text("No puedes curar a un jugador que está en medio de un combate.").color(NamedTextColor.RED))
            return
        }

        var curados = 0
        for (companion in duelist.team) {
            if (companion.stats.hp < companion.stats.maxHp) {
                companion.stats.hp = companion.stats.maxHp
                curados++
            }
        }

        sender.sendMessage(Component.text("Has curado el equipo de ${targetPlayer.name} ($curados Companions curados).").color(NamedTextColor.GREEN))

        targetPlayer.sendMessage(Component.text("¡Tus Companions han sido completamente curados!").color(NamedTextColor.GREEN))
    }

    private fun handleIaDuel(sender: CommandSender, args: Array<String>) {
        if (args.size < 3) {
            sender.sendMessage(Component.text("Uso correcto: /tbc iaduel <npc_id> <jugador>").color(NamedTextColor.RED))
            return
        }

        val npcId = args[1]
        val targetName = args[2]

        val targetPlayer = Bukkit.getPlayerExact(targetName)
        if (targetPlayer == null) {
            sender.sendMessage(Component.text("Jugador no encontrado.").color(NamedTextColor.RED))
            return
        }

        if (plugin.combatManager.isInCombat(targetPlayer.uniqueId)) {
            sender.sendMessage(Component.text("El jugador ya está en combate.").color(NamedTextColor.RED))
            return
        }

        val targetDuelist = plugin.duelistManager.getDuelist(targetPlayer.uniqueId)
        val npcDuelist = plugin.npcManager.createNpcDuelist(npcId)

        if (targetDuelist == null || npcDuelist == null) {
            sender.sendMessage(Component.text("Error al cargar los datos del jugador o el NPC no existe.").color(NamedTextColor.RED))
            return
        }

        val arenaLocation = targetPlayer.location.clone().apply { pitch = 0f }

        plugin.combatManager.startDuel(targetDuelist, npcDuelist, arenaLocation)
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