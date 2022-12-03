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
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import org.jfree.svg.SVGGraphics2D
import org.jfree.svg.SVGUtils
import java.awt.Rectangle
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset


val database = TempDatabase()
val lastAdd: MutableMap<String, Temperature> = mutableMapOf()

fun getTimestamp(timestamp: Long): String {
    return LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC).toString()
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
                val list = lastAdd.values.map { "${it.host}: ${getTimestamp(it.timestamp)} : ${it.temp}" }
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

fun Application.module() {
    routing {
        get("/") {
            call.respondText("Hello, world!")
        }
    }
}

fun getHtml(): String {
    return """
            <html>
            <body>
             hoi
             ${test()}
            </body>
            </html>
        """.trimIndent()
}

fun test(): String {


    val data = database.getAll()
    val client1 = data.filter { it.host == "28:cd:c1:02:1e:22" }
    val client2 = data.filter { it.host == "28:cd:c1:02:1e:2a" }
    val client3 = data.filter { it.host == "28:cd:c1:02:1e:24" }
    val client4 = data.filter { it.host == "28:cd:c1:02:1e:28" }
    val client5 = data.filter { it.host == "28:cd:c1:02:1e:2c" }
    val client6 = data.filter { it.host == "28:cd:c1:02:1e:26" }

    val svgXYDataSeries = XYSeriesCollection()

    val team1_xy_data = XYSeries("Aanbouw")
    client1.forEach {
        val time = it.timestamp.toDouble()
        val temp = it.temp-14000
        team1_xy_data.add(time, temp)
    }
    svgXYDataSeries.addSeries(team1_xy_data)

    val team2_xy_data = XYSeries("Huiskamer")
    client2.forEach {
        val time = it.timestamp.toDouble()
        val temp = it.temp-14000
        team2_xy_data.add(time, temp)
    }
    svgXYDataSeries.addSeries(team2_xy_data)

    val team3_xy_data = XYSeries("Schuur")
    client3.forEach {
        val time = it.timestamp.toDouble()
        val temp = it.temp-14000
        team3_xy_data.add(time, temp)
    }
    svgXYDataSeries.addSeries(team3_xy_data)

    val team4_xy_data = XYSeries("Bijkeuken")
    client4.forEach {
        val time = it.timestamp.toDouble()
        val temp = it.temp-14000
        team4_xy_data.add(time, temp)
    }
    svgXYDataSeries.addSeries(team4_xy_data)

    val team5_xy_data = XYSeries("Kelder")
    client5.forEach {
        val time = it.timestamp.toDouble()
        val temp = it.temp-14000
        team5_xy_data.add(time, temp)
    }
    svgXYDataSeries.addSeries(team5_xy_data)

    val team6_xy_data = XYSeries("CV")
    client6.forEach {
        val time = it.timestamp.toDouble()
        val temp = it.temp-14000
        team6_xy_data.add(time, temp)
    }
    svgXYDataSeries.addSeries(team6_xy_data)

    val XYLineChart = ChartFactory.createXYLineChart("Temp", "Time", "Temp", svgXYDataSeries, PlotOrientation.VERTICAL, true, true, false)


    val chart: JFreeChart = XYLineChart
    val g2 = SVGGraphics2D(600.0, 400.0)
    val r = Rectangle(0, 0, 600, 400)
    chart.draw(g2, r)
    val f = File("SVGBarChartDemo1.svg")
    SVGUtils.writeToSVG(f, g2.svgElement)

    val img = File("SVGBarChartDemo1.svg").useLines { it.toList() }
    return img[1]

}


