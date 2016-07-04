package org.surrel.gpx_explorer;

import android.Manifest;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow;
import org.surrel.gpx_explorer.dummy.DummyContent;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * An activity representing properties of a Track. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link TrackDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class TrackPropertiesActivity extends AppCompatActivity {

    private static final String TAG = "TrackPropertiesActivity";
    private static final int READ_REQUEST_CODE = 1;
    private static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    private static final int OUT_ZOOM = 3;

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    private GPXReader gpxReader;
    private MapView map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        View recyclerView = findViewById(R.id.track_list);
        assert recyclerView != null;
        setupRecyclerView((RecyclerView) recyclerView);

        if (findViewById(R.id.track_detail_container) != null) {
            mTwoPane = true;
        }

        // Map
        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        map.getController().setZoom(OUT_ZOOM);
        map.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // START PERMISSION CHECK
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> permissions = new ArrayList<>();
            String message = "osmdroid permissions:";
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
//                message += "\nLocation to show user location.";
//            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                message += "\nStorage access to store map tiles.";
            }
            if (!permissions.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                String[] params = permissions.toArray(new String[permissions.size()]);
                requestPermissions(params, REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            } // else: We already have permissions, so handle as normal
        }

        gpxReader = new GPXReader();

        // File picker
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == READ_REQUEST_CODE && resultCode == AppCompatActivity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri;
            if (resultData != null) {
                uri = resultData.getData();
                String title = "";

                // The query, since it only applies to a single document, will only return
                // one row. There's no need to filter, sort, or select fields, since we want
                // all fields for one document.
                try (Cursor cursor = getContentResolver()
                        .query(uri, null, null, null, null, null)) {
                    // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
                    // "if there's anything to look at, look at it" conditionals.
                    if (cursor != null && cursor.moveToFirst()) {
                        String[] colNames = cursor.getColumnNames();
                        for (String name : colNames) {
                            Log.i(TAG, name + ": " + cursor.getString(cursor.getColumnIndex(name)));
                            if (name.equals(OpenableColumns.DISPLAY_NAME)) {
                                title = cursor.getString(cursor.getColumnIndex(name));
                                ActionBar bar = getActionBar();
                                if (bar != null) {
                                    bar.setTitle(title);
                                }
                            }
                        }
                    }
                }

                try {
                    ParcelFileDescriptor parcelFileDescriptor =
                            getContentResolver().openFileDescriptor(uri, "r");
                    gpxReader.loadFile(parcelFileDescriptor);
                    List<GeoPoint> pts = gpxReader.getPts();

                    Polyline line = new Polyline(getApplicationContext());
                    line.setTitle(title);
                    line.setSubDescription(Polyline.class.getCanonicalName());
                    line.setWidth(20f);
                    line.setPoints(pts);
                    line.setGeodesic(true);
                    line.setInfoWindow(new BasicInfoWindow(R.layout.bonuspack_bubble, map));
                    final String finalTitle = title;
                    line.setOnClickListener(new Polyline.OnClickListener() {
                        @Override
                        public boolean onClick(Polyline polyline, MapView mapView, GeoPoint eventPos) {
                            Toast.makeText(getApplicationContext(), finalTitle, Toast.LENGTH_LONG).show();
                            return false;
                        }
                    });
                    map.getOverlayManager().add(line);

                    // Pan and zoom to fit (these calls needed in this order!)
                    final BoundingBoxE6 bb = BoundingBoxE6.fromGeoPoints((ArrayList<? extends GeoPoint>) pts);
                    map.getController().setCenter(bb.getCenter());
                    map.getController().zoomToSpan(bb.getLatitudeSpanE6(), bb.getLongitudeSpanE6());
                    map.getController().setCenter(bb.getCenter());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(DummyContent.ITEMS));
    }

    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final List<DummyContent.DummyItem> mValues;

        public SimpleItemRecyclerViewAdapter(List<DummyContent.DummyItem> items) {
            mValues = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.track_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mItem = mValues.get(position);
            holder.mIdView.setText(mValues.get(position).id);
            holder.mContentView.setText(mValues.get(position).content);

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mTwoPane) {
                        Bundle arguments = new Bundle();
                        arguments.putString(TrackDetailFragment.ARG_ITEM_ID, holder.mItem.id);
                        TrackDetailFragment fragment = new TrackDetailFragment();
                        fragment.setArguments(arguments);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.track_detail_container, fragment)
                                .commit();
                    } else {
                        Context context = v.getContext();
                        Intent intent = new Intent(context, TrackDetailActivity.class);
                        intent.putExtra(TrackDetailFragment.ARG_ITEM_ID, holder.mItem.id);

                        context.startActivity(intent);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mIdView;
            public final TextView mContentView;
            public DummyContent.DummyItem mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mIdView = (TextView) view.findViewById(R.id.id);
                mContentView = (TextView) view.findViewById(R.id.content);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mContentView.getText() + "'";
            }
        }
    }
}
