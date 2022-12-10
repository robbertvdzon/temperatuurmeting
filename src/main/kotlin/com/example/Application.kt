package com.example

import com.example.plugins.TempDatabase
import com.example.plugins.Temperature
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jfree.chart.ChartFactory
import org.jfree.chart.JFreeChart
import org.jfree.data.time.Millisecond
import org.jfree.data.time.TimeSeries
import org.jfree.data.time.TimeSeriesCollection
import org.jfree.svg.SVGGraphics2D
import org.jfree.svg.SVGUtils
import java.awt.Rectangle
import java.io.File
import java.lang.Math.round
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

private const val AANBOUW = "28:cd:c1:02:1e:22" // nr 1
private const val HUISKAMER = "28:cd:c1:02:1e:2a" // nr 2
private const val SCHUUR = "28:cd:c1:02:1e:24" // nr 3
private const val BIJKEUKEN = "28:cd:c1:02:1e:28" // nr 4
private const val EETKAMER = "28:cd:c1:02:1e:2c" // nr 5
private const val CV = "28:cd:c1:02:1e:26" // nr 6

private val rooms: Map<String, String> = mapOf(
    AANBOUW to "AANBOUW (1)",
    HUISKAMER to "HUISKAMER (2)",
    SCHUUR to "SCHUUR (3)",
    BIJKEUKEN to "BIJKEUKEN (4)",
    EETKAMER to "EETKAMER (5)",
    CV to "CV (6)"
)

private val correctionsPerRoom: Map<String, Long> = mapOf(
    AANBOUW to 0,
    HUISKAMER to 122,
    SCHUUR to -36,
    BIJKEUKEN to 14,
    EETKAMER to 157,
    CV to -9
)

val database = TempDatabase()
val lastAdd: MutableMap<String, Temperature> = mutableMapOf()

fun getTimestamp(timestamp: Long): String {
    return LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyy-MM-dd HH:mm:ss"))
}

fun toTemp(temp: Long): Double {
    //21 degrees = 14053
    //1 degree = 21.6
    val diffFrom21Degrees = 14053.0 - temp
    val diffDegrees = diffFrom21Degrees / 21.6
    return 21.0 + diffDegrees
}

fun main(args: Array<String>) {
    database.init()
    embeddedServer(Netty, port = 8000) {

        routing {
            get("/") {
                call.respondText(getHtml(), ContentType.Text.Html)
            }
            get("/db") {
                call.respondText(database.getAll().toString())
            }
            get("/last") {
                val list = rooms.keys
                    .map { key ->
                        val lastAdded = lastAdd.get(key).correctTempertature() ?: Temperature("?", 0, 0)
                        val name = rooms.get(key)!!
                        "${name.padEnd(15)}: ${getTimestamp(lastAdded.timestamp)} : ${toTemp(lastAdded.temp).toString().take(6)}"
                    }
                call.respondText(list.joinToString(separator = "\n"))
            }
            get("/average") {
                val list = rooms.keys
                    .map { key ->
                        val name = rooms.get(key)!!
                        val allTemps = database.getAll().filter { it.host == key }.map { it.temp }
                        val average = round(allTemps.average())
                        val min = allTemps.min()
                        val max = allTemps.max()
                        val samples = allTemps.size
                        "${name.padEnd(15)}: $samples samples, average: $average, min: $min, max: $max "
                    }
                call.respondText(list.joinToString(separator = "\n"))
            }
            get("/diff") {
                val baseDate = LocalDateTime.of(2022, 12, 10, 12, 0, 0)

                val list = rooms.keys
                    .map { key ->
                        val name = rooms.get(key)!!
                        print(" $name :")
                        val diffs: MutableList<Long> = mutableListOf()
                        for (i in 0L..14) {
                            val startDate = baseDate.plusHours(i).toEpochSecond(ZoneOffset.UTC)
                            val endDate = baseDate.plusHours(i + 1).toEpochSecond(ZoneOffset.UTC)

                            val allTempsRef = database.getAll().filter { it.timestamp > startDate && it.timestamp < endDate && it.host == AANBOUW }.map { it.temp }
                            val averageRef = round(allTempsRef.average())

                            val allTemps = database.getAll().filter { it.timestamp > startDate && it.timestamp < endDate && it.host == key }.map { it.temp }
                            val average = round(allTemps.average())
                            val diff = averageRef - average
                            diffs.add(diff)
                            print(" $diff")
                        }
                        println(" : ${diffs.average()}")
                    }
            }
            get("/post/{host}/{temp}") {
                val host = call.parameters["host"] ?: "unknown"
                val timestamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                val temp = call.parameters["temp"]?.toLong() ?: 0
                println("ADD $host,$timestamp,$temp")
                val temperature = Temperature(host, timestamp, temp)
                database.add(temperature)

                lastAdd.put(host, temperature)

                call.respondText("ok")
            }
            get("/start/{host}") {
                val host = call.parameters["host"] ?: "unknown"
                println("Client started: $host")
                call.respondText("ok")
            }
        }
    }.start(wait = true)

}

