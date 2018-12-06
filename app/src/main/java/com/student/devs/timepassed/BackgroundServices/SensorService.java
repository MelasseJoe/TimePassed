package com.student.devs.timepassed.BackgroundServices;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;

import com.student.devs.timepassed.R;

import java.util.List;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

/**
 * Lance le service espion
 */
public class SensorService extends Service {
    public int counter = 0;
    public int counter_buffer = 0;

    public static boolean hasStarted = false; //permet de savoir si le timer est déjà lancé ou non

    public BroadcastReceiver mScreenStateReceiver;

    public String APP_name;

    // long startTime = SystemClock.elapsedRealtime();

    public String prevApp = " "; // sert à la comparaison entre l'appli en cours et l'appli antérieur (lorsque que l'utilisateur a passer un temps suffisant sur une appli)
    public String prevTickApp = " "; // sert à comparer l'appli ouverte actuellement et celle de la seconde d'avant
    public String saveApp = ""; //Application en cours d'utilisation depuis + de 10 min (ENregistrable sur la Bdd)
    public boolean demande = false; // vrai si l'utilisateur a passer suffisament de temps sur une appli (quand bigTimer > seuilSocio)
    public boolean locked = false; // vrai si le téléphone est verrouiullé
    public int bigTimer = 0; // timer avant de considérer qu'il a passé assez de temps sur l'appli
    public int smallTimer = 0; // timer avant d'envoyer la notif
    public int saveTime = 0; //Sauvegarde du temps de bigtimer, une fois le seuil save passé.
    public int savedBigTimer = 0; //Sauvegarde le bigTimer au moment ou on verrouille le téléphone


    public Context context;


    public SensorService(Context applicationContext) {
        super();
        Log.i("HERE", "here I am!");

        context = applicationContext;


        /* Utiliser dans la fonction TImer(), on en a normalement plus besoin ici

        //TODO//////////////////////////////////////code temps/////////////////////////////
        long endTime = SystemClock.elapsedRealtime();
        long elapsedMilliSeconds = endTime - startTime;
        double elapsedSeconds =  elapsedMilliSeconds / 1000.0;
        int elspSec = (int)elapsedSeconds;
        ////////////////////////////////////////code temps/////////////////////////////


        */

    }

