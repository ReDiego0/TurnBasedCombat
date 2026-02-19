package org.ReDiego0.turnBasedCombat.manager

import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.model.Element
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class ElementManager(private val plugin: TurnBasedCombat) {

    private val elements = mutableMapOf<String, Element>()

    init {
        loadElements()
    }

    fun loadElements() {
        elements.clear()
        val file = File(plugin.dataFolder, "elements.yml")
        if (!file.exists()) {
            plugin.saveResource("elements.yml", false)
        }

        val config = YamlConfiguration.loadConfiguration(file)
        val keys = config.getKeys(false)

        for (key in keys) {
            val displayName = config.getString("$key.displayName") ?: key
            val weaknesses = config.getStringList("$key.weaknesses").toSet()
            val resistances = config.getStringList("$key.resistances").toSet()
            val immunities = config.getStringList("$key.immunities").toSet()
            val status = config.getString("$key.nativeStatusEffect")

            elements[key] = Element(key, displayName, weaknesses, resistances, immunities, status)
        }

        plugin.logger.info("Elementos cargados: ${elements.size}")
    }

    fun getElement(id: String): Element? = elements[id]

    fun calculateMultiplier(attackElementId: String, defenderSpeciesId: String): Double {
        val speciesElements = plugin.speciesManager.getSpecies(defenderSpeciesId)?.elements ?: listOf("normal")
        var multiplier = 1.0

        for (defElementId in speciesElements) {
            val defElement = getElement(defElementId) ?: continue

            when {
                defElement.weaknesses.contains(attackElementId) -> multiplier *= 2.0
                defElement.resistances.contains(attackElementId) -> multiplier *= 0.5
                defElement.immunities.contains(attackElementId) -> multiplier *= 0.0
            }
        }

        return multiplier
    }
}