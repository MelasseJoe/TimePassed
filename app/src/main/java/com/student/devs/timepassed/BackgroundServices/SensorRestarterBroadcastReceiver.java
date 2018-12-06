package com.student.devs.timepassed.BackgroundServices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Reçoit le message envoyé par le onDestroy de SensorService et relance le service
 */
public class SensorRestarterBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!SensorService.hasStarted) {
            Log.i(SensorRestarterBroadcastReceiver.class.getSimpleName(), "Service Stops! Oooooooooooooppppssssss!!!!");
            context.startService(new Intent(context, SensorService.class));
        }
    }
}

//http://www.andreas-schrade.de/2017/03/28/android-threading/

//https://stackoverflow.com/questions/12219249/background-service-getting-killed-in-android

//https://stackoverflow.com/questions/51460417/who-killed-my-service
/*


    If you are implementing the service, override onStartCommand() and return START_STICKY as the result. It will tell the system that even if it will want to kill your service due to low memory, it should re-create it as soon as memory will be back to normal.
    If you are not sure 1st approach will work - you'll have to use AlarmManager http://developer.android.com/reference/android/app/AlarmManager.html . That is a system service, which will execute actions when you'll tell, for example periodically. That will ensure that if your service will be terminated, or even the whole process will die(for example with force close) - it will be 100% restarted by AlarmManager.

 */