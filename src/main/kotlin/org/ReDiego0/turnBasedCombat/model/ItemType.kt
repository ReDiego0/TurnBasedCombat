package org.ReDiego0.turnBasedCombat.model

enum class ItemType {
    HEAL_HP,
    HEAL_TEAM,
    CURE_STATUS,
    FULL_RESTORE,
    LEVEL_UP,
    TEACH_TECHNIQUE,
    CAPTURE_DEVICE
}

data class ItemTemplate(
    val id: String,
    val displayName: String,
    val type: ItemType,
    val power: Double = 0.0,
    val techniqueId: String? = null
)