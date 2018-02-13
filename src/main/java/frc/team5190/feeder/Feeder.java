package frc.team5190.feeder;

import edu.wpi.first.wpilibj.networktables.NetworkTable;

public class Feeder {

    public static void main(String[] args) {
        NetworkTable.setClientMode();
        NetworkTable.setTeam(5190);
        NetworkTable.setIPAddress("10.51.90.2");
        NetworkTable.initialize();
        new PathFeeder();
        new VisionFeeder(false);
    }
}