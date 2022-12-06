package com.example.plugins

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class TempDatabase {
    var database:Database? = null

    fun add(temperature: Temperature){
        transaction(database) {
            // Statements here
            Temperatures.insert {
                it[host] = temperature.host
                it[timestamp] = temperature.timestamp
                it[temp] = temperature.temp
            }
        }

    }

    fun getAll(): List<Temperature>{
        var res: List<Temperature> = emptyList()
        transaction(database) {
            res = Temperatures.selectAll().map { it.toTemp() }
        }
        return res
    }

    fun init() {
        val driverClassName = "org.h2.Driver"
        val jdbcURL = "jdbc:h2:file:./temperatures"
        database = Database.connect(jdbcURL, driverClassName)
        transaction(database) {
            SchemaUtils.create(Temperatures)
        }

    }


}

private fun ResultRow.toTemp(): Temperature {
    return Temperature(
        host = this[Temperatures.host],
        timestamp = this[Temperatures.timestamp],
        temp = this[Temperatures.temp],
    )

}