    public SensorService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        startTimer();
        IntentFilter screenStateFilter = new IntentFilter();
        //screenStateFilter.addAction(Intent.ACTION_SCREEN_ON); servait pour le test de Screen_ON
        registerReceiver(mScreenStateReceiver, screenStateFilter);
        Notification notification = new Notification();
        startForeground(1234, notification);
        return START_STICKY;
    }


    @Override
    public void onCreate(){

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("EXIT", "ondestroy!");
        Intent broadcastIntent = new Intent("uk.ac.shef.oak.ActivityRecognition.RestartSensor");
        sendBroadcast(broadcastIntent);
        hasStarted = false;
        stoptimertask();
    }

    private Timer timer;
    private TimerTask timerTask;
    long oldTime=0;

    public void startTimer() {
        hasStarted = true;
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, to wake up every 1 second
        timer.schedule(timerTask, 1000, 1000);
    }


    /**
     * it sets the timer to print the counter every x seconds
     */
    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            public void run() {
                APP_name = who2();
                Timer();
                Log.i("in timer", "in timer ++++  "+ (bigTimer) + "   " + APP_name);
            }
        };
    }

    /**
     * not needed
     */
    public void stoptimertask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Get le package de l'appli au premier plan
     * @return nom de l'appli au premier plan
     */
    public String who2() {
        String nom_appli = null;
        if (Build.VERSION.SDK_INT >= 21) {
            String currentApp = null;
            UsageStatsManager usm = (UsageStatsManager) this.getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            List<UsageStats> applist = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time);
            if (applist != null && applist.size() > 0) {
                SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
                for (UsageStats usageStats : applist) {
                    mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                }
                if (mySortedMap != null && !mySortedMap.isEmpty()) {
                    currentApp = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                    try {
                        ApplicationInfo info = getPackageManager().getApplicationInfo(currentApp,0);
                        nom_appli = getPackageManager().getApplicationLabel(info).toString();
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
            //Log.e("ok", "Current App in foreground is: " + currentApp);
            return nom_appli;
        }
        else {
            ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            String mm=(manager.getRunningTasks(1).get(0)).topActivity.getPackageName();
            Log.e("ok", "Current App in foreground is: " + mm);
            return mm;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void notification(View v) {
        showNotification(this, "Test", "bienvenue", new Intent() );
    }

    /**
     * Permet d'envoyer la notification à l'utilisateur
     * @param context
     * @param title
     * @param body
     * @param intent
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void showNotification(Context context, String title, String body, Intent intent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = 1;
        String channelId = "channel-01";
        String channelName = "Channel Name";
        int importance = NotificationManager.IMPORTANCE_HIGH;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);
            notificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        mBuilder.setContentIntent(resultPendingIntent);

        notificationManager.notify(notificationId, mBuilder.build());
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void Timer() {
        KeyguardManager myKM = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
        if (myKM.inKeyguardRestrictedInputMode()) {
            //it is locked
            if(!locked){
                savedBigTimer = bigTimer;
                Log.i("Saved", "savedBigTimer vaut " + savedBigTimer);
            }
            bigTimer = 0;
            locked =true ;
        } else {
            //it is not locked
            if (locked){
                locked = false;
                bigTimer = savedBigTimer;
                Log.i("Saved", "bigTimer vaut " + bigTimer);
            }
            String currentApp = "" + who2(); // appli au premier plan
            int seuilNotif = 10; // seuil a dépasser aprés avoir changé d'appli pour envoyer la notif
            int seuilSocio = 30; // seuil a dépasser pour considérer qu'il a passé assez de temps sur une appli pour lui demander d'estimer son temps
            float seuilSave = seuilSocio / 2;


            if (currentApp.equals(prevTickApp)) {
                bigTimer++;
            } else if (!currentApp.equals(prevTickApp)) {
                if (bigTimer > seuilSocio && !demande) { // il a changé d'appli et il a passer le seuilSocio
                    demande = true;
                    prevApp = prevTickApp;
                } else if (bigTimer < seuilSocio && bigTimer > seuilSave) {
                    saveApp = prevTickApp;
                    saveTime = bigTimer;
                    bigTimer = 0;
                } else if (currentApp.equals(saveApp)) {
                    saveApp = "";
                    bigTimer = saveTime;
                } else {
                    bigTimer = 0;
                }
            }
            //pour ludo clean saveApp


            if (prevApp.equals(currentApp) && smallTimer < seuilNotif) { // il est revenu sur l'appli en moins de seuilNotif, donc on continue de compter
                demande = false;
                smallTimer = 0;
                Log.i("Ballec", "ca passe par la");
            }

            if (demande) {
                smallTimer++;
            }

            if (smallTimer > seuilNotif) {

                /*long endTime = SystemClock.elapsedRealtime();
                long elapsedMilliSeconds = endTime - startTime;
                double elapsedSeconds = elapsedMilliSeconds / 1000.0;
                int elspSec = (int) elapsedSeconds;*/

                // TODO demander à l'utilisteur avec la notif
                showNotification(getApplicationContext(), "Temps Passé", "Estimer le temps passé sur vos applications", new Intent());
                //startTime = SystemClock.elapsedRealtime();
                Log.i("in timer", "Temps passé " + bigTimer + " sur " + prevApp);

                // TODO set la BDD avec le bon ID, la bonne Appli et le bon Temps



                bigTimer = 0;
                smallTimer = 0;
                demande = false;
            }


            prevTickApp = currentApp;


        }
    }
}

