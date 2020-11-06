/*
 * Copyright (c) 2020., University of Memphis, MD2K Center of Excellence
 * All Rights Reserved
 *
 */

package org.md2k.wearos.datacollector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class MyBroadCastReceiver extends BroadcastReceiver {
    private static String TAG = "MessageReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        SensorDataCollector sdCollector = SensorDataCollector.getInstance(context);
        sdCollector.triggerSensors();
    }
}