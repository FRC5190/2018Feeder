package frc.team5190.feeder;

import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.tables.IRemote;
import edu.wpi.first.wpilibj.tables.IRemoteConnectionListener;
import edu.wpi.first.wpilibj.tables.ITable;
import edu.wpi.first.wpilibj.tables.ITableListener;
import jaci.pathfinder.Pathfinder;
import jaci.pathfinder.Trajectory;
import jaci.pathfinder.Waypoint;
import jaci.pathfinder.modifiers.TankModifier;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Base64;

class PathFeeder implements ITableListener, IRemoteConnectionListener {

    private NetworkTable pathfinderInputTable;
    private NetworkTable pathfinderOutputTable;
    private DisplayFrame display;

    PathFeeder() {
        display = new DisplayFrame();
        pathfinderInputTable = NetworkTable.getTable("pathfinderInput");
        pathfinderOutputTable = NetworkTable.getTable("pathfinderOutput");
        pathfinderInputTable.addTableListener(this, true);
        NetworkTable.addGlobalConnectionListener(this, true);

        // delete any old requests pending
        for (int i = 0; i < 20; i++) {
            pathfinderInputTable.delete("request_" + i);
            pathfinderInputTable.delete("folder_" + i);
            pathfinderInputTable.delete("path_" + i);
            pathfinderInputTable.delete("obstructed_" + i);
            pathfinderInputTable.delete("index_" + i);
        }
    }

    @Override
    public void valueChanged(ITable iTable, String string, Object receivedObject, boolean newValue) {
        if (string.startsWith("request_") && newValue) {
            int requestId = ((Double) receivedObject).intValue();
            String folder = pathfinderInputTable.getString("folder_" + requestId, "CS-L");
            String path = pathfinderInputTable.getString("path_" + requestId, "Switch");
            boolean obstructed = pathfinderInputTable.getBoolean("obstructed_" + requestId, false);
            int index = (int) pathfinderInputTable.getNumber("index_" + requestId, 0);
            feed(requestId, folder, path, obstructed, index);
        }
    }

    private void feed(int requestId, String folder, String path, boolean obstructed, int index) {
        String fileName = folder + "/" + path + ".bot";
        String line;
        ArrayList<Waypoint> wayPoints = new ArrayList<>();
        Trajectory[] trajectories = new Trajectory[2];

        display.setBackground(Color.YELLOW);
        System.out.println("Received request: " + requestId + ", " + folder + "/" + path + ", " + obstructed + ", " + index);

        // 1 - read the wayPoints from file
        try {
            InputStreamReader reader = new InputStreamReader(ClassLoader.getSystemResourceAsStream(fileName));
            BufferedReader bufferedReader = new BufferedReader(reader);

            while ((line = bufferedReader.readLine()) != null) {
                String[] tokenize = line.split(",");
                if (tokenize.length == 3) {
                    wayPoints.add(new Waypoint(
                            Double.parseDouble(tokenize[0]),
                            Double.parseDouble(tokenize[1]),
                            Pathfinder.d2r(Double.parseDouble(tokenize[2]))));
                }
            }

            bufferedReader.close();
        } catch (FileNotFoundException e) {
            display.setBackground(Color.BLACK);
            display.setTitle(e.getMessage());
            System.out.println("Unable to open file '" + fileName + "'");
            return;
        } catch (IOException e) {
            display.setBackground(Color.BLACK);
            display.setTitle(e.getMessage());
            System.out.println("Error reading file '" + fileName + "'");
            return;
        }

        // 2 - generate the profiles
        try {
            Trajectory.Config config = new Trajectory.Config(
                    Trajectory.FitMethod.HERMITE_QUINTIC, Trajectory.Config.SAMPLES_HIGH,
                    Constants.TIME_DELTA, Constants.MAX_VELOCITY,
                    Constants.MAX_ACCELERATION, Constants.MAX_JERK);
            Trajectory trajectory = Pathfinder.generate(wayPoints.toArray(new Waypoint[0]), config);
            TankModifier modifier = new TankModifier(trajectory);
            modifier.modify(Constants.WHEEL_WIDTH / 12.0);

            trajectories[0] = modifier.getLeftTrajectory();
            trajectories[1] = modifier.getRightTrajectory();
        } catch (Exception e) {
            display.setBackground(Color.BLACK);
            display.setTitle(e.getMessage());
            System.out.println("Could not generate trajectory from waypoints in '" + fileName + "'");
            return;
        }

        // serialize and publish the responses
        try {
            pathfinderOutputTable.putValue("trajectories_" + requestId, serializeTrajectoryArray(trajectories));
            pathfinderOutputTable.putValue("folder_" + requestId, folder);
            pathfinderOutputTable.putValue("path_" + requestId, path);
            pathfinderOutputTable.putValue("response_" + requestId, requestId);
        } catch (Exception e) {
            display.setBackground(Color.BLACK);
            display.setTitle(e.getMessage());
            System.out.println("Could not publish trajectory to NT");
            return;
        }

        // delete old keys
        pathfinderInputTable.delete("request_" + requestId);
        pathfinderInputTable.delete("folder_" + requestId);
        pathfinderInputTable.delete("path_" + requestId);
        pathfinderInputTable.delete("obstructed_" + requestId);
        pathfinderInputTable.delete("index_" + requestId);
        System.out.println("Processed request: " + requestId + ", " + folder + "/" + path + ", " + obstructed + ", " + index);
        display.setBackground(Color.GREEN);
    }

    private String serializeTrajectoryArray(Trajectory[] trajectoryArray) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ObjectOutputStream so = new ObjectOutputStream(bo);

        Double[][][] s = new Double[2][trajectoryArray[0].segments.length][8];
        for (int i = 0; i < trajectoryArray.length; i++) {
            for (int j = 0; j < trajectoryArray[i].segments.length; j++) {
                s[i][j][0] = trajectoryArray[i].segments[j].dt;
                s[i][j][1] = trajectoryArray[i].segments[j].x;
                s[i][j][2] = trajectoryArray[i].segments[j].y;
                s[i][j][3] = trajectoryArray[i].segments[j].position;
                s[i][j][4] = trajectoryArray[i].segments[j].velocity;
                s[i][j][5] = trajectoryArray[i].segments[j].acceleration;
                s[i][j][6] = trajectoryArray[i].segments[j].jerk;
                s[i][j][7] = trajectoryArray[i].segments[j].heading;
            }
        }

        so.writeObject(s);
        so.flush();
        return new String(Base64.getEncoder().encode(bo.toByteArray()));
    }

    @Override
    public void connected(IRemote arg0) {
        display.setBackground(Color.GREEN);
    }

    @Override
    public void disconnected(IRemote arg0) {
        display.setBackground(Color.RED);
    }
}