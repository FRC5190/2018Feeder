package frc.team5190.feeder;

import edu.wpi.cscore.*;
import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.tables.ITable;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;

class VisionFeeder {

    VisionFeeder(boolean display) {
        System.loadLibrary("opencv_java310");
        // This is the network port you want to stream the raw received image to
        // By rules, this has to be between 1180 and 1190, so 1185 is a good choice
        int streamPort = 1185;
        MjpegServer inputStream = new MjpegServer("MJPEG Server", streamPort);
        String cameraName = "5190";
        HttpCamera camera = setHttpCamera(cameraName, inputStream);
        if (camera == null) {
            camera = new HttpCamera("CoprocessorCamera", "YourURLHere");
            inputStream.setSource(camera);
        }

        JLabel matLabel = null;
        if (display) {
            JFrame frame = new JFrame();
            frame.setLayout(new FlowLayout());
            frame.setSize(640, 480);
            matLabel = new JLabel();
            frame.add(matLabel);
            frame.setVisible(true);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        }

        CvSink imageSink = new CvSink("CV Image Grabber");
        imageSink.setSource(camera);

        CvSource imageSource = new CvSource("CV Image Source", VideoMode.PixelFormat.kMJPEG, 640, 480, 30);
        MjpegServer cvStream = new MjpegServer("CV Image Stream", 1186);
        cvStream.setSource(imageSource);

        Mat src = new Mat();
        Mat hsv = new Mat();
        Mat mask = new Mat();
        Mat yellow = new Mat();
        Mat gray = new Mat();
        Mat blur = new Mat();
        Mat fltr = new Mat();
        Mat canny = new Mat();
        Mat dilation = new Mat();
        Mat kernel = new Mat();
        Scalar lowerYellow = new Scalar(20, 200, 100);
        Scalar upperYellow = new Scalar(40, 255, 255);

        // Infinitely process image
        while (true) {
            long frameTime = imageSink.grabFrame(src);
            if (frameTime == 0) continue;

            Imgproc.cvtColor(src, hsv, Imgproc.COLOR_BGR2HSV);
            Core.inRange(hsv, lowerYellow, upperYellow, mask);
            Core.bitwise_and(src, src, yellow, mask);
            Imgproc.cvtColor(yellow, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.GaussianBlur(gray, blur, new Size(5, 5), 0);
            Imgproc.bilateralFilter(blur, fltr, 1, 10, 100);
            Imgproc.Canny(fltr, canny, 10, 100);
            Imgproc.morphologyEx(canny, dilation, Imgproc.MORPH_CLOSE, kernel);

            if (display)
                showImage(hsv, matLabel);
        }
    }

    private static void showImage(Mat m, JLabel lbl) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (m.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }

        int bufferSize = m.channels() * m.cols() * m.rows();
        byte[] b = new byte[bufferSize];
        m.get(0, 0, b); // get all the pixels
        BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);
        ImageIcon icon = new ImageIcon(image);
        lbl.setIcon(icon);
    }


    private static HttpCamera setHttpCamera(String cameraName, MjpegServer server) {
        // Start by grabbing the camera from NetworkTables
        NetworkTable publishingTable = NetworkTable.getTable("CameraPublisher");
        // Wait for robot to connect. Allow this to be attempted indefinitely
        while (true) {
            try {
                if (publishingTable.getSubTables().size() > 0) {
                    break;
                }
                Thread.sleep(500);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        HttpCamera camera;
        if (!publishingTable.containsSubTable(cameraName)) {
            return null;
        }

        ITable cameraTable = publishingTable.getSubTable(cameraName);
        String[] urls = cameraTable.getStringArray("streams", null);
        if (urls == null) {
            return null;
        }

        ArrayList<String> fixedUrls = new ArrayList<String>();
        for (String url : urls) {
            if (url.startsWith("mjpg")) {
                fixedUrls.add(url.split(":", 2)[1]);
            }
        }

        camera = new HttpCamera("CoprocessorCamera", fixedUrls.toArray(new String[0]));
        server.setSource(camera);
        return camera;
    }
}