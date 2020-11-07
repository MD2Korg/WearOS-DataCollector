/*
 *  Copyright (c) 2020, The University of Memphis, MD2K Center of Excellence
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 *  FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.md2k.wearos.datacollector;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.zip.GZIPOutputStream;


public class SensorDataCollector implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {


    private static volatile SensorDataCollector sSoleInstance;
    private final Context context;
    private GoogleApiClient mGoogleApiClient;

    /**
     * CONFIG CONSTANTS
     */

    private static int level = -1;
    private static final int MAX_NUM_ROWS_PER_FILE = 50000; //Sufficiently large that there will be only 1 file/minute created.

    private static List<Long> ppg_time;
    private static List<Double> ppg1_array;

    private static List<Long> accel_time;
    private static List<Double> accel_array_x;
    private static List<Double> accel_array_y;
    private static List<Double> accel_array_z;

    private static List<Long> gyro_time;
    private static List<Double> gyro_array_x;
    private static List<Double> gyro_array_y;
    private static List<Double> gyro_array_z;

    private static long battery_time;
    private static int battery_level = -1;

    private static SensorManager mSensorManager;


    private static final IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);


    private static Intent batteryStatus;


    private static final List<String> accel_headers = new ArrayList<>(Arrays.asList("timestamp", "localtime", "x", "y", "z"));
    private static final List<String> gyro_headers = new ArrayList<>(Arrays.asList("timestamp", "localtime", "x", "y", "z"));
    private static final List<String> ppg_headers = new ArrayList<>(Arrays.asList("timestamp", "localtime", "ppg1"));
    private static final List<String> battery_headers = new ArrayList<>(Arrays.asList("timestamp", "localtime", "level"));


    private static final String TAG = "MD2K-SensorDataCollector";

    private String ccUID;

    private Location lastKnownLocation;
    private LocationRequest locationRequest;

    private static final long lastTimeGenerated = 0;

    private final Runnable mReleaseRunnable = new Runnable() {
        @Override
        public void run() {
            mSensorManager.unregisterListener(ppgListener);
            mSensorManager.unregisterListener(gyroListener);
            mSensorManager.unregisterListener(accelListener);

        }
    };

    private SensorDataCollector(Context applicationContext) {

        if (sSoleInstance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class");
        }
        this.context = applicationContext;



        ppg_time = new ArrayList<>();
        ppg1_array = new ArrayList<>();

        accel_time = new ArrayList<>();
        accel_array_x = new ArrayList<>();
        accel_array_y = new ArrayList<>();
        accel_array_z = new ArrayList<>();

        gyro_time = new ArrayList<>();
        gyro_array_x = new ArrayList<>();
        gyro_array_y = new ArrayList<>();
        gyro_array_z = new ArrayList<>();


        batteryStatus = applicationContext.registerReceiver(null, ifilter);

        mSensorManager = ((SensorManager) applicationContext.getSystemService(Context.SENSOR_SERVICE));
    }


    void enableAccel() {
        mSensorManager.registerListener(accelListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME, 60000000);
    }
    void enableGyro() {
        mSensorManager.registerListener(gyroListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME, 60000000);
    }
    void enablePPG() {
        mSensorManager.registerListener(ppgListener, mSensorManager.getDefaultSensor(65572), SensorManager.SENSOR_DELAY_FASTEST);
    }
    void enableGPS() {
        mGoogleApiClient = new GoogleApiClient.Builder(context) //TODO: Look into this
                .addApi(LocationServices.API)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }

        //Location

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(600000); //Controls the speed with which we force a location update.
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);


