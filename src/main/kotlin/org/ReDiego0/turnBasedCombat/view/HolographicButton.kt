package org.ReDiego0.turnBasedCombat.view

import net.kyori.adventure.text.Component
import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Display
import org.bukkit.entity.Interaction
import org.bukkit.entity.TextDisplay
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f

class HolographicButton(
    private val plugin: TurnBasedCombat,
    private val location: Location,
    private val actionId: String,
    private val text: Component
) {
    private var displayEntity: TextDisplay? = null
    var interactionEntity: Interaction? = null
        private set

    fun spawn() {
        val world = location.world ?: return

        displayEntity = world.spawn(location, TextDisplay::class.java) { display ->
            display.text(text)
            display.billboard = Display.Billboard.CENTER
            display.backgroundColor = Color.fromARGB(150, 0, 0, 0)
            display.brightness = Display.Brightness(15, 15)

            display.transformation = Transformation(
                Vector3f(0f, 0f, 0f),
                AxisAngle4f(0f, 0f, 0f, 1f),
                Vector3f(1.5f, 1.5f, 1.5f),
                AxisAngle4f(0f, 0f, 0f, 1f)
            )
        }

        interactionEntity = world.spawn(location, Interaction::class.java) { interaction ->
            interaction.interactionWidth = 1.2f
            interaction.interactionHeight = 0.6f

            val key = NamespacedKey(plugin, "combat_action")
            interaction.persistentDataContainer.set(key, PersistentDataType.STRING, actionId)
        }
    }

    fun updateText(newText: Component) {
        displayEntity?.text(newText)
    }

    fun remove() {
        displayEntity?.remove()
        interactionEntity?.remove()
    }
}