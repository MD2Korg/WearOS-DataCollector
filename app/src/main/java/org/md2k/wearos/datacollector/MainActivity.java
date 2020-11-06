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

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import androidx.core.content.ContextCompat;

public class MainActivity extends WearableActivity {

    private static PowerManager.WakeLock mWakeLock;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        String[] permissions = {
                Manifest.permission.BODY_SENSORS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION};

        boolean hasPermission = true;
        for (String permission : permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                hasPermission = false;
            }
        }
        if (!hasPermission) {
            requestPermissions(permissions, 12345);
        }


        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WearOS:WakeLock");


        setContentView(R.layout.activity_main);
        startService();


        final SensorDataCollector sdInstance = SensorDataCollector.getInstance(getApplicationContext());

        final ToggleButton accelToggle = (ToggleButton) findViewById(R.id.accelButton);
        accelToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.d("BUTTON", "ACCEL CHECKED");
                    sdInstance.enableAccel();
                } else {
                    Log.d("BUTTON", "ACCEL NOT CHECKED");
                    sdInstance.disableAccel();
                }
            }
        });

        ToggleButton gyroToggle = (ToggleButton) findViewById(R.id.gyroButton);
        gyroToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.d("BUTTON", "GYRO CHECKED");
                    sdInstance.enableGyro();
                } else {
                    Log.d("BUTTON", "GYRO NOT CHECKED");
                    sdInstance.disableGyro();
                }
            }
        });

        ToggleButton ppgToggle = (ToggleButton) findViewById(R.id.ppgButton);
        ppgToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.d("BUTTON", "PPG CHECKED");
                    sdInstance.enablePPG();
                } else {
                    Log.d("BUTTON", "PPG NOT CHECKED");
                    sdInstance.disablePPG();
                }
            }
        });

        ToggleButton gpsToggle = (ToggleButton) findViewById(R.id.gpsButton);
        gpsToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.d("BUTTON", "GPS CHECKED");
                    sdInstance.enableGPS();
                } else {
                    Log.d("BUTTON", "GPS NOT CHECKED");
                    sdInstance.disableGPS();
                }
            }
        });


        setAmbientEnabled();
    }

    private void startService() {
        Intent serviceIntent = new Intent(this, DataCollectionService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }



}