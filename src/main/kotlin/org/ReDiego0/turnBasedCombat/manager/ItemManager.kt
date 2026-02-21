package org.ReDiego0.turnBasedCombat.manager

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.ReDiego0.turnBasedCombat.model.ItemTemplate
import org.ReDiego0.turnBasedCombat.model.ItemType
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File

class ItemManager(private val plugin: TurnBasedCombat) {

    private val items = mutableMapOf<String, ItemTemplate>()

    init {
        loadItems()
    }

    fun loadItems() {
        items.clear()
        val file = File(plugin.dataFolder, "items.yml")
        if (!file.exists()) plugin.saveResource("items.yml", false)

        val config = YamlConfiguration.loadConfiguration(file)
        for (key in config.getKeys(false)) {
            val name = config.getString("$key.displayName") ?: key
            val typeStr = config.getString("$key.type") ?: "HEAL_HP"
            val type = runCatching { ItemType.valueOf(typeStr) }.getOrDefault(ItemType.HEAL_HP)
            val power = config.getDouble("$key.power", 0.0)
            val techId = config.getString("$key.techniqueId")

            items[key] = ItemTemplate(key, name, type, power, techId)
        }
        plugin.logger.info("Ítems cargados: ${items.size}")
    }

    fun getItem(id: String): ItemTemplate? = items[id]

    fun useItem(duelist: Duelist, itemId: String, targetIndex: Int, player: Player): Boolean {
        val item = getItem(itemId) ?: return false
        val companion = duelist.team.getOrNull(targetIndex) ?: return false

        val species = plugin.speciesManager.getSpecies(companion.speciesId)
        if (species != null) {
            for ((targetId, req) in species.evolutions) {
                if (req.item == itemId) {
                    if (req.level > 0 && companion.level < req.level) {
                        player.sendMessage(Component.text("¡${companion.nickname} necesita ser nivel ${req.level} para usar esto!").color(NamedTextColor.RED))
                        return false
                    }
                    plugin.experienceManager.evolveCompanion(companion, targetId, player)
                    consumeItem(duelist, itemId)
                    return true
                }
            }
        }

        when (item.type) {
            ItemType.HEAL_HP -> {
                if (companion.stats.hp >= companion.stats.maxHp) {
                    player.sendMessage(Component.text("¡${companion.nickname} ya tiene la vida al máximo!").color(NamedTextColor.RED))
                    return false
                }
                companion.stats.hp += item.power
                if (companion.stats.hp > companion.stats.maxHp) companion.stats.hp = companion.stats.maxHp
                player.sendMessage(Component.text("Has curado a ${companion.nickname} con ${item.displayName}.").color(NamedTextColor.GREEN))
            }
            ItemType.HEAL_TEAM -> {
                duelist.team.forEach { it.stats.hp = it.stats.maxHp; it.activeStatus = null }
                player.sendMessage(Component.text("¡El equipo entero ha sido curado!").color(NamedTextColor.GREEN))
            }
            ItemType.LEVEL_UP -> {
                plugin.experienceManager.awardXp(companion, companion, null)
                player.sendMessage(Component.text("¡${companion.nickname} comió ${item.displayName}!").color(NamedTextColor.GOLD))
            }
            ItemType.TEACH_TECHNIQUE -> {
                val tech = item.techniqueId ?: return false
                if (companion.moves.contains(tech)) {
                    player.sendMessage(Component.text("¡${companion.nickname} ya conoce esta técnica!").color(NamedTextColor.RED))
                    return false
                }
                if (companion.moves.size >= 4) {
                    player.sendMessage(Component.text("¡${companion.nickname} ya conoce 4 movimientos! Olvida uno primero.").color(NamedTextColor.RED))
                    return false
                }
                companion.moves.add(tech)
                player.sendMessage(Component.text("¡${companion.nickname} aprendió una nueva técnica!").color(NamedTextColor.AQUA))
            }
            else -> {
                player.sendMessage(Component.text("Este ítem aún no tiene efecto programado.").color(NamedTextColor.GRAY))
                return false
            }
        }

        consumeItem(duelist, itemId)
        return true
    }

    private fun consumeItem(duelist: Duelist, itemId: String) {
        val count = duelist.bag[itemId] ?: 0
        if (count > 1) duelist.bag[itemId] = count - 1
        else duelist.bag.remove(itemId)
    }
}