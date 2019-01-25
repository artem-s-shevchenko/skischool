package ch.epfl.esl.sportstracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;

public class ExerciseActivity extends WearableActivity implements SensorEventListener,
        LocationListener {
    private ConstraintLayout mLayout;
    public static final String STOP_ACTIVITY = "STOP_ACTIVITY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission("android" + ""
                        + ".permission.BODY_SENSORS") == PackageManager
                        .PERMISSION_DENIED) {
            requestPermissions(new String[]{"android.permission" +
                    ".BODY_SENSORS"}, 0);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                (checkSelfPermission("android" + ""
                        + ".permission.ACCESS_FINE_LOCATION") == PackageManager
                        .PERMISSION_DENIED ||
                        checkSelfPermission("android.permission" +
                                ".ACCESS_COARSE_LOCATION") ==
                                PackageManager.PERMISSION_DENIED ||
                        checkSelfPermission("android" + "" +
                                ".permission.INTERNET") == PackageManager
                                .PERMISSION_DENIED)) {
            requestPermissions(new String[]{"android.permission" +
                    ".ACCESS_FINE_LOCATION", "android"
                    + ".permission.ACCESS_COARSE_LOCATION", "android" +
                    ".permission.INTERNET"}, 0);
        }

        mLayout = findViewById(R.id.containerRecording);

        final SensorManager sensorManager =
                (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor hr_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        sensorManager.registerListener(this, hr_sensor, SensorManager.SENSOR_DELAY_UI);

        // Acquire a reference to the system Location Manager
        final LocationManager locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000,
                        0, this);
            } catch (Exception e) {
                Log.w("ExerciseActivity", "Could not request location updates");
            }
        }
        ////TEST
        Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(loc != null) {
            double longitude = loc.getLongitude();
            double latitude = loc.getLatitude();

            Intent intent = new Intent(ExerciseActivity.this, WearService.class);
            intent.setAction(WearService.ACTION_SEND.LOCATION.name());
            intent.putExtra(WearService.LONGITUDE, longitude);
            intent.putExtra(WearService.LATITUDE, latitude);
            startService(intent);
        }
        else {
            Intent intent = new Intent(ExerciseActivity.this, WearService.class);
            intent.setAction(WearService.ACTION_SEND.LOCATION.name());
            intent.putExtra(WearService.LONGITUDE, 6.909257);
            intent.putExtra(WearService.LATITUDE, 46.434449);
            startService(intent);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                sensorManager.unregisterListener(ExerciseActivity.this);
                locationManager.removeUpdates(ExerciseActivity.this);
                finish();
            }
        }, new IntentFilter(STOP_ACTIVITY));

        // Enables Always-on
        setAmbientEnabled();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        updateDisplay();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mLayout.setBackgroundColor(getResources().getColor(android.R
                    .color.black, getTheme()));
        } else {
            mLayout.setBackgroundColor(getResources().getColor(android.R
                    .color.white, getTheme()));
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int heartRate = (int) event.values[0];

        Intent intent = new Intent(ExerciseActivity.this, WearService.class);
        intent.setAction(WearService.ACTION_SEND.HEART_RATE.name());
        intent.putExtra(WearService.HEART_RATE, heartRate);
        startService(intent);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onLocationChanged(Location location) {
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();

        Intent intent = new Intent(ExerciseActivity.this, WearService.class);
        intent.setAction(WearService.ACTION_SEND.LOCATION.name());
        intent.putExtra(WearService.LONGITUDE, longitude);
        intent.putExtra(WearService.LATITUDE, latitude);
        startService(intent);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public void AlarmCallback(View view) {
        Intent intent = new Intent(ExerciseActivity.this, WearService.class);
        intent.setAction(WearService.ACTION_SEND.ALARM.name());
        startService(intent);
    }
}
