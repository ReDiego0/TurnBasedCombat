package org.ReDiego0.turnBasedCombat

import org.ReDiego0.turnBasedCombat.command.DuelCommand
import org.ReDiego0.turnBasedCombat.database.SQLManager
import org.ReDiego0.turnBasedCombat.listener.BagListener
import org.ReDiego0.turnBasedCombat.listener.CombatInteractionListener
import org.ReDiego0.turnBasedCombat.listener.CombatInventoryListener
import org.ReDiego0.turnBasedCombat.listener.ConnectionListener
import org.ReDiego0.turnBasedCombat.listener.MenuListener
import org.ReDiego0.turnBasedCombat.manager.ArenaManager
import org.ReDiego0.turnBasedCombat.manager.CombatManager
import org.ReDiego0.turnBasedCombat.manager.DuelistManager
import org.ReDiego0.turnBasedCombat.manager.ElementManager
import org.ReDiego0.turnBasedCombat.manager.ExperienceManager
import org.ReDiego0.turnBasedCombat.manager.ItemManager
import org.ReDiego0.turnBasedCombat.manager.NpcManager
import org.ReDiego0.turnBasedCombat.manager.SpeciesManager
import org.ReDiego0.turnBasedCombat.manager.TechniqueManager
import org.bukkit.plugin.java.JavaPlugin

class TurnBasedCombat : JavaPlugin() {

    companion object {
        lateinit var instance: TurnBasedCombat
            private set
    }

    lateinit var database: SQLManager
    lateinit var duelistManager: DuelistManager
    lateinit var combatManager: CombatManager
    lateinit var techniqueManager: TechniqueManager
    lateinit var elementManager: ElementManager
    lateinit var speciesManager: SpeciesManager
    lateinit var arenaManager: ArenaManager
    lateinit var npcManager: NpcManager
    lateinit var experienceManager: ExperienceManager
    lateinit var itemManager: ItemManager

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

        duelistManager = DuelistManager(this)
        combatManager = CombatManager(this)
        techniqueManager = TechniqueManager(this)
        speciesManager = SpeciesManager(this)
        elementManager = ElementManager(this)
        arenaManager = ArenaManager(this)
        npcManager = NpcManager(this)
        experienceManager = ExperienceManager(this)
        itemManager = ItemManager(this)

        server.pluginManager.registerEvents(ConnectionListener(duelistManager), this)
        server.pluginManager.registerEvents(CombatInteractionListener(this, combatManager, duelistManager), this)
        server.pluginManager.registerEvents(MenuListener(this), this)
        server.pluginManager.registerEvents(BagListener(this), this)
        server.pluginManager.registerEvents(CombatInventoryListener(this), this)

        getCommand("tbc")?.setExecutor(DuelCommand(this))

        logger.info("TurnBasedCombat habilitado correctamente ٩(◕‿◕)۶")
    }

    override fun onDisable() {
        if (::database.isInitialized) {
            database.close()
        }
        logger.info("TurnBasedCombat deshabilitado")
    }
}