package ch.epfl.esl.sportstracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.overlay.Marker;

import static android.graphics.Color.RED;
import static android.graphics.Color.TRANSPARENT;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class LessonActivity extends AppCompatActivity {
    public static final String RECEIVE_HEART_RATE = "RECEIVE_HEART_RATE";
    public static final String HEART_RATE = "HEART_RATE";
    public static final String RECEIVE_LOCATION = "RECEIVE_LOCATION";
    public static final String LONGITUDE = "LONGITUDE";
    public static final String LATITUDE = "LATITUDE";
    public static final String RECEIVE_ALARM = "RECEIVE_ALARM";

    private HeartRateBroadcastReceiver heartRateBroadcastReceiver;
    private LocationBroadcastReceiver locationBroadcastReceiver;
    private AlarmBroadcastReceiver alarmBroadcastReceiver;
    private boolean isTeacher;
    private String userID;
    private DatabaseReference recordingRefCurrent;
    private DatabaseReference recordingRefHistory;
    private ArrayList<Integer> hrDataArrayList = new ArrayList<>();
    private ArrayList<Double> hrLongArray = new ArrayList<>();
    private ArrayList<Double> hrLatArray = new ArrayList<>();
    private ArrayList<Integer> hrPlotData = new ArrayList<>();
    private boolean alarmWasReceived = false;
    private long lastUpdate = System.currentTimeMillis();
    private MapView map = null;
    private IMapController mapC = null;
    private static XYPlot heartRatePlot;
    private MonitorListener mListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson);

        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.v("MAP", "Permission is granted");
            } else {
                Log.v("MAP", "Permission is revoked");
                requestPermissions(new String[]{"android.permission" +
                        ".WRITE_EXTERNAL_STORAGE"}, 0);
            }
        }

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        mapC = map.getController();
        mapC.setZoom(17.0);
        GeoPoint startPoint = new GeoPoint(46.434449, 6.909257);
        mapC.setCenter(startPoint);

        heartRatePlot = findViewById(R.id.HRplot);
        configurePlot();
        for(int i=0;i<10;++i) {
            hrPlotData.add(0);
        }

        Intent intent = getIntent();
        isTeacher = intent.getBooleanExtra("isTeacher", false);
        userID = intent.getStringExtra("userID");
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference profileGetRef = database.getReference().child("Users").child(userID);
        recordingRefHistory = profileGetRef.child("recordings").push();
        recordingRefCurrent = profileGetRef.child("current_training");
    }

    private void configurePlot() {
        // Get background color from Theme
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.windowBackground,
                typedValue, true);
        int backgroundColor = typedValue.data;
        // Set background colors
        heartRatePlot.setPlotMargins(0, 0, 0, 0);
        heartRatePlot.getBorderPaint().setColor(backgroundColor);
        heartRatePlot.getBackgroundPaint().setColor(backgroundColor);
        heartRatePlot.getGraph().getBackgroundPaint().setColor(backgroundColor);
        heartRatePlot.getGraph().getGridBackgroundPaint().setColor
                (backgroundColor);
        // Set the grid color
        heartRatePlot.getGraph().getRangeGridLinePaint().setColor(Color.DKGRAY);
        heartRatePlot.getGraph().getDomainGridLinePaint().setColor(Color
                .DKGRAY);
        // Set the origin axes colors
        heartRatePlot.getGraph().getRangeOriginLinePaint().setColor(Color
                .DKGRAY);
        heartRatePlot.getGraph().getDomainOriginLinePaint().setColor(Color
                .DKGRAY);
        // Set the XY axis boundaries and step values
        heartRatePlot.setRangeBoundaries(40, 200, BoundaryMode.FIXED);
        heartRatePlot.setDomainBoundaries(0, 9,
                BoundaryMode.FIXED);
        heartRatePlot.setRangeStepValue(9); // 9 values 40 60 ... 200
        heartRatePlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT)
                .setFormat(new
                        DecimalFormat("#")); // Force the Axis to be integer
        heartRatePlot.setRangeLabel(getString(R.string.heart_rate));
    }

    public void StopCallback(View view) {
        if(!isTeacher) {
            Intent intentStopRec = new Intent(LessonActivity.this,
                    WearService.class);
            intentStopRec.setAction(WearService.ACTION_SEND.STOPACTIVITY.name());
            intentStopRec.putExtra(WearService.ACTIVITY_TO_STOP, BuildConfig.W_excerciseactivity);
            startService(intentStopRec);
            recordingRefCurrent.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    recordingRefHistory.runTransaction(new Transaction.Handler() {
                        @NonNull
                        @Override
                        public Transaction.Result doTransaction(@NonNull
                                                                        MutableData mutableData) {
                            mutableData.child("hr").setValue(hrDataArrayList);
                            mutableData.child("long").setValue
                                    (hrLongArray);
                            mutableData.child("lat").setValue
                                    (hrLatArray);
                            mutableData.child("datetime").setValue(System.currentTimeMillis());

                            return Transaction.success(mutableData);
                        }

                        @Override
                        public void onComplete(@Nullable DatabaseError
                                                       databaseError, boolean b,
                                               @Nullable DataSnapshot
                                                       dataSnapshot) {
                            finish();
                        }
                    });
                }
            });
        }
        else {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Get the HR data back from the watch
        if(!isTeacher) {
            heartRateBroadcastReceiver = new HeartRateBroadcastReceiver();
            LocalBroadcastManager.getInstance(this).registerReceiver
                    (heartRateBroadcastReceiver, new IntentFilter(RECEIVE_HEART_RATE));

            // Get the location data back from the watch
            locationBroadcastReceiver = new LocationBroadcastReceiver();
            LocalBroadcastManager.getInstance(this).registerReceiver
                    (locationBroadcastReceiver, new IntentFilter(RECEIVE_LOCATION));

            alarmBroadcastReceiver = new AlarmBroadcastReceiver();
            LocalBroadcastManager.getInstance(this).registerReceiver
                    (alarmBroadcastReceiver, new IntentFilter(RECEIVE_ALARM));
        }
        else {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference profileGetRef = database.getReference().child("Users").child(userID);
            mListener = new MonitorListener();
            recordingRefCurrent.addValueEventListener(mListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(!isTeacher) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(heartRateBroadcastReceiver);

            LocalBroadcastManager.getInstance(this).unregisterReceiver(locationBroadcastReceiver);

            LocalBroadcastManager.getInstance(this).unregisterReceiver(alarmBroadcastReceiver);
        } else {
            recordingRefCurrent.removeEventListener(mListener);
        }
    }

    private class HeartRateBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Show HR in a TextView
            int heartRateWatch = intent.getIntExtra(HEART_RATE, -1);
            TextView hrTextView = findViewById(R.id.hr);
            hrTextView.setText(String.valueOf(heartRateWatch));
            hrDataArrayList.add(heartRateWatch);
            hrPlotData.remove(0);
            hrPlotData.add(heartRateWatch);

            ArrayList<Number> xy = new ArrayList<>();
            for (int i = 0; i < 10; ++i) {
                xy.add(i);
                xy.add(hrPlotData.get(i));
            }
            XYSeries hrWatchSeries = new SimpleXYSeries(xy, SimpleXYSeries.ArrayFormat
                    .XY_VALS_INTERLEAVED, "HR Smart Watch");
            LineAndPointFormatter formatter = new LineAndPointFormatter(RED, TRANSPARENT,
                    TRANSPARENT, null);
            formatter.getLinePaint().setStrokeWidth(8);
            heartRatePlot.clear();
            heartRatePlot.addSeries(hrWatchSeries, formatter);
            heartRatePlot.redraw();

            //hrLatArray.add(46.519962);
            //hrLongArray.add(6.633597);
            updateDB();
        }
    }

    private class LocationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Update TextViews
            double longitude = intent.getDoubleExtra(LONGITUDE, -1);
            double latitude = intent.getDoubleExtra(LATITUDE, -1);

            TextView longitudeTextView = findViewById(R.id.longitude);
            longitudeTextView.setText(String.valueOf(longitude));

            TextView latitudeTextView = findViewById(R.id.latitude);
            latitudeTextView.setText(String.valueOf(latitude));

            hrLongArray.add(longitude);
            hrLatArray.add(latitude);
            GeoPoint currentPoint = new GeoPoint(latitude, longitude);
            mapC.setCenter(currentPoint);
            map.getOverlays().clear();
            Marker startMarker = new Marker(map);
            startMarker.setPosition(currentPoint);
            startMarker.setTitle("Position");
            map.getOverlays().add(startMarker);

            updateDB();
        }
    }

    private class AlarmBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(getApplicationContext(), R.string.emergency,
                    Toast.LENGTH_SHORT).show();
            alarmWasReceived = true;
            TextView emergTextView = findViewById(R.id.emergency);
            emergTextView.setText("HELP!!!");
            updateDB();
        }

    }

    private void updateDB() {
        if(System.currentTimeMillis() - lastUpdate > 5 * 1000) {
            recordingRefCurrent.runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull
                                                                MutableData mutableData) {
                    mutableData.child("hr").setValue(hrPlotData);
                    if(hrLatArray.size()>0) {
                        mutableData.child("long").setValue
                                (hrLongArray.get(hrLongArray.size()-1));
                        mutableData.child("lat").setValue
                                (hrLatArray.get(hrLatArray.size()-1));
                    }
                    mutableData.child("emergency").setValue
                            (alarmWasReceived);

                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(@Nullable DatabaseError
                                               databaseError, boolean b,
                                       @Nullable DataSnapshot
                                               dataSnapshot) {
                }
            });
            lastUpdate = System.currentTimeMillis();
        }
    }

    private class MonitorListener implements ValueEventListener {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

            if(dataSnapshot.hasChild("hr")) {
                        /*hrDataArrayList.clear();
                        for(final DataSnapshot hr: dataSnapshot.child("hr").getChildren()) {
                            int val = hr.getValue(Integer.class);
                            hrDataArrayList.add(val);
                        }
                        TextView hrTextView = findViewById(R.id.hr);
                        if(hrDataArrayList.size()>0)
                            hrTextView.setText(String.valueOf(hrDataArrayList.get(hrDataArrayList.size()-1)));*/
                hrPlotData.clear();
                for(final DataSnapshot hr: dataSnapshot.child("hr").getChildren()) {
                    int val = hr.getValue(Integer.class);
                    hrPlotData.add(val);
                }

                ArrayList<Number> xy = new ArrayList<>();
                for (int i = 0; i < 10; ++i) {
                    xy.add(i);
                    xy.add(hrPlotData.get(i));
                }
                XYSeries hrWatchSeries = new SimpleXYSeries(xy, SimpleXYSeries.ArrayFormat
                        .XY_VALS_INTERLEAVED, "HR Smart Watch");
                LineAndPointFormatter formatter = new LineAndPointFormatter(RED,
                        TRANSPARENT,
                        TRANSPARENT, null);
                formatter.getLinePaint().setStrokeWidth(8);
                heartRatePlot.clear();
                heartRatePlot.addSeries(hrWatchSeries, formatter);
                heartRatePlot.redraw();

                TextView hrTextView = findViewById(R.id.hr);
                hrTextView.setText(String.valueOf(hrPlotData.get(hrPlotData.size()-1)));
            }

            if(dataSnapshot.hasChild("long")) {
                double vallat = dataSnapshot.child("lat").getValue(Double.class);
                TextView latTextView = findViewById(R.id.latitude);
                latTextView.setText(String.valueOf(vallat));
                double vallong = dataSnapshot.child("long").getValue(Double.class);
                TextView longitudeTextView = findViewById(R.id.longitude);
                longitudeTextView.setText(String.valueOf(vallong));

                GeoPoint currentPoint = new GeoPoint(vallat, vallong);
                mapC.setCenter(currentPoint);
                map.getOverlays().clear();
                Marker startMarker = new Marker(map);
                startMarker.setPosition(currentPoint);
                startMarker.setTitle("Position");
                map.getOverlays().add(startMarker);
            }

            if(dataSnapshot.hasChild("emergency")) {
                alarmWasReceived = dataSnapshot.child("emergency").getValue(Boolean.class);
                TextView emergTextView = findViewById(R.id.emergency);
                if(alarmWasReceived) {
                    emergTextView.setText("HELP!!!");
                }
            }

        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    }
}
