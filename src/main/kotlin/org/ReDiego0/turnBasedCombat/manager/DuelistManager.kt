package org.ReDiego0.turnBasedCombat.manager

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.model.CombatStats
import org.ReDiego0.turnBasedCombat.model.Companion
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.bukkit.Bukkit
import java.sql.SQLException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture

class DuelistManager(private val plugin: TurnBasedCombat) {

    private val duelists = ConcurrentHashMap<UUID, Duelist>()
    private val gson = Gson()

    fun getDuelist(uuid: UUID): Duelist? = duelists[uuid]

    fun loadDuelistData(uuid: UUID, name: String): CompletableFuture<Duelist> {
        return CompletableFuture.supplyAsync {
            var duelist: Duelist? = null

            try {
                plugin.database.getConnection().use { conn ->
                    val stmt = conn.prepareStatement("SELECT * FROM tbc_duelists WHERE uuid = ?")
                    stmt.setString(1, uuid.toString())
                    val rs = stmt.executeQuery()

                    if (rs.next()) {
                        val currency = rs.getInt("currency")
                        val wins = rs.getInt("wins")
                        val losses = rs.getInt("losses")

                        duelist = Duelist(uuid, name, currency, mutableListOf(), mutableListOf())
                    } else {
                        val insert = conn.prepareStatement(
                            "INSERT INTO tbc_duelists (uuid, currency, active_team_id, wins, losses) VALUES (?, 0, 0, 0, 0)"
                        )
                        insert.setString(1, uuid.toString())
                        insert.executeUpdate()

                        duelist = Duelist(uuid, name)
                        plugin.logger.info("Nuevo Duelista registrado en DB: $name")
                    }

                    loadCompanionsForDuelist(uuid, duelist!!)
                }
            } catch (e: SQLException) {
                plugin.logger.severe("Error SQL cargando duelista $name: ${e.message}")
                e.printStackTrace()
            }

            return@supplyAsync duelist ?: Duelist(uuid, name)
        }.thenApply { loadedDuelist ->
            duelists[uuid] = loadedDuelist
            loadedDuelist
        }
    }


    private fun loadCompanionsForDuelist(uuid: UUID, duelist: Duelist) {
        plugin.database.getConnection().use { conn ->
            val stmt = conn.prepareStatement("SELECT * FROM tbc_companions WHERE owner_uuid = ?")
            stmt.setString(1, uuid.toString())
            val rs = stmt.executeQuery()

            while (rs.next()) {
                val statsJson = rs.getString("stats_json")
                val movesJson = rs.getString("moves_json")

                val stats = gson.fromJson(statsJson, CombatStats::class.java)
                val movesType = object : TypeToken<MutableList<String>>() {}.type
                val moves: MutableList<String> = gson.fromJson(movesJson, movesType)

                val companion = Companion(
                    id = rs.getInt("id"),
                    ownerUuid = uuid,
                    speciesId = rs.getString("species_id"),
                    nickname = rs.getString("nickname"),
                    level = rs.getInt("level"),
                    xp = rs.getDouble("xp"),
                    stats = stats,
                    moves = moves,
                    heldItem = rs.getString("held_item_id")
                )

                if (rs.getBoolean("is_in_team")) {
                    duelist.team.add(companion)
                } else {
                    duelist.pcStorage.add(companion)
                }
            }
        }
    }

    fun saveDuelistData(uuid: UUID) {
        val duelist = duelists[uuid] ?: return

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            try {
                plugin.database.getConnection().use { conn ->
                    val stmt = conn.prepareStatement(
                        "UPDATE tbc_duelists SET currency = ? WHERE uuid = ?"
                    )
                    stmt.setInt(1, duelist.currency)
                    stmt.setString(2, uuid.toString())
                    stmt.executeUpdate()
                    saveCompanionList(duelist.team, true, conn)
                    saveCompanionList(duelist.pcStorage, false, conn)
                }
            } catch (e: SQLException) {
                plugin.logger.severe("Error guardando datos de ${duelist.name}: ${e.message}")
            }

            duelists.remove(uuid)
        })
    }

    private fun saveCompanionList(list: List<Companion>, inTeam: Boolean, conn: java.sql.Connection) {
        val sql = """
            UPDATE tbc_companions SET 
            nickname = ?, level = ?, xp = ?, current_hp = ?, 
            stats_json = ?, moves_json = ?, is_in_team = ?, held_item_id = ?
            WHERE id = ?
        """
        val stmt = conn.prepareStatement(sql)

        for (comp in list) {
            stmt.setString(1, comp.nickname)
            stmt.setInt(2, comp.level)
            stmt.setDouble(3, comp.xp)
            stmt.setDouble(4, comp.stats.hp) // Guardamos HP actual
            stmt.setString(5, gson.toJson(comp.stats))
            stmt.setString(6, gson.toJson(comp.moves))
            stmt.setBoolean(7, inTeam)
            stmt.setString(8, comp.heldItem)
            stmt.setInt(9, comp.id)
            stmt.addBatch()
        }
        stmt.executeBatch()
    }
}