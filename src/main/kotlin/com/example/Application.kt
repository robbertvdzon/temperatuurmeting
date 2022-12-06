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
    BIJKEUKEN to "BIJKEUKEN (4)" ,
    EETKAMER to "EETKAMER (5)",
    CV to "CV (6)"
)

val database = TempDatabase()
val lastAdd: MutableMap<String, Temperature> = mutableMapOf()

fun getTimestamp(timestamp: Long): String {
    return LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyy-MM-dd HH:mm:ss"))
}

fun toTemp(temp: Double): Double{
    //21 degrees = 14053
    //1 degree = 21.6
    val diffFrom21Degrees = 14053.0 - temp
    val diffDegrees = diffFrom21Degrees/21.6
    return 21.0+diffDegrees
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
//                    .filter { it !=  SCHUUR && it != CV}
                    .map { key ->
                    val lastAdded = lastAdd.get(key)?:Temperature("?",0,0.0)
                    val name = rooms.get(key)!!
                    "${name.padEnd(15)}: ${getTimestamp(lastAdded.timestamp)} : ${toTemp(lastAdded.temp)}"
                }
//                val list = lastAdd.values.map { "${getHost(it)}: ${getTimestamp(it.timestamp)} : ${it.temp}" }
                call.respondText(list.joinToString(separator = "\n"))
            }
            get("/post/{host}/{temp}") {
                val host = call.parameters["host"] ?: "unknown"
                val timestamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                val temp = call.parameters["temp"]?.toDouble() ?: 0.0
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
    return rooms.getOrDefault(it.host,it.host)
}


fun getHtml(): String {
    val data: List<Temperature> = database.getAll()
    return """
            <html>
            <body>
             ${createGraph("CV",data.filter { it.host == CV })}
             <br>
             ${createGraph("Schuur",data.filter { it.host == SCHUUR })}
             <br>
             ${createGraph("Huiskamer",data.filter { it.host == HUISKAMER })}
             <br>
             ${createGraph("Aanbouw",data.filter { it.host == AANBOUW })}
             <br>
             ${createGraph("Bijkeuken",data.filter { it.host == BIJKEUKEN })}
             <br>
             ${createGraph("Eetkamer",data.filter { it.host == EETKAMER })}
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
    val startDate = LocalDateTime.of(2022, 12,3,17,0,0).toEpochSecond(ZoneOffset.UTC)
    val endDate = LocalDateTime.of(2023, 12,2,22,0,0).toEpochSecond(ZoneOffset.UTC)

    val seriesData = TimeSeries(title)
    data.filter { it.timestamp>startDate && it.timestamp<endDate}
        .
    forEach {
        val time = it.timestamp.toDouble()
        val temp = toTemp(it.temp)
        seriesData.add(Millisecond(convertToDateViaInstant(LocalDateTime.ofEpochSecond(time.toLong(),0, ZoneOffset.UTC))), temp)
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


