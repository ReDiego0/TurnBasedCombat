package org.ReDiego0.turnBasedCombat.manager

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import org.ReDiego0.turnBasedCombat.model.ActiveStatus
import org.ReDiego0.turnBasedCombat.model.CombatStats
import org.ReDiego0.turnBasedCombat.model.Companion
import org.ReDiego0.turnBasedCombat.model.Duelist
import org.ReDiego0.turnBasedCombat.model.MailMessage
import org.bukkit.Bukkit
import java.sql.SQLException
import java.sql.Statement
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
                        // val wins = rs.getInt("wins")
                        // val losses = rs.getInt("losses")
                        val mailboxJson = rs.getString("mailbox_json")

                        duelist = Duelist(uuid, name).apply {
                            this.currency = currency
                            if (mailboxJson != null) {
                                val mailType = object : TypeToken<MutableList<MailMessage>>() {}.type
                                val mails: MutableList<MailMessage> = gson.fromJson(mailboxJson, mailType)
                                this.mailbox.addAll(mails)
                            }
                        }
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
                val movePpJson = rs.getString("move_pp_json")
                val activeStatusJson = rs.getString("active_status_json")

                val stats = gson.fromJson(statsJson, CombatStats::class.java)

                val movesType = object : TypeToken<MutableList<String>>() {}.type
                val moves: MutableList<String> = gson.fromJson(movesJson, movesType)

                val movePpType = object : TypeToken<MutableMap<String, Int>>() {}.type
                val movePP: MutableMap<String, Int> = if (movePpJson != null) gson.fromJson(movePpJson, movePpType) else mutableMapOf()
                val activeStatus: ActiveStatus? = if (activeStatusJson != null) gson.fromJson(activeStatusJson, ActiveStatus::class.java) else null

                val companion = Companion(
                    id = rs.getInt("id"),
                    ownerUuid = uuid,
                    speciesId = rs.getString("species_id"),
                    nickname = rs.getString("nickname"),
                    level = rs.getInt("level"),
                    xp = rs.getDouble("xp"),
                    stats = stats,
                    moves = moves,
                    movePP = movePP,
                    activeStatus = activeStatus,
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
                        "UPDATE tbc_duelists SET currency = ?, mailbox_json = ? WHERE uuid = ?"
                    )
                    stmt.setInt(1, duelist.currency)
                    stmt.setString(2, gson.toJson(duelist.mailbox))
                    stmt.setString(3, uuid.toString())
                    stmt.executeUpdate()

                    saveCompanionList(duelist.team, true, conn, uuid)
                    saveCompanionList(duelist.pcStorage, false, conn, uuid)
                }
            } catch (e: SQLException) {
                plugin.logger.severe("Error guardando datos de ${duelist.name}: ${e.message}")
            }

            duelists.remove(uuid)
        })
    }

    private fun saveCompanionList(list: List<Companion>, inTeam: Boolean, conn: java.sql.Connection, ownerUuid: UUID) {
        val updateSql = """
            UPDATE tbc_companions SET 
            nickname = ?, level = ?, xp = ?, current_hp = ?, 
            stats_json = ?, moves_json = ?, is_in_team = ?, held_item_id = ?,
            move_pp_json = ?, active_status_json = ?
            WHERE id = ?
        """

        val insertSql = """
            INSERT INTO tbc_companions 
            (owner_uuid, species_id, nickname, level, xp, current_hp, stats_json, moves_json, is_in_team, held_item_id, move_pp_json, active_status_json) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """

        val updateStmt = conn.prepareStatement(updateSql)
        val insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)

        for (comp in list) {
            val statsJson = gson.toJson(comp.stats)
            val movesJson = gson.toJson(comp.moves)
            val movePpJson = gson.toJson(comp.movePP)
            val statusJson = if (comp.activeStatus != null) gson.toJson(comp.activeStatus) else null

            if (comp.id <= 0) {
                insertStmt.setString(1, ownerUuid.toString())
                insertStmt.setString(2, comp.speciesId)
                insertStmt.setString(3, comp.nickname ?: comp.speciesId)
                insertStmt.setInt(4, comp.level)
                insertStmt.setDouble(5, comp.xp)
                insertStmt.setDouble(6, comp.stats.hp)
                insertStmt.setString(7, statsJson)
                insertStmt.setString(8, movesJson)
                insertStmt.setBoolean(9, inTeam)
                insertStmt.setString(10, comp.heldItem)
                insertStmt.setString(11, movePpJson)
                insertStmt.setString(12, statusJson)
                insertStmt.executeUpdate()

                val generatedKeys = insertStmt.generatedKeys
                if (generatedKeys.next()) {
                    comp.id = generatedKeys.getInt(1)
                }
            } else {
                updateStmt.setString(1, comp.nickname)
                updateStmt.setInt(2, comp.level)
                updateStmt.setDouble(3, comp.xp)
                updateStmt.setDouble(4, comp.stats.hp)
                updateStmt.setString(5, statsJson)
                updateStmt.setString(6, movesJson)
                updateStmt.setBoolean(7, inTeam)
                updateStmt.setString(8, comp.heldItem)
                updateStmt.setString(9, movePpJson)
                updateStmt.setString(10, statusJson)
                updateStmt.setInt(11, comp.id)
                updateStmt.addBatch()
            }
        }
        updateStmt.executeBatch()
    }
}