package org.ReDiego0.turnBasedCombat.model

import java.util.UUID

enum class MailType {
    INFO,
    REWARD,
    TECHNIQUE
}

data class MailMessage(
    val id: UUID = UUID.randomUUID(),
    val type: MailType,
    val title: String,
    val body: String,

    val attachedItems: MutableMap<String, Int>? = null,

    val targetCompanionId: Int? = null,
    val techniqueId: String? = null,

    var isRead: Boolean = false,
    var isClaimed: Boolean = false
) {
    fun isDeletable(): Boolean {
        if (!isRead) return false
        return when (type) {
            MailType.INFO -> true
            MailType.REWARD -> isClaimed
            MailType.TECHNIQUE -> isClaimed
        }
    }
}