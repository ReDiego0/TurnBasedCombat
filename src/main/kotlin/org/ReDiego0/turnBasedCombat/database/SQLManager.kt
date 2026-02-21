package org.ReDiego0.turnBasedCombat.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.ReDiego0.turnBasedCombat.TurnBasedCombat
import java.io.File
import java.sql.Connection

class SQLManager(private val plugin: TurnBasedCombat) : DatabaseDataSource {

    private var dataSource: HikariDataSource? = null

    override fun connect() {
        val type = plugin.config.getString("database.type", "sqlite")?.lowercase()
        val config = HikariConfig()

        if (type == "mysql") {
            val host = plugin.config.getString("database.host")
            val port = plugin.config.getString("database.port")
            val db = plugin.config.getString("database.database")
            val user = plugin.config.getString("database.username")
            val pass = plugin.config.getString("database.password")

            config.jdbcUrl = "jdbc:mysql://$host:$port/$db?useSSL=false"
            config.username = user
            config.password = pass
            plugin.logger.info("Conectando a MySQL... :3")
        } else {
            val file = File(plugin.dataFolder, "turnbasedcombat.db")
            if (!file.parentFile.exists()) file.parentFile.mkdirs()

            config.jdbcUrl = "jdbc:sqlite:${file.absolutePath}"
            config.driverClassName = "org.sqlite.JDBC"
            plugin.logger.info("Usando SQLite local (UwU)")
        }

        config.maximumPoolSize = 10
        config.connectionTimeout = 30000
        config.leakDetectionThreshold = 60000

        this.dataSource = HikariDataSource(config)
        initTables()
    }

    override fun getConnection(): Connection {
        return dataSource?.connection ?: throw IllegalStateException("Database not initialized!")
    }

    override fun initTables() {
        getConnection().use { conn ->
            val statement = conn.createStatement()

            // Tabla de Duelistas (Jugadores)
            statement.execute("""
                CREATE TABLE IF NOT EXISTS tbc_duelists (
                    uuid VARCHAR(36) PRIMARY KEY,
                    currency INT DEFAULT 0,
                    active_team_id INT DEFAULT 0,
                    wins INT DEFAULT 0,
                    losses INT DEFAULT 0
                );
            """.trimIndent())

            // Tabla de Companions (Los Mobs capturados)
            // Nota: se guarda stats y moves como JSON string para flexibilidad
            statement.execute("""
                CREATE TABLE IF NOT EXISTS tbc_companions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    owner_uuid VARCHAR(36) NOT NULL,
                    species_id VARCHAR(255) NOT NULL,
                    nickname VARCHAR(255) NOT NULL,
                    level INTEGER NOT NULL,
                    xp DOUBLE NOT NULL,
                    current_hp DOUBLE NOT NULL,
                    stats_json TEXT NOT NULL,
                    moves_json TEXT NOT NULL,
                    move_pp_json TEXT,
                    active_status_json TEXT,
                    is_in_team BOOLEAN NOT NULL,
                    held_item_id VARCHAR(255)
                );
            """.trimIndent())

            plugin.logger.info("Tablas verificadas correctamente ᕙ(`▿´)ᕗ")
        }
    }

    override fun close() {
        dataSource?.close()
    }
}