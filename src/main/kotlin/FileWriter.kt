import frc.team5190.feeder.PathFeeder
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
                    val trajectories = PathFeeder.generatePath(folderpath[0], folderpath[j])!!

                    println("Processing " + folderpath[0] + "/" + folderpath[j])

                    Pathfinder.writeToCSV(File("folders/" + folderpath[0] + "/" + folderpath[j] + " Left Detailed.csv"), trajectories[0])
                    Pathfinder.writeToCSV(File("folders/" + folderpath[0] + "/" + folderpath[j] + " Right Detailed.csv"), trajectories[1])
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }

    }
}