private fun getHost(it: Temperature): String {
    return rooms.getOrDefault(it.host, it.host)
}


fun Temperature?.correctTempertature(): Temperature?{
    if (this == null) return null
    val correction = correctionsPerRoom.get(host)?:0
    return Temperature(host, timestamp, temp + correction)
}

fun List<Temperature>.correctTemperatures() = this.mapNotNull { it.correctTempertature() }

fun getHtml(): String {
    val data: List<Temperature> = database.getAll().correctTemperatures()
    return """
            <html>
            <body>
             ${createGraph2("All", data)}
             <br>
             ${createGraph("CV", data.filter { it.host == CV })}
             <br>
             ${createGraph("Schuur", data.filter { it.host == SCHUUR })}
             <br>
             ${createGraph("Huiskamer", data.filter { it.host == HUISKAMER })}
             <br>
             ${createGraph("Aanbouw", data.filter { it.host == AANBOUW })}
             <br>
             ${createGraph("Bijkeuken", data.filter { it.host == BIJKEUKEN })}
             <br>
             ${createGraph("Eetkamer", data.filter { it.host == EETKAMER })}
             <br>
            </body>
            </html>
        """.trimIndent()
}

fun convertToDateViaInstant(dateToConvert: LocalDateTime): Date {
    return Date
        .from(
            dateToConvert.atZone(ZoneId.systemDefault())
                .toInstant()
        )
}


fun createGraph(title: String, data: List<Temperature>): String {

    val timeSeriesCollection = TimeSeriesCollection()
    val startDate = LocalDateTime.of(2022, 12, 10, 12, 0, 0).toEpochSecond(ZoneOffset.UTC)
    val endDate = LocalDateTime.of(2023, 12, 2, 22, 0, 0).toEpochSecond(ZoneOffset.UTC)

    val seriesData = TimeSeries(title)
    data.filter { it.timestamp > startDate && it.timestamp < endDate }.forEach {
        val time = it.timestamp.toDouble()
        val temp = toTemp(it.temp)
        seriesData.add(Millisecond(convertToDateViaInstant(LocalDateTime.ofEpochSecond(time.toLong(), 0, ZoneOffset.UTC))), temp)
    }
    timeSeriesCollection.addSeries(seriesData)
    val XYLineChart = ChartFactory.createTimeSeriesChart(title, "", "", timeSeriesCollection, false, false, false)


    val chart: JFreeChart = XYLineChart
    val g2 = SVGGraphics2D(2000.0, 400.0)
    val r = Rectangle(0, 0, 2000, 400)
    chart.draw(g2, r)
    val f = File("temp_chart.svg")
    SVGUtils.writeToSVG(f, g2.svgElement)

    val img = File("temp_chart.svg").useLines { it.toList() }
    return img[1]

}

fun createGraph2(title: String, data: List<Temperature>): String {

    val timeSeriesCollection = TimeSeriesCollection()
    val startDate = LocalDateTime.of(2022, 12, 10, 12, 0, 0).toEpochSecond(ZoneOffset.UTC)
    val endDate = LocalDateTime.of(2023, 12, 10, 10, 0, 0).toEpochSecond(ZoneOffset.UTC)

    rooms.keys.filter { it != CV }.forEach { key ->
        val name = rooms.get(key)
        val seriesData = TimeSeries(name)
        data.filter { it.host == key && it.timestamp > startDate && it.timestamp < endDate }.forEach {
            val time = it.timestamp.toDouble()
            val temp = toTemp(it.temp)
            seriesData.add(Millisecond(convertToDateViaInstant(LocalDateTime.ofEpochSecond(time.toLong(), 0, ZoneOffset.UTC))), temp)
        }
        timeSeriesCollection.addSeries(seriesData)
    }

    val XYLineChart = ChartFactory.createTimeSeriesChart(title, "", "", timeSeriesCollection, true, false, false)


    val chart: JFreeChart = XYLineChart
    val g2 = SVGGraphics2D(2000.0, 400.0)
    val r = Rectangle(0, 0, 2000, 400)
    chart.draw(g2, r)
    val f = File("temp_chart.svg")
    SVGUtils.writeToSVG(f, g2.svgElement)

    val img = File("temp_chart.svg").useLines { it.toList() }
    return img[1]

}


