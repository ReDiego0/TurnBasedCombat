package org.ReDiego0.turnBasedCombat

import org.ReDiego0.turnBasedCombat.database.SQLManager
import org.bukkit.plugin.java.JavaPlugin

class TurnBasedCombat : JavaPlugin() {

    companion object {
        lateinit var instance: TurnBasedCombat
            private set
    }

    lateinit var database: SQLManager

    override fun onEnable() {
        instance = this
        saveDefaultConfig()

        try {
            database = SQLManager(this)
            database.connect()
        } catch (e: Exception) {
            logger.severe("¡Error crítico! No se pudo conectar a la base de datos (T_T)")
            e.printStackTrace()
            server.pluginManager.disablePlugin(this)
            return
        }

        logger.info("TurnBasedCombat habilitado correctamente ٩(◕‿◕)۶")
    }

    override fun onDisable() {
        if (::database.isInitialized) {
            database.close()
        }
        logger.info("TurnBasedCombat deshabilitado")
    }
}
