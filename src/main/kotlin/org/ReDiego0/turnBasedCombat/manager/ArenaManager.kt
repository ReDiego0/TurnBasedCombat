package org.ReDiego0.turnBasedCombat.manager

import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.block.Biome
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class ArenaManager(private val plugin: TurnBasedCombat) {

    private val arenas = mutableMapOf<String, Location>()
    private val biomeMapping = mutableMapOf<Biome, String>()
    private var defaultArenaId: String = "default"

    init {
        loadArenas()
    }

    fun loadArenas() {
        arenas.clear()
        biomeMapping.clear()

        val file = File(plugin.dataFolder, "arenas.yml")
        if (!file.exists()) {
            plugin.saveResource("arenas.yml", false)
        }

        val config = YamlConfiguration.loadConfiguration(file)
        defaultArenaId = config.getString("default_arena") ?: "default"

        val arenasSection = config.getConfigurationSection("arenas") ?: return
        for (key in arenasSection.getKeys(false)) {
            val worldName = config.getString("arenas.$key.world") ?: continue
            val world = Bukkit.getWorld(worldName) ?: continue
            val x = config.getDouble("arenas.$key.x")
            val y = config.getDouble("arenas.$key.y")
            val z = config.getDouble("arenas.$key.z")
            val yaw = config.getDouble("arenas.$key.yaw").toFloat()
            val pitch = config.getDouble("arenas.$key.pitch").toFloat()

            arenas[key] = Location(world, x, y, z, yaw, pitch)

            val biomes = config.getStringList("arenas.$key.biomes")
            for (biomeStr in biomes) {
                runCatching {
                    val biome = Registry.BIOME.get(NamespacedKey.minecraft(biomeStr.lowercase()))
                    if (biome != null) {
                        biomeMapping[biome] = key
                    }
                }
            }
        }
        plugin.logger.info("Arenas cargadas: ${arenas.size}")
    }

    fun getArenaForLocation(location: Location): Location {
        val biome = location.block.biome
        val arenaId = biomeMapping[biome] ?: defaultArenaId
        return arenas[arenaId]?.clone() ?: location.clone().apply { pitch = 0f }
    }
}