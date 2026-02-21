package org.ReDiego0.turnBasedCombat.manager

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.model.Companion
import org.bukkit.entity.Player
import kotlin.math.pow

class ExperienceManager(private val plugin: TurnBasedCombat) {

    fun awardXp(winner: Companion, defeated: Companion, player: Player?) {
        val xpGain = ((50.0 * defeated.level) / 7.0)
        winner.xp += xpGain

        player?.sendMessage(Component.text("¡${winner.nickname} ha ganado ${xpGain.toInt()} puntos de EXP!").color(NamedTextColor.GREEN))

        while (winner.xp >= getXpRequiredForLevel(winner.level + 1)) {
            levelUp(winner, player)
        }
    }

    private fun getXpRequiredForLevel(level: Int): Double {
        return level.toDouble().pow(3)
    }

    private fun levelUp(companion: Companion, player: Player?) {
        companion.level += 1

        val species = plugin.speciesManager.getSpecies(companion.speciesId) ?: return

        val scaleFactor = 1.0 + (companion.level * 0.05)

        companion.stats.attack = (species.baseStats.attack * scaleFactor).toInt()
        companion.stats.defense = (species.baseStats.defense * scaleFactor).toInt()
        companion.stats.speed = (species.baseStats.speed * scaleFactor).toInt()

        val oldHp = companion.stats.hp
        companion.stats.maxHp = species.baseStats.hp * scaleFactor

        val hpDifference = companion.stats.maxHp - species.baseStats.hp * (1.0 + ((companion.level - 1) * 0.05))
        if (hpDifference > 0) {
            companion.stats.hp += hpDifference
        }

        player?.sendMessage(Component.text("¡${companion.nickname} ha subido al nivel ${companion.level}!").color(NamedTextColor.GOLD))

        val newMoves = species.learnset[companion.level]
        if (newMoves != null) {
            for (moveId in newMoves) {
                if (companion.moves.size < 4 && !companion.moves.contains(moveId)) {
                    companion.moves.add(moveId)
                    val techniqueName = plugin.techniqueManager.getTechnique(moveId)?.displayName ?: moveId
                    player?.sendMessage(Component.text("¡${companion.nickname} ha aprendido $techniqueName!").color(NamedTextColor.AQUA))
                } else if (!companion.moves.contains(moveId)) {
                    val techniqueName = plugin.techniqueManager.getTechnique(moveId)?.displayName ?: moveId
                    player?.sendMessage(Component.text("${companion.nickname} quiere aprender $techniqueName. ¡Revisa tu buzón!").color(NamedTextColor.YELLOW))

                    if (player != null) {
                        val duelist = plugin.duelistManager.getDuelist(player.uniqueId)
                        if (duelist != null) {
                            val mail = org.ReDiego0.turnBasedCombat.model.MailMessage(
                                type = org.ReDiego0.turnBasedCombat.model.MailType.TECHNIQUE,
                                title = "${companion.nickname} quiere aprender $techniqueName",
                                body = "¿Quieres que ${companion.nickname} olvide un movimiento antiguo para aprender $techniqueName?",
                                targetCompanionId = companion.id,
                                techniqueId = moveId
                            )
                            duelist.mailbox.add(mail)
                        }
                    }
                }
            }
        }

        species.evolutions.forEach { (targetId, req) ->
            if (req.item == null && req.level > 0 && companion.level >= req.level) {
                evolveCompanion(companion, targetId, player)
            }
        }
    }

    fun evolveCompanion(companion: Companion, targetSpeciesId: String, player: Player?) {
        val targetSpecies = plugin.speciesManager.getSpecies(targetSpeciesId) ?: return
        val oldSpecies = plugin.speciesManager.getSpecies(companion.speciesId)

        if (companion.nickname == oldSpecies?.displayName) {
            companion.nickname = targetSpecies.displayName
        }

        companion.speciesId = targetSpeciesId

        val scaleFactor = 1.0 + (companion.level * 0.05)

        val hpPercentage = if (companion.stats.maxHp > 0) companion.stats.hp / companion.stats.maxHp else 1.0
        companion.stats.maxHp = targetSpecies.baseStats.hp * scaleFactor
        companion.stats.hp = companion.stats.maxHp * hpPercentage

        companion.stats.attack = (targetSpecies.baseStats.attack * scaleFactor).toInt()
        companion.stats.defense = (targetSpecies.baseStats.defense * scaleFactor).toInt()
        companion.stats.speed = (targetSpecies.baseStats.speed * scaleFactor).toInt()

        player?.sendMessage(Component.text("¡Qué es esto! ¡Tu Companion ha evolucionado a ${targetSpecies.displayName}!").color(NamedTextColor.LIGHT_PURPLE))
    }
}