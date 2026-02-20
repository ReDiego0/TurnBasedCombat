package org.ReDiego0.turnBasedCombat.manager

import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.model.AIType
import org.ReDiego0.turnBasedCombat.model.Companion
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.ReDiego0.turnBasedCombat.model.NpcCompanionSpec
import org.ReDiego0.turnBasedCombat.model.NpcTemplate
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID

class NpcManager(private val plugin: TurnBasedCombat) {

    private val npcTemplates = mutableMapOf<String, NpcTemplate>()

    init {
        loadNpcs()
    }

    fun loadNpcs() {
        npcTemplates.clear()
        val file = File(plugin.dataFolder, "npcs.yml")
        if (!file.exists()) {
            plugin.saveResource("npcs.yml", false)
        }

        val config = YamlConfiguration.loadConfiguration(file)
        val keys = config.getKeys(false)

        for (key in keys) {
            val displayName = config.getString("$key.displayName") ?: key
            val aiTypeStr = config.getString("$key.aiType")?.uppercase() ?: "RANDOM"

            val aiType = runCatching { AIType.valueOf(aiTypeStr) }
                .getOrDefault(AIType.RANDOM)

            val teamSpecs = mutableListOf<NpcCompanionSpec>()
            val teamSection = config.getConfigurationSection("$key.team")

            if (teamSection != null) {
                for (specKey in teamSection.getKeys(false)) {
                    val speciesId = teamSection.getString("$specKey.speciesId") ?: continue
                    val level = teamSection.getInt("$specKey.level", 1)
                    teamSpecs.add(NpcCompanionSpec(speciesId, level))
                }
            }

            npcTemplates[key] = NpcTemplate(key, displayName, aiType, teamSpecs)
        }

        plugin.logger.info("NPCs cargados: ${npcTemplates.size}")
    }

    fun getNpcTemplate(id: String): NpcTemplate? = npcTemplates[id]

    fun createNpcDuelist(npcId: String): Duelist? {
        val template = getNpcTemplate(npcId) ?: return null

        val npcUuid = UUID.randomUUID()
        val duelist = Duelist(npcUuid, template.displayName)

        for (spec in template.teamSpecs) {
            val species = plugin.speciesManager.getSpecies(spec.speciesId) ?: continue

            val companion = Companion(
                id = -1,
                ownerUuid = npcUuid,
                speciesId = spec.speciesId,
                nickname = species.displayName,
                level = spec.level,
                xp = 0.0,
                stats = species.baseStats.copy(),
                moves = getMovesForLevel(species.learnset, spec.level).toMutableList()
            )

            scaleStatsToLevel(companion)
            duelist.team.add(companion)
        }

        return if (duelist.team.isEmpty()) null else duelist
    }

    private fun getMovesForLevel(learnset: Map<Int, List<String>>, level: Int): List<String> {
        val availableMoves = mutableListOf<String>()
        for ((learnLevel, moves) in learnset) {
            if (level >= learnLevel) {
                availableMoves.addAll(moves)
            }
        }
        return availableMoves.takeLast(4)
    }

    private fun scaleStatsToLevel(companion: Companion) {
        val scaleFactor = 1.0 + (companion.level * 0.05)
        companion.stats.maxHp = companion.stats.hp * scaleFactor
        companion.stats.hp = companion.stats.maxHp
        companion.stats.attack = (companion.stats.attack * scaleFactor).toInt()
        companion.stats.defense = (companion.stats.defense * scaleFactor).toInt()
        companion.stats.speed = (companion.stats.speed * scaleFactor).toInt()
    }
}