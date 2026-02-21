package org.ReDiego0.turnBasedCombat.manager

import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.model.Technique
import org.ReDiego0.turnBasedCombat.model.TechniqueCategory
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class TechniqueManager(private val plugin: TurnBasedCombat) {

    private val techniques = mutableMapOf<String, Technique>()

    init {
        loadTechniques()
    }

    fun loadTechniques() {
        techniques.clear()
        val file = File(plugin.dataFolder, "techniques.yml")
        if (!file.exists()) {
            plugin.saveResource("techniques.yml", false)
        }

        val config = YamlConfiguration.loadConfiguration(file)
        val keys = config.getKeys(false)

        for (key in keys) {
            val displayName = config.getString("$key.displayName") ?: key
            val elementId = config.getString("$key.elementId") ?: "normal"
            val categoryStr = config.getString("$key.category")?.uppercase() ?: "PHYSICAL"

            val category = runCatching { TechniqueCategory.valueOf(categoryStr) }
                .getOrDefault(TechniqueCategory.PHYSICAL)

            val power = config.getInt("$key.power", 40)
            val accuracy = config.getInt("$key.accuracy", 100)
            val maxPP = config.getInt("$key.pp", 20)
            val statusChance = config.getDouble("$key.applyStatusChance", 0.0)

            val statusPower = config.getInt("$key.statusPower", 0)
            val statusDuration = config.getInt("$key.statusDuration", 3)

            techniques[key] = Technique(
                key, displayName, elementId, category, power,
                accuracy, maxPP, statusChance, statusPower, statusDuration
            )
        }

        plugin.logger.info("Técnicas cargadas: ${techniques.size}")
    }

    fun getTechnique(id: String): Technique? = techniques[id]
}