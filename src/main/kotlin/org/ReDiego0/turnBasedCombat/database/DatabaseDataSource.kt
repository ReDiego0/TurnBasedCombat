package org.ReDiego0.turnBasedCombat.database

import java.sql.Connection
import java.sql.SQLException

interface DatabaseDataSource {

    @Throws(SQLException::class)
    fun connect()

    fun close()

    @Throws(SQLException::class)
    fun getConnection(): Connection

    fun initTables()
}