import frc.team5190.feeder.Constants
import jaci.pathfinder.*
import jaci.pathfinder.modifiers.TankModifier
import java.io.*
import java.util.*

object FileWriter {

    @JvmStatic
    fun main(args: Array<String>) {

        val folderpaths = arrayOf(arrayOf("CS-L", "Switch", "Center", "Switch 2"), arrayOf("CS-R", "Switch", "Center", "Switch 2"), arrayOf("LS-LL", "Scale"), arrayOf("LS-LR", "Switch"), arrayOf("LS-RR", "Scale"))

        for (folderpath in folderpaths) {
            for (j in 1 until folderpath.size) {
                try {
                    val waypoints = ArrayList<Waypoint>()

                    val reader = InputStreamReader(ClassLoader.getSystemResourceAsStream(folderpath[0] + "/" + folderpath[j] + ".bot"))
                    val bufferedReader = BufferedReader(reader)
                    val trajectories = arrayOfNulls<Trajectory>(2)

                    println("Processing " + folderpath[0] + "/" + folderpath[j])

                    var line = bufferedReader.readLine()
                    while (line != null) {

                        val tokenize = line.split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                        if (tokenize.size == 3) {
                            waypoints.add(Waypoint(
                                    (tokenize[0]).toDouble(),
                                    (tokenize[1]).toDouble(),
                                    (Pathfinder.d2r((tokenize[2].toDouble())))))
                        }
                        line = bufferedReader.readLine()
                    }

                    val config = Trajectory.Config(
                            Trajectory.FitMethod.HERMITE_CUBIC, Trajectory.Config.SAMPLES_HIGH,
                            Constants.TIME_DELTA, Constants.MAX_VELOCITY,
                            Constants.MAX_ACCELERATION, Constants.MAX_JERK)
                    val trajectory = Pathfinder.generate(waypoints.toTypedArray(), config)
                    val modifier = TankModifier(trajectory)
                    modifier.modify(Constants.WHEEL_WIDTH / 12.0)

                    trajectories[0] = modifier.leftTrajectory
                    trajectories[1] = modifier.rightTrajectory

                    Pathfinder.writeToCSV(File("folders/" + folderpath[0] + "/" + folderpath[j] + " Left Detailed.csv"), trajectories[0])
                    Pathfinder.writeToCSV(File("folders/" + folderpath[0] + "/" + folderpath[j] + " Right Detailed.csv"), trajectories[1])

                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }

    }
}
