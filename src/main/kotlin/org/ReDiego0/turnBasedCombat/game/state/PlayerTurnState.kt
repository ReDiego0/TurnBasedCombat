// Deprecated
//package org.ReDiego0.turnBasedCombat.game.state
//
//import net.kyori.adventure.text.Component
//import net.kyori.adventure.text.format.NamedTextColor
//import org.ReDiego0.turnBasedCombat.game.CombatSession
//import org.ReDiego0.turnBasedCombat.model.Duelist
//import org.ReDiego0.turnBasedCombat.view.CombatRenderer
//import org.ReDiego0.turnBasedCombat.view.CombatInventoryGUIs
//import org.bukkit.Bukkit
//import kotlin.random.Random
//
//class PlayerTurnState(
//    private val activePlayer: Duelist,
//    private val renderer: CombatRenderer
//) : CombatState {
//
//    override fun onEnter(session: CombatSession) {
//        renderer.showMainMenu(session, activePlayer)
//    }
//
//    override fun onInput(session: CombatSession, player: Duelist, inputId: String) {
//        if (player.uuid != activePlayer.uuid) return
//
//        val bukkitPlayer = Bukkit.getPlayer(player.uuid) ?: return
//        val opponent = getOpponent(session, activePlayer)
//
//        when {
//            inputId == "menu_fight" -> {
//                renderer.showTechniquesMenu(session, activePlayer)
//            }
//            inputId == "menu_main" -> {
//                renderer.showMainMenu(session, activePlayer)
//            }
//            inputId == "menu_bag" -> {
//                CombatInventoryGUIs.openCombatBag(bukkitPlayer, activePlayer)
//            }
//            inputId == "menu_team" -> {
//                CombatInventoryGUIs.openCombatTeam(bukkitPlayer, activePlayer)
//            }
//            inputId == "menu_flee" -> {
//                if (!opponent.isWild) {
//                    bukkitPlayer.sendMessage(Component.text("¡No puedes huir de un combate contra otro duelista!").color(NamedTextColor.RED))
//                    return
//                }
//
//                val activeCompanion = activePlayer.team.firstOrNull { !it.isFainted() }
//                val oppCompanion = opponent.team.firstOrNull { !it.isFainted() }
//
//                if (activeCompanion == null || oppCompanion == null) return
//
//                val activeSpeed = activeCompanion.stats.speed.toDouble()
//                val oppSpeed = oppCompanion.stats.speed.toDouble()
//
//                val f = (activeSpeed * 128.0) / oppSpeed
//
//                if (f >= 255.0 || Random.nextDouble(0.0, 255.0) < f) {
//                    bukkitPlayer.sendMessage(Component.text("¡Has escapado con éxito!").color(NamedTextColor.GREEN))
//                    session.endCombat(null)
//                } else {
//                    bukkitPlayer.sendMessage(Component.text("¡No pudiste escapar!").color(NamedTextColor.RED))
//                    routeNextTurn(session, opponent)
//                }
//            }
//            inputId.startsWith("tech_") -> {
//                val techniqueId = inputId.removePrefix("tech_")
//                val plugin = org.ReDiego0.turnBasedCombat.TurnBasedCombat.instance
//                session.transitionTo(ExecuteTechniqueState(plugin, activePlayer, techniqueId, renderer))
//            }
//        }
//    }
//
//    private fun routeNextTurn(session: CombatSession, nextPlayer: Duelist) {
//        val plugin = org.ReDiego0.turnBasedCombat.TurnBasedCombat.instance
//        if (Bukkit.getPlayer(nextPlayer.uuid) == null) {
//            session.transitionTo(AITurnState(plugin, nextPlayer, renderer))
//        } else {
//            session.transitionTo(PlayerTurnState(nextPlayer, renderer))
//        }
//    }
//
//    override fun onTick(session: CombatSession) {}
//
//    override fun onExit(session: CombatSession) {
//        renderer.clearMenu(activePlayer)
//    }
//
//    private fun getOpponent(session: CombatSession, player: Duelist): Duelist {
//        return if (session.player1.uuid == player.uuid) session.player2 else session.player1
//    }
//}