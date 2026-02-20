package org.ReDiego0.turnBasedCombat.placeholder

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.bukkit.OfflinePlayer
import kotlin.math.pow

class TBCPlaceholderExpansion(private val plugin: TurnBasedCombat) : PlaceholderExpansion() {

    override fun getIdentifier(): String = "tbc"
    override fun getAuthor(): String = "TuNombre"
    override fun getVersion(): String = "1.0.0"
    override fun persist(): Boolean = true

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        if (player == null) return ""

        val duelist = plugin.duelistManager.getDuelist(player.uniqueId) ?: return "N/A"
        val activeCompanion = duelist.team.firstOrNull { !it.isFainted() }

        return when (params.lowercase()) {
            "currency" -> duelist.currency.toString()
            "wins" -> duelist.wins.toString()
            "losses" -> duelist.losses.toString()
            "winrate" -> {
                val total = duelist.wins + duelist.losses
                if (total == 0) "0%" else "${((duelist.wins.toDouble() / total) * 100).toInt()}%"
            }
            "team_size" -> duelist.team.size.toString()
            "team_max" -> "6"
            "pc_size" -> duelist.pcStorage.size.toString()

            "active_name" -> activeCompanion?.nickname ?: "Ninguno"
            "active_species" -> activeCompanion?.let { plugin.speciesManager.getSpecies(it.speciesId)?.displayName } ?: "N/A"
            "active_level" -> activeCompanion?.level?.toString() ?: "0"
            "active_hp" -> activeCompanion?.stats?.hp?.toInt()?.toString() ?: "0"
            "active_maxhp" -> activeCompanion?.stats?.maxHp?.toInt()?.toString() ?: "0"
            "active_xp" -> activeCompanion?.xp?.toInt()?.toString() ?: "0"
            "active_xp_needed" -> activeCompanion?.let { ((it.level + 1).toDouble().pow(3)).toInt().toString() } ?: "0"
            "active_attack" -> activeCompanion?.stats?.attack?.toString() ?: "0"
            "active_defense" -> activeCompanion?.stats?.defense?.toString() ?: "0"
            "active_speed" -> activeCompanion?.stats?.speed?.toString() ?: "0"

            else -> null
        }
    }
}