package org.ReDiego0.turnBasedCombat.manager

import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.model.CombatStats
import org.ReDiego0.turnBasedCombat.model.EvolutionRequirement
import org.ReDiego0.turnBasedCombat.model.SpeciesTemplate
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class SpeciesManager(private val plugin: TurnBasedCombat) {

    private val species = mutableMapOf<String, SpeciesTemplate>()

    init {
        loadSpecies()
    }

    fun loadSpecies() {
        species.clear()
        val file = File(plugin.dataFolder, "species.yml")
        if (!file.exists()) {
            plugin.saveResource("species.yml", false)
        }

        val config = YamlConfiguration.loadConfiguration(file)
        val keys = config.getKeys(false)

        for (key in keys) {
            val displayName = config.getString("$key.displayName") ?: key
            val elements = config.getStringList("$key.elements")
            val modelId = config.getString("$key.modelId") ?: "unknown"

            val hp = config.getDouble("$key.baseStats.hp", 50.0)
            val attack = config.getInt("$key.baseStats.attack", 50)
            val defense = config.getInt("$key.baseStats.defense", 50)
            val speed = config.getInt("$key.baseStats.speed", 50)
            val accuracy = config.getInt("$key.baseStats.accuracy", 100)
            val critChance = config.getDouble("$key.baseStats.criticalChance", 0.05)

            val baseStats = CombatStats(hp, hp, attack, defense, speed, accuracy, critChance)

            val learnset = mutableMapOf<Int, List<String>>()
            val learnsetSection = config.getConfigurationSection("$key.learnset")

            val catchRate = config.getInt("$key.catchRate", 255)

            if (learnsetSection != null) {
                for (levelStr in learnsetSection.getKeys(false)) {
                    val level = levelStr.toIntOrNull() ?: continue
                    learnset[level] = learnsetSection.getStringList(levelStr)
                }
            }

            val evolutions = mutableMapOf<String, EvolutionRequirement>()
            val evoSection = config.getConfigurationSection("$key.evolutions")
            if (evoSection != null) {
                for (targetId in evoSection.getKeys(false)) {
                    val reqLevel = evoSection.getInt("$targetId.level", 0)
                    val reqItem = evoSection.getString("$targetId.item")
                    evolutions[targetId] = EvolutionRequirement(reqLevel, reqItem)
                }
            }

            species[key] = SpeciesTemplate(key, displayName, elements, baseStats, learnset, modelId, catchRate, evolutions)
        }

        plugin.logger.info("Especies cargadas: ${species.size}")
    }

    fun getSpecies(id: String): SpeciesTemplate? = species[id]
}