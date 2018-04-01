import frc.team5190.feeder.PathFeeder
import jaci.pathfinder.Pathfinder
import java.io.File

object FileWriter {

    @JvmStatic
    fun main(args: Array<String>) {

        val folderpaths = arrayOf(arrayOf("CS-L", "Drop First Cube", "Pickup Second Cube"),
                arrayOf("CS-R", "Drop First Cube"),
                arrayOf("LS-LL", "Drop First Cube", "Pickup Second Cube", "Pickup Third Cube"),
                arrayOf("LS-RR", "Drop First Cube"))

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
