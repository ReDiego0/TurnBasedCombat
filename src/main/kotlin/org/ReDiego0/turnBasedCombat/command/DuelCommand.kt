package org.ReDiego0.turnBasedCombat.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.model.MailMessage
import org.ReDiego0.turnBasedCombat.model.MailType
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
        if (sender !is Player) {
            if (args.isNotEmpty() && (args[0].lowercase() == "sendmail" || args[0].lowercase() == "sendreward")) {
                if (args[0].lowercase() == "sendmail") handleSendMail(sender, args)
                if (args[0].lowercase() == "sendreward") handleSendReward(sender, args)
                return true
            }
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Uso: /tbc duel <jugador> o /tbc accept").color(NamedTextColor.RED))
            return true
        }

        when (args[0].lowercase()) {
            "duel" -> handleDuelRequest(sender, args)
            "accept" -> handleAccept(sender)
            "team" -> handleTeam(sender)
            "bag" -> handleBag(sender)
            "mailbox", "buzon" -> handleMailbox(sender)

            "giveitem" -> {
                if (sender.hasPermission("tbc.admin")) handleGiveItem(sender, args)
            }
            "iaduel" -> {
                if (sender.hasPermission("tbc.admin")) handleIaDuel(sender, args)
            }
            "givecompanion" -> {
                if (sender.hasPermission("tbc.admin")) handleGiveCompanion(sender, args)
            }
            "heal" -> {
                if (sender.hasPermission("tbc.admin")) handleHeal(sender, args)
            }
            "pc" -> {
                if (sender.hasPermission("tbc.admin")) handlePc(sender, args)
            }
            "sendmail" -> {
                if (sender.hasPermission("tbc.admin")) handleSendMail(sender, args)
            }
            "sendreward" -> {
                if (sender.hasPermission("tbc.admin")) handleSendReward(sender, args)
            }
            else -> sender.sendMessage(Component.text("Comando desconocido.").color(NamedTextColor.RED))
        }

        return true
    }

    private fun handleMailbox(sender: Player) {
        val duelist = plugin.duelistManager.getDuelist(sender.uniqueId)
        if (duelist == null) {
            sender.sendMessage(Component.text("Error al cargar los datos de tu perfil.").color(NamedTextColor.RED))
            return
        }

        if (plugin.combatManager.isInCombat(sender.uniqueId)) {
            sender.sendMessage(Component.text("No puedes revisar tu correo mientras estás en combate.").color(NamedTextColor.RED))
            return
        }

        val gui = org.ReDiego0.turnBasedCombat.view.MailboxGUI(plugin)
        gui.openFor(sender, duelist)
    }

    private fun handleSendMail(sender: CommandSender, args: Array<String>) {
        if (args.size < 4) {
            sender.sendMessage(Component.text("Uso: /tbc sendmail <jugador|online|all> <firma:true|false> <mensaje...>").color(NamedTextColor.RED))
            return
        }

        val targetType = args[1].lowercase()
        val showAdmin = args[2].toBooleanStrictOrNull() ?: false
        var messageBody = args.drop(3).joinToString(" ")

        if (showAdmin) {
            val senderName = if (sender is Player) sender.name else "Administración"
            messageBody += "\n\n- Atte: $senderName"
        }

        val mailTemplate = MailMessage(
            type = MailType.INFO,
            title = "Mensaje de Administración",
            body = messageBody
        )

        distributeMail(sender, targetType, mailTemplate)
    }

    private fun handleSendReward(sender: CommandSender, args: Array<String>) {
        if (args.size < 5) {
            sender.sendMessage(Component.text("Uso: /tbc sendreward <jugador|online|all> <item:cant,item:cant...> <firma:true|false> <mensaje...>").color(NamedTextColor.RED))
            return
        }

        val targetType = args[1].lowercase()
        val itemsString = args[2] // Formato esperado: pocion:5,piedra_fuego:1
        val showAdmin = args[3].toBooleanStrictOrNull() ?: false
        var messageBody = args.drop(4).joinToString(" ")

        val attachedItems = mutableMapOf<String, Int>()
        val itemPairs = itemsString.split(",")

        for (pair in itemPairs) {
            val parts = pair.split(":")
            val itemId = parts[0]
            val amount = if (parts.size > 1) parts[1].toIntOrNull() ?: 1 else 1

            if (plugin.itemManager.getItem(itemId) == null) {
                sender.sendMessage(Component.text("El ítem '$itemId' no existe en items.yml. Envío cancelado.").color(NamedTextColor.RED))
                return
            }
            attachedItems[itemId] = (attachedItems[itemId] ?: 0) + amount
        }

        if (showAdmin) {
            val senderName = if (sender is Player) sender.name else "Administración"
            messageBody += "\n\n- Atte: $senderName"
        }

        val mailTemplate = MailMessage(
            type = MailType.REWARD,
            title = "🎁 ¡Tienes un Regalo!",
            body = messageBody,
            attachedItems = attachedItems
        )

        distributeMail(sender, targetType, mailTemplate)
    }

    private fun distributeMail(sender: CommandSender, targetType: String, template: MailMessage) {
        when (targetType) {
            "online" -> {
                var count = 0
                for (player in Bukkit.getOnlinePlayers()) {
                    val duelist = plugin.duelistManager.getDuelist(player.uniqueId) ?: continue
                    duelist.mailbox.add(template.copy(id = UUID.randomUUID()))
                    player.sendMessage(Component.text("¡Tienes un nuevo correo! Usa /tbc buzon").color(NamedTextColor.GOLD))
                    count++
                }
                sender.sendMessage(Component.text("Mensaje enviado a $count jugadores conectados.").color(NamedTextColor.GREEN))
            }
            "all" -> {
                sender.sendMessage(Component.text("Iniciando envío masivo a todos los jugadores registrados... (Esto puede tardar unos segundos)").color(NamedTextColor.YELLOW))

                Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                    try {
                        plugin.database.getConnection().use { conn ->
                            val stmt = conn.prepareStatement("SELECT uuid FROM tbc_duelists")
                            val rs = stmt.executeQuery()
                            var count = 0

                            while (rs.next()) {
                                val uuidStr = rs.getString("uuid")
                                val uuid = runCatching { UUID.fromString(uuidStr) }.getOrNull() ?: continue

                                val duelist = plugin.duelistManager.getDuelist(uuid)
                                    ?: plugin.duelistManager.loadDuelistData(uuid, "Offline").join()

                                duelist.mailbox.add(template.copy(id = UUID.randomUUID()))

                                if (Bukkit.getPlayer(uuid) == null) {
                                    plugin.duelistManager.saveDuelistData(uuid)
                                } else {
                                    Bukkit.getPlayer(uuid)?.sendMessage(Component.text("¡Tienes un nuevo correo! Usa /tbc buzon").color(NamedTextColor.GOLD))
                                }
                                count++

                                Thread.sleep(25)
                            }
                            sender.sendMessage(Component.text("¡Envío masivo completado! Mensaje entregado a $count usuarios.").color(NamedTextColor.GREEN))
                        }
                    } catch (e: Exception) {
                        sender.sendMessage(Component.text("Ocurrió un error crítico en el envío masivo.").color(NamedTextColor.RED))
                        e.printStackTrace()
                    }
                })
            }
            else -> {
                val targetPlayer = Bukkit.getPlayerExact(targetType)
                if (targetPlayer == null) {
                    sender.sendMessage(Component.text("Jugador '$targetType' no encontrado o desconectado.").color(NamedTextColor.RED))
                    return
                }
                val duelist = plugin.duelistManager.getDuelist(targetPlayer.uniqueId) ?: return
                duelist.mailbox.add(template.copy(id = UUID.randomUUID()))
                sender.sendMessage(Component.text("Mensaje enviado a ${targetPlayer.name}.").color(NamedTextColor.GREEN))
                targetPlayer.sendMessage(Component.text("¡Tienes un nuevo correo! Usa /tbc buzon").color(NamedTextColor.GOLD))
            }
        }
    }

    private fun handleGiveItem(sender: CommandSender, args: Array<String>) {
        if (args.size < 4) {
            sender.sendMessage(Component.text("Uso: /tbc giveitem <jugador> <item_id> <cantidad>").color(NamedTextColor.RED))
            return
        }

        val targetPlayer = Bukkit.getPlayerExact(args[1])
        if (targetPlayer == null) {
            sender.sendMessage(Component.text("Jugador no encontrado.").color(NamedTextColor.RED))
            return
        }

        val duelist = plugin.duelistManager.getDuelist(targetPlayer.uniqueId) ?: return
        val itemId = args[2]

        val itemTemplate = plugin.itemManager.getItem(itemId)
        if (itemTemplate == null) {
            sender.sendMessage(Component.text("El ítem '$itemId' no existe en items.yml.").color(NamedTextColor.RED))
            return
        }

        val amount = args[3].toIntOrNull() ?: 1
        val currentAmount = duelist.bag[itemId] ?: 0

        duelist.bag[itemId] = currentAmount + amount

        sender.sendMessage(Component.text("Has dado $amount x ${itemTemplate.displayName} a ${targetPlayer.name}.").color(NamedTextColor.GREEN))
        targetPlayer.sendMessage(Component.text("¡Has recibido $amount x ${itemTemplate.displayName}!").color(NamedTextColor.GREEN))
    }

    private fun handleGiveCompanion(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("tbc.admin")) {
            sender.sendMessage(Component.text("No tienes permisos.").color(NamedTextColor.RED))
            return
        }

        if (args.size < 4) {
            sender.sendMessage(Component.text("Uso: /tbc givecompanion <jugador> <species_id> <nivel>").color(NamedTextColor.RED))
            return
        }

        val targetPlayer = Bukkit.getPlayerExact(args[1])
        if (targetPlayer == null) {
            sender.sendMessage(Component.text("Jugador no encontrado.").color(NamedTextColor.RED))
            return
        }

        val duelist = plugin.duelistManager.getDuelist(targetPlayer.uniqueId) ?: return
        val speciesId = args[2]
        val species = plugin.speciesManager.getSpecies(speciesId)

        if (species == null) {
            sender.sendMessage(Component.text("Especie no encontrada en species.yml.").color(NamedTextColor.RED))
            return
        }

        val level = args[3].toIntOrNull() ?: 1

        val availableMoves = mutableListOf<String>()
        for ((learnLevel, moves) in species.learnset) {
            if (level >= learnLevel) availableMoves.addAll(moves)
        }
        val finalMoves = availableMoves.takeLast(4).toMutableList()

        val scaleFactor = 1.0 + (level * 0.05)
        val stats = org.ReDiego0.turnBasedCombat.model.CombatStats(
            hp = species.baseStats.hp * scaleFactor,
            maxHp = species.baseStats.hp * scaleFactor,
            attack = (species.baseStats.attack * scaleFactor).toInt(),
            defense = (species.baseStats.defense * scaleFactor).toInt(),
            speed = (species.baseStats.speed * scaleFactor).toInt(),
            accuracy = species.baseStats.accuracy,
            criticalChance = species.baseStats.criticalChance
        )

        val newCompanion = org.ReDiego0.turnBasedCombat.model.Companion(
            id = -1,
            ownerUuid = duelist.uuid,
            speciesId = speciesId,
            nickname = species.displayName,
            level = level,
            xp = 0.0,
            stats = stats,
            moves = finalMoves
        )

        if (duelist.team.size < 6) {
            duelist.team.add(newCompanion)
            targetPlayer.sendMessage(Component.text("¡Has recibido un ${species.displayName} (Nv. $level)!").color(NamedTextColor.GREEN))
        } else {
            duelist.pcStorage.add(newCompanion)
            targetPlayer.sendMessage(Component.text("¡Has recibido un ${species.displayName} (Nv. $level)! Se ha enviado al PC.").color(NamedTextColor.GREEN))
        }

        sender.sendMessage(Component.text("Companion entregado a ${targetPlayer.name}.").color(NamedTextColor.GREEN))
    }

    private fun handleBag(sender: Player) {
        val duelist = plugin.duelistManager.getDuelist(sender.uniqueId)
        if (duelist == null) {
            sender.sendMessage(Component.text("Error al cargar los datos de tu perfil.").color(NamedTextColor.RED))
            return
        }

        val guiManager = org.ReDiego0.turnBasedCombat.view.BagGUIs(plugin)
        guiManager.openMainMenu(sender, duelist)
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
            var needsHeal = false

            if (companion.stats.hp < companion.stats.maxHp) {
                companion.stats.hp = companion.stats.maxHp
                needsHeal = true
            }

            if (companion.activeStatus != null) {
                companion.activeStatus = null
                needsHeal = true
            }

            if (companion.movePP.isNotEmpty()) {
                companion.movePP.clear()
                needsHeal = true
            }

            if (needsHeal) curados++
        }

        sender.sendMessage(Component.text("[Administración] Has curado el equipo de ${targetPlayer.name} ($curados Companions restaurados).").color(NamedTextColor.GREEN))
        targetPlayer.sendMessage(Component.text("¡Todo tu equipo ha recuperado su HP, PP y salud!").color(NamedTextColor.GREEN))
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