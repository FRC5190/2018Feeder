package frc.team5190.feeder

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import edu.wpi.first.networktables.EntryListenerFlags
import edu.wpi.first.networktables.NetworkTableInstance
import jaci.pathfinder.Pathfinder
import jaci.pathfinder.Trajectory
import jaci.pathfinder.Trajectory.FitMethod
import jaci.pathfinder.Waypoint
import jaci.pathfinder.modifiers.TankModifier
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.awt.Color
import java.io.FileNotFoundException
import java.io.IOException


object PathFeeder {

    private val pathfinderOutputTable = NetworkTableInstance.getDefault().getTable("pathfinderOutput")!!
    private val pathfinderInputTable = NetworkTableInstance.getDefault().getTable("pathfinderInput")!!

    private val display: DisplayFrame = DisplayFrame()

    private val gson = Gson()

    init {
        pathfinderInputTable.addEntryListener({ table, key, entry, value, flags ->
            println("In $key $value")
            val request = gson.fromJson<PathRequest>(value.string)
            val folderName = request.folderName
            val fileName = request.fileName

            display.background = Color.YELLOW
            println("Received request: $folderName/$fileName")

            // 2 - generate the profiles
            val trajectories = generatePath(folderName, fileName)

            // serialize and publish the responses
            try {
                pathfinderOutputTable.getEntry("${key.substring(0, key.lastIndexOf('_'))}_response").forceSetString(gson.toJson(trajectories))
                NetworkTableInstance.getDefault().flush()
            } catch (e: Exception) {
                display.background = Color.BLACK
                display.title = e.message
                println("Could not publish trajectory to NT")
                return@addEntryListener
            }

            println("Processed request: $folderName/$fileName")
            display.background = Color.GREEN
        }, EntryListenerFlags.kNew or EntryListenerFlags.kUpdate)

        NetworkTableInstance.getDefault().addConnectionListener({
            display.background = if (it.connected) Color.GREEN else Color.RED
        }, true)
    }

    fun generatePath(folderName: String, fileName: String): Array<Trajectory>? {
        val filePath = "$folderName/$fileName.xml"

        val doc = try {
            Jsoup.parse(javaClass.classLoader.getResourceAsStream(filePath).use {
                it.bufferedReader().readText()
            }, "", Parser.xmlParser())!!
        } catch (e: FileNotFoundException) {
            display.background = Color.BLACK
            display.title = e.message
            println("Unable to open file '$fileName'")
            return null
        } catch (e: IOException) {
            display.background = Color.BLACK
            display.title = e.message
            println("Error reading file '$fileName'")
            return null
        }

        try {
            val trajectoryElement = doc.getElementsByTag("Trajectory").first()

            val timeStep = trajectoryElement.attr("dt").toDouble()
            val velocity = trajectoryElement.attr("velocity").toDouble()
            val acceleration = trajectoryElement.attr("acceleration").toDouble()
            val jerk = trajectoryElement.attr("jerk").toDouble()
            val wheelBaseW = trajectoryElement.attr("wheelBaseW").toDouble()

            val fitMethod = FitMethod.valueOf(trajectoryElement.attr("fitMethod"))

            val waypoints = trajectoryElement.getElementsByTag("Waypoint").map { waypointElement ->
                val xText = waypointElement.getElementsByTag("X").first().text().toDouble()
                val yText = waypointElement.getElementsByTag("Y").first().text().toDouble()
                val angleText = waypointElement.getElementsByTag("Angle").first().text().toDouble()

                Waypoint(xText, yText, angleText)
            }.toTypedArray()

            val config = Trajectory.Config(fitMethod, Trajectory.Config.SAMPLES_HIGH, timeStep, velocity, acceleration, jerk)
            val trajectory = Pathfinder.generate(waypoints, config)
            val modifier = TankModifier(trajectory)
            modifier.modify(wheelBaseW)

            return arrayOf(modifier.leftTrajectory, modifier.rightTrajectory)
        } catch (e: Exception) {
            display.background = Color.BLACK
            display.title = e.message
            println("Could not generate trajectory from waypoints in '$fileName'")
        }
        return null
    }

    class PathRequest(val folderName: String, val fileName: String)

}