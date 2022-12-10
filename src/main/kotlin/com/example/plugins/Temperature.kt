package com.example.plugins
import org.jetbrains.exposed.sql.*

data class Temperature (val host:String, val timestamp: Long, val temp: Long)


object Temperatures : Table() {
    val id = integer("id").autoIncrement()
    val host = varchar("host", 128)
    val timestamp = long("timestamp")
    val temp = long("temp")

    override val primaryKey = PrimaryKey(id)
}