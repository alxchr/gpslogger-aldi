/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 2 of the License, or
*    (at your option) any later version.
*
*    GPSLogger for Android is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.mendhak.gpslogger.loggers;

import android.location.Location;
import com.mendhak.gpslogger.common.RejectionHandler;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class Gpx10FileLogger implements IFileLogger {
    protected final static Object lock = new Object();

    private final static ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(128), new RejectionHandler());
    private File gpxFile = null;
    private final boolean addNewTrackSegment;
    private final int satelliteCount;
    protected final String name = "GPX";

    Gpx10FileLogger(File gpxFile, boolean addNewTrackSegment, int satelliteCount) {
        this.gpxFile = gpxFile;
        this.addNewTrackSegment = addNewTrackSegment;
        this.satelliteCount = satelliteCount;
    }


    public void Write(Location loc) throws Exception {
        long time = loc.getTime();
        if (time <= 0) {
            time = System.currentTimeMillis();
        }
        String dateTimeString = Utilities.GetIsoDateTime(new Date(time));

        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(dateTimeString, gpxFile, loc, addNewTrackSegment, satelliteCount, "");
        EXECUTOR.execute(writeHandler);
    }

    public void Annotate(String description, Location loc) throws Exception {

        long time = loc.getTime();
        if (time <= 0) {
            time = System.currentTimeMillis();
        }
        String dateTimeString = Utilities.GetIsoDateTime(new Date(time));

    //    Gpx10AnnotateHandler annotateHandler = new Gpx10AnnotateHandler(description, gpxFile, loc, dateTimeString);
    //    EXECUTOR.execute(annotateHandler);
        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(dateTimeString, gpxFile, loc, addNewTrackSegment, satelliteCount, description);
        EXECUTOR.execute(writeHandler);
    }

    @Override
    public String getName() {
        return name;
    }


}
/*
class Gpx10AnnotateHandler implements Runnable {
    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(Gpx10AnnotateHandler.class.getSimpleName());
    String description;
    File gpxFile;
    Location loc;
    String dateTimeString;

    public Gpx10AnnotateHandler(String description, File gpxFile, Location loc, String dateTimeString) {
        this.description = description;
        this.gpxFile = gpxFile;
        this.loc = loc;
        this.dateTimeString = dateTimeString;
    }

    @Override
    public void run() {

        synchronized (Gpx10FileLogger.lock) {
            if (!gpxFile.exists()) {
                return;
            }

            int offsetFromEnd = (Session.shouldAddNewTrackSegment()) ? 12 : 21;

            long startPosition = gpxFile.length() - offsetFromEnd;
            String waypoint, wpt = GetWaypointXml(loc, dateTimeString, description);

            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(gpxFile, "rw");
                raf.seek(startPosition);

                if (Session.shouldAddNewTrackSegment()) {
                    waypoint = wpt + "</trk></gpx>";
                } else {
                    Session.setAddNewTrackSegment(true);
                    waypoint = "</trkseg>" + wpt + "</trk></gpx>";
                }
                raf.write(waypoint.getBytes());
                raf.close();
                Utilities.AddFileToMediaDatabase(gpxFile, "text/plain");
                tracer.debug("Finished writing to GPX10 file");
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
            tracer.debug("Finished annotation to GPX10 File");
        }

    }

    String GetWaypointXml(Location loc, String dateTimeString, String description) {

        StringBuilder waypoint = new StringBuilder();

        waypoint.append("\n<wpt lat=\"")
                .append(String.valueOf(loc.getLatitude()))
                .append("\" lon=\"")
                .append(String.valueOf(loc.getLongitude()))
                .append("\">");

        if (loc.hasAltitude()) {
            waypoint.append("<ele>").append(String.valueOf(loc.getAltitude())).append("</ele>");
        } else {
            waypoint.append("<ele>0.0</ele>");
        }
        waypoint.append("<time>").append(dateTimeString).append("</time>");
        waypoint.append("<name>").append(description).append("</name>");
        waypoint.append("</wpt>\n");
        return waypoint.toString();
    }
}
*/

class Gpx10WriteHandler implements Runnable {
    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(Gpx10WriteHandler.class.getSimpleName());
    String dateTimeString;
    Location loc;
    private File gpxFile = null;
    private boolean addNewTrackSegment;
//    private int satelliteCount;
    String description;

    public Gpx10WriteHandler(String dateTimeString, File gpxFile, Location loc, boolean addNewTrackSegment, int satelliteCount,String description) {
        this.dateTimeString = dateTimeString;
        this.addNewTrackSegment = addNewTrackSegment;
        this.gpxFile = gpxFile;
        this.loc = loc;
    //    this.satelliteCount = satelliteCount;
        this.description = description;
    }