//        requestLocationUpdates();
    }

    void disableAccel() {
        mSensorManager.unregisterListener(accelListener);
    }
    void disableGyro() {
        mSensorManager.unregisterListener(gyroListener);
    }
    void disablePPG() {
        mSensorManager.unregisterListener(ppgListener);
    }
    void disableGPS() {
        locationRequest.setExpirationTime(0);
    }

    static SensorDataCollector getInstance(Context applicationContext) {

        if (sSoleInstance == null) {
            synchronized (SensorDataCollector.class) {
                if (sSoleInstance == null)
                    sSoleInstance = new SensorDataCollector(applicationContext);
            }
        }
        return sSoleInstance;
    }


    public Context getContext() {
        return context;
    }


    void triggerSensors() {

        checkBattery();

        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(System.currentTimeMillis());


        //Log PPG data
        if (ppg_time.size() >= 100) {
            final List<Long> ppg_time_output_buffer = new ArrayList<>(ppg_time);
            final List<Double> ppg1_output_buffer = new ArrayList<>(ppg1_array);

            ppg_time.clear();
            ppg1_array.clear();

            Thread t = new Thread() {
                public void run() {
                    writePPGFile(ppg_time_output_buffer, ppg1_output_buffer);
                }

            };
            t.start();
        }

//        Log Accel data
        if (accel_time.size() >= 100) {
            final List<Long> accel_time_output_buffer = new ArrayList<>(accel_time);
            final List<Double> accel_x_output_buffer = new ArrayList<>(accel_array_x);
            final List<Double> accel_y_output_buffer = new ArrayList<>(accel_array_y);
            final List<Double> accel_z_output_buffer = new ArrayList<>(accel_array_z);

            accel_time.clear();
            accel_array_x.clear();
            accel_array_y.clear();
            accel_array_z.clear();

            Thread t = new Thread() {
                public void run() {
                    writeAccelFile(accel_time_output_buffer, accel_x_output_buffer, accel_y_output_buffer, accel_z_output_buffer);
                }

            };
            t.start();
        }

//        Log Gyro data
        if (gyro_time.size() >= 100) {
            final List<Long> gyro_time_output_buffer = new ArrayList<>(gyro_time);
            final List<Double> gyro_x_output_buffer = new ArrayList<>(gyro_array_x);
            final List<Double> gyro_y_output_buffer = new ArrayList<>(gyro_array_y);
            final List<Double> gyro_z_output_buffer = new ArrayList<>(gyro_array_z);

            gyro_time.clear();
            gyro_array_x.clear();
            gyro_array_y.clear();
            gyro_array_z.clear();

            Thread t = new Thread() {
                public void run() {
                    writeGyroFile(gyro_time_output_buffer, gyro_x_output_buffer, gyro_y_output_buffer, gyro_z_output_buffer);
                }

            };
            t.start();
        }
    }


    SensorEventListener ppgListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            long ts = System.currentTimeMillis() + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000L;

            ppg_time.add(ts);
            ppg1_array.add((double) event.values[0]);

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };


    SensorEventListener accelListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            long ts = System.currentTimeMillis() + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000L;

            accel_time.add(ts);
            accel_array_x.add((double) event.values[0]);
            accel_array_y.add((double) event.values[1]);
            accel_array_z.add((double) event.values[2]);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    SensorEventListener gyroListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            long ts = System.currentTimeMillis() + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000L;

            gyro_time.add(ts);
            gyro_array_x.add((double) event.values[0]);
            gyro_array_y.add((double) event.values[0]);
            gyro_array_z.add((double) event.values[2]);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private void checkBattery() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.context.registerReceiver(null, ifilter);
        level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);

        if (battery_level != level) {
            battery_time = System.currentTimeMillis();
            battery_level = level;
            Thread t = new Thread() {
                public void run() {
                    writeBatteryFile(battery_time, battery_level);
                }

            };
            t.start();
        }
    }

    private void writeBatteryFile(Long time, int level) {
        try {
            File sdcard = Environment.getExternalStorageDirectory();
            File o = new File(sdcard.getAbsoluteFile(), "battery_" + System.currentTimeMillis() + ".csv.gz");

            FileOutputStream output = new FileOutputStream(o);
            try {
                Writer writer = new OutputStreamWriter(new GZIPOutputStream(output));

                int offset = Calendar.getInstance().getTimeZone().getRawOffset();

                //Pack headers
                try {
                    writer.write(String.join(",", battery_headers) + "\n");

                    writer.write(time * 1000 + "," + (time + offset) * 1000 + "," + level + "\n");
                } finally {
                    writer.close();
                }


            } finally {
                output.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeAccelFile(List<Long> time, List<Double> x, List<Double> y, List<Double> z) {
        try {
            for (int accel_index = 0; accel_index < time.size(); accel_index += MAX_NUM_ROWS_PER_FILE) {
                File sdcard = Environment.getExternalStorageDirectory();
                File o = new File(sdcard.getAbsoluteFile(), "accel_" + System.currentTimeMillis() + ".csv.gz");

                int offset = Calendar.getInstance().getTimeZone().getRawOffset();

                FileOutputStream output = new FileOutputStream(o);
                try {
                    Writer writer = new OutputStreamWriter(new GZIPOutputStream(output));
                    //Pack headers
                    try {
                        writer.write(String.join(",", accel_headers) + "\n");

                        for (int i = accel_index; i < time.size() && i < accel_index + MAX_NUM_ROWS_PER_FILE; i++) {
                            writer.write(time.get(i)*1000 + "," + (time.get(i) + offset) * 1000 + ",");
                            writer.write(x.get(i) + "," + y.get(i) + "," + z.get(i) + "\n" );
                        }

                    } finally {
                        writer.close();
                    }


                } finally {
                    output.close();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeGyroFile(List<Long> time, List<Double> x, List<Double> y, List<Double> z) {
        try {
            for (int gyro_index = 0; gyro_index < time.size(); gyro_index += MAX_NUM_ROWS_PER_FILE) {
                File sdcard = Environment.getExternalStorageDirectory();
                File o = new File(sdcard.getAbsoluteFile(), "gyro_" + System.currentTimeMillis() + ".csv.gz");


                int offset = Calendar.getInstance().getTimeZone().getRawOffset();

                FileOutputStream output = new FileOutputStream(o);
                try {
                    Writer writer = new OutputStreamWriter(new GZIPOutputStream(output));
                    //Pack headers
                    try {
                        writer.write(String.join(",", gyro_headers) + "\n");

                        for (int i = gyro_index; i < time.size() && i < gyro_index + MAX_NUM_ROWS_PER_FILE; i++) {
                            writer.write(time.get(i)*1000 + "," + (time.get(i) + offset) * 1000 + ",");
                            writer.write(x.get(i) + "," + y.get(i) + "," + z.get(i) + "\n" );
                        }

                    } finally {
                        writer.close();
                    }


                } finally {
                    output.close();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void writePPGFile(List<Long> time, List<Double> ppg1) {

        Long timedelta = (time.get(time.size() - 1) - time.get(0)) / 1000;
        Log.d("DEBUG", "" + time.size() + " (" + timedelta + "): " + time.size() * 1.0 / timedelta);

        try {
            for (int ppg_index = 0; ppg_index < time.size(); ppg_index += MAX_NUM_ROWS_PER_FILE) {

                File sdcard = Environment.getExternalStorageDirectory();
                File o = new File(sdcard.getAbsoluteFile(), "ppg_" + System.currentTimeMillis() + ".csv.gz");


                int offset = Calendar.getInstance().getTimeZone().getRawOffset();

                FileOutputStream output = new FileOutputStream(o);
                try {
                    Writer writer = new OutputStreamWriter(new GZIPOutputStream(output));
                    //Pack headers
                    try {
                        writer.write(String.join(",", ppg_headers) + "\n");

                        for (int i = ppg_index; i < time.size() && i < ppg_index + MAX_NUM_ROWS_PER_FILE; i++) {
                            writer.write(time.get(i) * 1000 + "," + (time.get(i) + offset) * 1000 + ",");
                            writer.write(ppg1.get(i) + "\n");
                        }

                    } finally {
                        writer.close();
                    }


                } finally {
                    output.close();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
//        requestLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        lastKnownLocation = location;

    }

    private void requestLocationUpdates() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setInterval(10 * 60000);
        locationRequest.setFastestInterval(60000);
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
        } catch (SecurityException unlikely) {

        }
    }

}
