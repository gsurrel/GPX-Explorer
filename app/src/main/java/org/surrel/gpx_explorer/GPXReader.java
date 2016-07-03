package org.surrel.gpx_explorer;

import android.net.Uri;
import android.util.Log;

import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gsurrel on 7/3/16.
 */
public class GPXReader {

    private String TAG = "GPXReader";

    void loadFile(Uri uri) {
        File file = new File(uri.toString());
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException e) {
            Log.e(TAG, "Couldn't read file!");
        }
        Log.d(TAG, text.toString());
    }

    List<GeoPoint> getPts() {
        List<GeoPoint> pts = new ArrayList<>();

        pts.add(new GeoPoint(40.796788, -73.949232));
        pts.add(new GeoPoint(40.796788, -73.981762));
        pts.add(new GeoPoint(40.768094, -73.981762));
        pts.add(new GeoPoint(40.768094, -73.949232));
        pts.add(new GeoPoint(40.796788, -73.949232));

        return pts;
    }
}
