package org.ReDiego0.turnBasedCombat.game.state

import org.ReDiego0.turnBasedCombat.game.CombatSession
import org.ReDiego0.turnBasedCombat.model.Duelist

interface CombatState {
    /**
     * Se ejecuta una vez al entrar en este estado.
     */
    fun onEnter(session: CombatSession)

    /**
     * Maneja la entrada del jugador (Clicks en la GUI).
     */
    fun onInput(session: CombatSession, player: Duelist, inputId: String)

    /**
     * Se ejecuta cada tick (Game Loop).
     */
    fun onTick(session: CombatSession)

    /**
     * Se ejecuta al salir del estado.
     */
    fun onExit(session: CombatSession)
}