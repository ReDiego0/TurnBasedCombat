package org.ReDiego0.turnBasedCombat.manager

import com.google.gson.Gson
import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.bukkit.Bukkit
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture

class DuelistManager(private val plugin: TurnBasedCombat) {

    private val duelists = ConcurrentHashMap<UUID, Duelist>()
    private val gson = Gson()

    fun getDuelist(uuid: UUID): Duelist? = duelists[uuid]

    fun loadDuelist(uuid: UUID, name: String): CompletableFuture<Duelist> {
        return CompletableFuture.supplyAsync {
            val loadedDuelist = Duelist(uuid, name)
            if (isNewPlayer(uuid)) {
                null
            }

            return@supplyAsync loadedDuelist
        }.thenApply { duelist ->
            duelists[uuid] = duelist
            plugin.logger.info("Datos cargados para ${duelist.name} (UwU)")
            duelist
        }
    }

    fun saveAndRemove(uuid: UUID) {
        val duelist = duelists.remove(uuid) ?: return
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            saveToDatabase(duelist)
        })
    }

    private fun saveToDatabase(duelist: Duelist) {
        plugin.logger.info("Datos guardados para ${duelist.name} ᕙ(`▿´)ᕗ")
    }

    private fun isNewPlayer(uuid: UUID): Boolean {
        return true // Placeholder
    }
}