    @Override
    public void run() {
        synchronized (Gpx10FileLogger.lock) {

            try {
                if (!gpxFile.exists()) {
                    gpxFile.createNewFile();

                    FileOutputStream initialWriter = new FileOutputStream(gpxFile, true);
                    BufferedOutputStream initialOutput = new BufferedOutputStream(initialWriter);

                    StringBuilder initialXml = new StringBuilder();
                    String wpt = GetWaypointXml(loc, dateTimeString, "Start");
                    String filename = gpxFile.getName();
                    initialXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
                    initialXml.append("<gpx version=\"1.0\" creator=\"GPSLogger - http://gpslogger.mendhak.com/\" \n");
                    initialXml.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n");
                    initialXml.append("xmlns=\"http://www.topografix.com/GPX/1/0\" \n");
                    initialXml.append("xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 \n");
                    initialXml.append("http://www.topografix.com/GPX/1/0/gpx.xsd\">\n");
                    initialXml.append("<metadata><time>").append(dateTimeString).append("</time></metadata>\n").append("<trk>");
                    initialXml.append("<name>"+filename+"</name>"+wpt+"</trk></gpx>");
                    initialOutput.write(initialXml.toString().getBytes());
                    initialOutput.flush();
                    initialOutput.close();

                    //New file, so new segment.
                    addNewTrackSegment = true;
                }
                RandomAccessFile raf = new RandomAccessFile(gpxFile, "rw");
                int offsetFromEnd = (addNewTrackSegment) ? 12 : 21;
                long startPosition = gpxFile.length() - offsetFromEnd;
                raf.seek(startPosition);
                if (description.isEmpty()) {
                    String trackPoint = GetTrackPointXml(loc, dateTimeString);
                //    raf.seek(startPosition);
                    raf.write(trackPoint.getBytes());
                    raf.close();
                    if (Session.nextNumTrkpt() < 20) {
                        Session.setAddNewTrackSegment(false);
                    } else {
                        Session.setAddNewTrackSegment(true);
                        Session.clearNumTrkpt();
                    }
                    Utilities.AddFileToMediaDatabase(gpxFile, "text/plain");
                    tracer.debug("Finished writing to GPX10 file");
                } else {
                //    raf = new RandomAccessFile(gpxFile, "rw");
                //    raf.seek(startPosition);
                    String waypoint, wpt = GetWaypointXml(loc, dateTimeString, description);
                    if (addNewTrackSegment) {
                        waypoint = wpt + "</trk></gpx>";
                    } else {
                        Session.setAddNewTrackSegment(true);
                        Session.clearNumTrkpt();
                    waypoint = "</trkseg>" + wpt + "</trk></gpx>";
                    }
                    raf.write(waypoint.getBytes());
                    raf.close();
                    Utilities.AddFileToMediaDatabase(gpxFile, "text/plain");
                    tracer.debug("Finished writing to GPX10 file");
                }

            } catch (Exception e) {
                tracer.error("Gpx10FileLogger.Write", e);
            }
        }
    }

    String GetTrackPointXml(Location loc, String dateTimeString) {

        StringBuilder track = new StringBuilder();

        if (addNewTrackSegment) {
            track.append("<trkseg>\n");
        }

        track.append("<trkpt lat=\"")
                .append(String.valueOf((float)loc.getLatitude()))
                .append("\" lon=\"")
                .append(String.valueOf((float)loc.getLongitude()))
                .append("\">");

        if (loc.hasAltitude()) {
            track.append("<ele>").append(String.valueOf((float) loc.getAltitude())).append("</ele>");
        } else {
            track.append("<ele>0.0</ele>");
        }
        track.append("<time>").append(dateTimeString).append("</time>");
        track.append("</trkpt>\n");
        track.append("</trkseg></trk></gpx>");
        return track.toString();
    }
    String GetWaypointXml(Location loc, String dateTimeString, String description) {

        StringBuilder waypoint = new StringBuilder();

        waypoint.append("\n<wpt lat=\"")
                .append(String.valueOf((float)loc.getLatitude()))
                .append("\" lon=\"")
                .append(String.valueOf((float)loc.getLongitude()))
                .append("\">");

        if (loc.hasAltitude()) {
            waypoint.append("<ele>").append(String.valueOf((float)loc.getAltitude())).append("</ele>");
        } else {
            waypoint.append("<ele>0.0</ele>");
        }
        waypoint.append("<time>").append(dateTimeString).append("</time>");
        waypoint.append("<name>").append(description).append("</name>");
        waypoint.append("</wpt>\n");
        return waypoint.toString();
    }
}


