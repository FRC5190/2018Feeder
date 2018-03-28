package frc.team5190.feeder

import edu.wpi.first.networktables.NetworkTableInstance

object Feeder {

    @JvmStatic
    fun main(args: Array<String>) {
        NetworkTableInstance.getDefault().startClientTeam(5190)
        /* NetworkTable.setClientMode()
         NetworkTable.setTeam(5190)
         NetworkTable.setIPAddress("10.51.90.2")
         NetworkTable.initialize()*/
        PathFeeder
        VisionFeeder(false)
    }
}