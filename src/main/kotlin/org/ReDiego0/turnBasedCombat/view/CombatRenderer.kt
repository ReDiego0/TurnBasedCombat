package org.ReDiego0.turnBasedCombat.view

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.game.CombatSession
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.bukkit.Location
import org.bukkit.util.Vector
import java.util.UUID

interface CombatRenderer {
    fun showMainMenu(session: CombatSession, duelist: Duelist)
    fun showTechniquesMenu(session: CombatSession, duelist: Duelist)
    fun clearMenu(duelist: Duelist)
}

class VanillaCombatRenderer(
    private val plugin: TurnBasedCombat
) : CombatRenderer {

    private val activeButtons = mutableMapOf<UUID, MutableList<HolographicButton>>()

    override fun showMainMenu(session: CombatSession, duelist: Duelist) {
        clearMenu(duelist)

        val player = plugin.server.getPlayer(duelist.uuid) ?: return
        val anchor = player.location.add(player.location.direction.multiply(3.0))
        val buttons = mutableListOf<HolographicButton>()

        buttons.add(createButton(anchor, -1.0, 0.5, "menu_fight", Component.text("⚔️ Luchar").color(NamedTextColor.RED)))
        buttons.add(createButton(anchor, 1.0, 0.5, "menu_bag", Component.text("🎒 Bóveda").color(NamedTextColor.GOLD)))
        buttons.add(createButton(anchor, -1.0, -0.5, "menu_team", Component.text("🛡️ Equipo").color(NamedTextColor.GREEN)))
        buttons.add(createButton(anchor, 1.0, -0.5, "menu_flee", Component.text("🏃 Huir").color(NamedTextColor.GRAY)))

        buttons.forEach {
            it.spawn()
            it.interactionEntity?.let { entity -> session.activeEntities.add(entity) }
        }
        activeButtons[duelist.uuid] = buttons
    }

    override fun showTechniquesMenu(session: CombatSession, duelist: Duelist) {
        clearMenu(duelist)

        val player = plugin.server.getPlayer(duelist.uuid) ?: return
        val anchor = player.location.add(player.location.direction.multiply(3.0))
        val buttons = mutableListOf<HolographicButton>()
        val activeCompanion = duelist.team.firstOrNull { !it.isFainted() } ?: return

        activeCompanion.moves.forEachIndexed { index, techniqueId ->
            val xOffset = if (index % 2 == 0) -1.0 else 1.0
            val yOffset = if (index < 2) 0.5 else -0.5

            buttons.add(createButton(
                anchor, xOffset, yOffset, "tech_$techniqueId",
                Component.text(techniqueId).color(NamedTextColor.AQUA)
            ))
        }

        buttons.add(createButton(anchor, 0.0, -1.5, "menu_main", Component.text("↩ Volver").color(NamedTextColor.GRAY)))

        buttons.forEach {
            it.spawn()
            it.interactionEntity?.let { entity -> session.activeEntities.add(entity) }
        }
        activeButtons[duelist.uuid] = buttons
    }

    override fun clearMenu(duelist: Duelist) {
        activeButtons[duelist.uuid]?.forEach { it.remove() }
        activeButtons[duelist.uuid]?.clear()
    }

    private fun createButton(anchor: Location, x: Double, y: Double, action: String, text: Component): HolographicButton {
        val loc = anchor.clone().add(Vector(x, y, 0.0))
        return HolographicButton(plugin, loc, action, text)
    }
}