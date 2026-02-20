package org.ReDiego0.turnBasedCombat.manager

import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.model.CombatStats
import org.ReDiego0.turnBasedCombat.model.Companion
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.ReDiego0.turnBasedCombat.model.RegionTemplate
import org.bukkit.Particle
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.util.UUID
import kotlin.random.Random

class RegionManager(private val plugin: TurnBasedCombat) {

    private val regions = mutableMapOf<String, RegionTemplate>()

    init {
        loadRegions()
    }

    fun loadRegions() {
        regions.clear()
        val file = File(plugin.dataFolder, "regions.yml")
        if (!file.exists()) plugin.saveResource("regions.yml", false)

        val config = YamlConfiguration.loadConfiguration(file) ?: return

        for (key in config.getKeys(false)) {
            val particleStr = config.getString("$key.particle", "HAPPY_VILLAGER") ?: "HAPPY_VILLAGER"
            val chance = config.getDouble("$key.encounter_chance", 10.0)
            val minLevel = config.getInt("$key.levels.min", 1)
            val maxLevel = config.getInt("$key.levels.max", 5)

            val spawnSection = config.getConfigurationSection("$key.spawns")
            val spawns = mutableMapOf<String, Int>()
            if (spawnSection != null) {
                for (species in spawnSection.getKeys(false)) {
                    spawns[species] = spawnSection.getInt(species, 10)
                }
            }

            var validParticle = Particle.HAPPY_VILLAGER
            try {
                val parsedParticle = Particle.valueOf(particleStr.uppercase())
                if (parsedParticle.dataType == Void::class.java) {
                    validParticle = parsedParticle
                } else {
                    plugin.logger.warning("Partícula $particleStr en $key requiere datos extra. Usando HAPPY_VILLAGER.")
                }
            } catch (e: Exception) {
                plugin.logger.warning("Partícula $particleStr inválida. Usando HAPPY_VILLAGER.")
            }

            regions[key] = RegionTemplate(key, validParticle, chance, minLevel, maxLevel, spawns)
        }
        plugin.logger.info("Regiones cargadas: ${regions.size}")
    }

    fun getRegion(regionId: String): RegionTemplate? = regions[regionId]

    private fun rollEncounter(region: RegionTemplate): String? {
        if (region.spawns.isEmpty() || region.totalWeight <= 0) return null
        var randomValue = Random.nextInt(region.totalWeight)
        for ((speciesId, weight) in region.spawns) {
            randomValue -= weight
            if (randomValue < 0) return speciesId
        }
        return null
    }

    fun createWildEncounter(player: Player, region: RegionTemplate) {
        val speciesId = rollEncounter(region) ?: return
        val species = plugin.speciesManager.getSpecies(speciesId) ?: return

        val level = Random.nextInt(region.minLevel, region.maxLevel + 1)

        val wildDuelist = Duelist(
            uuid = UUID.randomUUID(),
            name = "Salvaje",
            isWild = true
        )

        val scaleFactor = 1.0 + (level * 0.05)
        val stats = CombatStats(
            hp = species.baseStats.hp * scaleFactor,
            maxHp = species.baseStats.hp * scaleFactor,
            attack = (species.baseStats.attack * scaleFactor).toInt(),
            defense = (species.baseStats.defense * scaleFactor).toInt(),
            speed = (species.baseStats.speed * scaleFactor).toInt(),
            accuracy = species.baseStats.accuracy,
            criticalChance = species.baseStats.criticalChance
        )

        val availableMoves = mutableListOf<String>()
        for ((learnLevel, moves) in species.learnset) {
            if (level >= learnLevel) availableMoves.addAll(moves)
        }
        val finalMoves = availableMoves.takeLast(4).toMutableList()

        val wildCompanion = Companion(
            id = -1,
            ownerUuid = wildDuelist.uuid,
            speciesId = speciesId,
            nickname = species.displayName,
            level = level,
            xp = 0.0,
            stats = stats,
            moves = finalMoves
        )

        wildDuelist.team.add(wildCompanion)

        val playerDuelist = plugin.duelistManager.getDuelist(player.uniqueId) ?: return
        plugin.combatManager.startDuel(playerDuelist, wildDuelist, player.location)
    }
}