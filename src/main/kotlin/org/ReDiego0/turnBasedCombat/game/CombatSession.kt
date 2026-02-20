package org.ReDiego0.turnBasedCombat.game

import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.game.state.CombatState
import org.ReDiego0.turnBasedCombat.game.state.InitializationState
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.ReDiego0.turnBasedCombat.view.VanillaCombatRenderer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask

class CombatSession(
    private val plugin: TurnBasedCombat,
    val player1: Duelist,
    val player2: Duelist,
    private val triggerLocation: Location
) {
    val pendingActions: MutableList<TurnAction> = mutableListOf()

    var currentState: CombatState? = null
        private set

    private var tickTask: BukkitTask? = null
    var turnCounter: Int = 0
    val activeEntities = mutableListOf<org.bukkit.entity.Entity>()

    private lateinit var p1OriginalLoc: Location
    private lateinit var p2OriginalLoc: Location
    lateinit var arenaCenter: Location

    fun start() {
        val p1 = Bukkit.getPlayer(player1.uuid)
        val p2 = Bukkit.getPlayer(player2.uuid)

        if (p1 == null || p2 == null) return

        p1OriginalLoc = p1.location.clone()
        p2OriginalLoc = p2.location.clone()

        arenaCenter = plugin.arenaManager.getArenaForLocation(triggerLocation)

        setupPlayerForCombat(p1, arenaCenter.clone().add(5.0, 0.0, 0.0).apply { yaw = 90f })
        setupPlayerForCombat(p2, arenaCenter.clone().add(-5.0, 0.0, 0.0).apply { yaw = -90f })

        isolatePlayers(p1, p2)

        tickTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            currentState?.onTick(this)
        }, 0L, 1L)

        val renderer = VanillaCombatRenderer(plugin)
        transitionTo(InitializationState(plugin, renderer))
    }

    fun transitionTo(newState: CombatState) {
        currentState?.onExit(this)
        currentState = newState
        currentState?.onEnter(this)
    }

    fun endCombat(winner: Duelist?) {
        tickTask?.cancel()
        currentState?.onExit(this)

        activeEntities.forEach { it.remove() }
        activeEntities.clear()

        val p1 = Bukkit.getPlayer(player1.uuid)
        val p2 = Bukkit.getPlayer(player2.uuid)

        p1?.let { restorePlayer(it, p1OriginalLoc) }
        p2?.let { restorePlayer(it, p2OriginalLoc) }

        if (p1 != null && p2 != null) {
            reintegratePlayers(p1, p2)
        }

        plugin.combatManager.removeSession(this)
    }

    private fun setupPlayerForCombat(player: Player, loc: Location) {
        player.teleport(loc)
        player.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, PotionEffect.INFINITE_DURATION, 255, false, false, false))
        player.addPotionEffect(PotionEffect(PotionEffectType.JUMP_BOOST, PotionEffect.INFINITE_DURATION, 250, false, false, false))
    }

    private fun restorePlayer(player: Player, loc: Location) {
        player.teleport(loc)
        player.removePotionEffect(PotionEffectType.SLOWNESS)
        player.removePotionEffect(PotionEffectType.JUMP_BOOST)
    }

    private fun isolatePlayers(p1: Player, p2: Player) {
        for (onlinePlayer in Bukkit.getOnlinePlayers()) {
            if (onlinePlayer != p1 && onlinePlayer != p2) {
                p1.hidePlayer(plugin, onlinePlayer)
                p2.hidePlayer(plugin, onlinePlayer)
                onlinePlayer.hidePlayer(plugin, p1)
                onlinePlayer.hidePlayer(plugin, p2)
            }
        }
    }

    private fun reintegratePlayers(p1: Player, p2: Player) {
        for (onlinePlayer in Bukkit.getOnlinePlayers()) {
            p1.showPlayer(plugin, onlinePlayer)
            p2.showPlayer(plugin, onlinePlayer)
            onlinePlayer.showPlayer(plugin, p1)
            onlinePlayer.showPlayer(plugin, p2)
        }
    }
}