package org.ReDiego0.turnBasedCombat.model

data class CombatStats(
    var hp: Double,
    var maxHp: Double,
    var attack: Int,
    var defense: Int,
    var speed: Int,
    var accuracy: Int,
    var criticalChance: Double
)