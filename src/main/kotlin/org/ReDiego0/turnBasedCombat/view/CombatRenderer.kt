package org.ReDiego0.turnBasedCombat.view

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.game.CombatSession
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.bukkit.Location
import org.bukkit.entity.Player
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

        val anchor = player.location.clone().add(player.location.direction.multiply(3.5)).add(0.0, 1.2, 0.0)
        val buttons = mutableListOf<HolographicButton>()

        buttons.add(createButton(player, anchor, -1.2, 0.4, "menu_fight", Component.text("⚔️ Luchar").color(NamedTextColor.RED)))
        buttons.add(createButton(player, anchor, 1.2, 0.4, "menu_bag", Component.text("🎒 Bóveda").color(NamedTextColor.GOLD)))
        buttons.add(createButton(player, anchor, -1.2, -0.4, "menu_team", Component.text("🛡️ Equipo").color(NamedTextColor.GREEN)))
        buttons.add(createButton(player, anchor, 1.2, -0.4, "menu_flee", Component.text("🏃 Huir").color(NamedTextColor.GRAY)))

        buttons.forEach {
            it.spawn()
            it.interactionEntity?.let { entity -> session.activeEntities.add(entity) }
        }
        activeButtons[duelist.uuid] = buttons
    }

    override fun showTechniquesMenu(session: CombatSession, duelist: Duelist) {
        clearMenu(duelist)

        val player = plugin.server.getPlayer(duelist.uuid) ?: return
        val anchor = player.location.clone().add(player.location.direction.multiply(3.5)).add(0.0, 1.2, 0.0)

        val buttons = mutableListOf<HolographicButton>()
        val activeCompanion = duelist.team.firstOrNull { !it.isFainted() } ?: return

        activeCompanion.moves.forEachIndexed { index, techniqueId ->
            val xOffset = if (index % 2 == 0) -1.2 else 1.2
            val yOffset = if (index < 2) 0.4 else -0.4

            val technique = plugin.techniqueManager.getTechnique(techniqueId)
            val displayName = technique?.displayName ?: techniqueId

            buttons.add(createButton(
                player, anchor, xOffset, yOffset, "tech_$techniqueId",
                Component.text("▶ $displayName").color(NamedTextColor.AQUA)
            ))
        }

        buttons.add(createButton(player, anchor, 0.0, -1.2, "menu_main", Component.text("↩ Volver").color(NamedTextColor.GRAY)))

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

    private fun createButton(player: Player, anchor: Location, offsetX: Double, offsetY: Double, action: String, text: Component): HolographicButton {
        val direction = player.location.direction.clone().setY(0.0).normalize()
        val up = Vector(0, 1, 0)

        val right = direction.getCrossProduct(up).normalize()

        val loc = anchor.clone()
            .add(right.multiply(offsetX))
            .add(0.0, offsetY, 0.0)

        return HolographicButton(plugin, loc, action, text)
    }
}