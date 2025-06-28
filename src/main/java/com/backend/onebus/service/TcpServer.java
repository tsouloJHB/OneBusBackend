package com.backend.onebus.service;

import com.backend.onebus.model.BusLocation;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TcpServer {
    @Autowired
    private BusTrackingService trackingService;
    @Autowired
    private GeometryFactory geometryFactory;

    public void startTcpServer() throws Exception {
        ServerSocket serverSocket = new ServerSocket(8081);
        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(() -> handleTcpClient(clientSocket)).start();
        }
    }

    private void handleTcpClient(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                BusLocation location = parseTcpPacket(line);
                if (location != null) {
                    trackingService.processTrackerPayload(location);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BusLocation parseTcpPacket(String packet) {
        // Example: ##,imei:359339072173798,A,2612.5458,S,02803.4981,E,50.0,110.5,260624,153243.00
        Pattern pattern = Pattern.compile("##,imei:(\\d+),A,(\\d+\\.\\d+),([NS]),(\\d+\\.\\d+),([EW]),(\\d+\\.\\d),(\\d+\\.\\d),(\\d{6}),(\\d{6})\\.\\d+");
        Matcher matcher = pattern.matcher(packet);
        if (matcher.matches()) {
            BusLocation location = new BusLocation();
            location.setTrackerImei(matcher.group(1));
            double lat = Double.parseDouble(matcher.group(2)) / 100;
            if (matcher.group(3).equals("S")) lat = -lat;
            double lon = Double.parseDouble(matcher.group(4)) / 100;
            if (matcher.group(5).equals("W")) lon = -lon;
            location.setLat(lat);
            location.setLon(lon);
            // Temporarily remove geometry creation to avoid PostGIS issues
            // org.locationtech.jts.geom.Point point = geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(lon, lat));
            // point.setSRID(4326); // Set SRID to WGS84
            // location.setLocation(point);
            location.setSpeedKmh(Double.parseDouble(matcher.group(6)) * 1.852); // Convert knots to km/h
            location.setHeadingDegrees(Double.parseDouble(matcher.group(7)));
            location.setTimestamp("20" + matcher.group(8) + "T" + matcher.group(9) + "Z");
            return location;
        }
        return null;
    }
}