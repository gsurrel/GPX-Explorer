package org.surrel.gpx_explorer;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gsurrel on 7/3/16.
 */
public class GPXReader {

    List<GeoPoint> pts = new ArrayList<>();
    private String TAG = "GPXReader";

    void loadFile(ParcelFileDescriptor parcelFile) {
        FileDescriptor file = parcelFile.getFileDescriptor();
        Log.e(TAG, "Loading file");
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                Log.e(TAG, line);
                float lat = Float.NaN, lon = Float.NaN;
                String[] lineSplits = line.split("[\" ]+");
                for (int i = 0; i < lineSplits.length - 1; i++) {
                    String split = lineSplits[i];
                    Log.e(TAG, split);
                    if (split.equals("lat=")) {
                        lat = Float.valueOf(lineSplits[i + 1]);
                    } else if (split.equals("lon=")) {
                        lon = Float.valueOf(lineSplits[i + 1]);
                    }
                    Log.e(TAG, "Lat/Lon: " + lat + ", " + lon);
                    if (!Float.isNaN(lat) && !Float.isNaN(lon)) {
                        pts.add(new GeoPoint(lat, lon));
                        Log.d(TAG, "Lat/Lon: " + lat + ", " + lon);
                    }
                }
            }
            br.close();
        } catch (IOException e) {
            Log.e(TAG, "Couldn't read file!");
        }
    }

    List<GeoPoint> getPts() {
//        pts.add(new GeoPoint(40.796788, -73.949232));
//        pts.add(new GeoPoint(40.796788, -73.981762));
//        pts.add(new GeoPoint(40.768094, -73.981762));
//        pts.add(new GeoPoint(40.768094, -73.949232));
//        pts.add(new GeoPoint(40.796788, -73.949232));

        return pts;
    }
}
