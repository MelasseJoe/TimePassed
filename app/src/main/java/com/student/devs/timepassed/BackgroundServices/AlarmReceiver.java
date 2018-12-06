package com.student.devs.timepassed.BackgroundServices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Reçoit le message envoyé par l'heure qu'il est et relance le service
 */
public class AlarmReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!SensorService.hasStarted) {
            Log.i(SensorRestarterBroadcastReceiver.class.getSimpleName(), "Service Stops! Restarted by Alarm");
            context.startService(new Intent(context, SensorService.class));
        }
    }
}
