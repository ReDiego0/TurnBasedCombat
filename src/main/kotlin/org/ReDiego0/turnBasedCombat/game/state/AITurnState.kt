// Deprecated
//package org.ReDiego0.turnBasedCombat.game.state
//
//import org.ReDiego0.turnBasedCombat.TurnBasedCombat
//import org.ReDiego0.turnBasedCombat.game.CombatSession
//import org.ReDiego0.turnBasedCombat.model.Duelist
//import org.ReDiego0.turnBasedCombat.view.CombatRenderer
//import org.bukkit.Bukkit
//
//class AITurnState(
//    private val plugin: TurnBasedCombat,
//    private val activeNpc: Duelist,
//    private val renderer: CombatRenderer
//) : CombatState {
//
//    override fun onEnter(session: CombatSession) {
//        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
//            processAiDecision(session)
//        }, 30L)
//    }
//
//    private fun processAiDecision(session: CombatSession) {
//        val activeCompanion = activeNpc.team.firstOrNull { !it.isFainted() }
//
//        if (activeCompanion == null || activeCompanion.moves.isEmpty()) {
//            val opponent = if (session.player1.uuid == activeNpc.uuid) session.player2 else session.player1
//            session.endCombat(opponent)
//            return
//        }
//
//        val chosenMoveId = activeCompanion.moves.random()
//
//        session.transitionTo(ExecuteTechniqueState(plugin, activeNpc, chosenMoveId, renderer))
//    }
//
//    override fun onInput(session: CombatSession, player: Duelist, inputId: String) {}
//    override fun onTick(session: CombatSession) {}
//    override fun onExit(session: CombatSession) {}
//}