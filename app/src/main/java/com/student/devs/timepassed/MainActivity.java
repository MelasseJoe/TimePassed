package com.student.devs.timepassed;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.student.devs.timepassed.BDD.CSVWriter;
import com.student.devs.timepassed.BDD.MyBDD;
import com.student.devs.timepassed.BackgroundServices.AlarmReceiver;
import com.student.devs.timepassed.BackgroundServices.SensorService;
import com.student.devs.timepassed.Objets.Application;

import java.io.File;
import java.io.FileWriter;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    Intent mServiceIntent;
    TimePicker alarmTimePicker;
    PendingIntent pendingIntent;
    AlarmManager alarmManager;

    private SensorService mSensorService;

    Context ctx;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.arnaud_background);

        ///////////////////////////////////////////PERMISSIONS/////////////////////////////////////////////
        /*if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.FOREGROUND_SERVICE}, 50);
        }*/
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 50);
        }
        ///////////////////////////////////////////PERMISSIONS/////////////////////////////////////////////


        ///////////////////////////////////////////Initialisation/////////////////////////////////////////////
        ctx = this;
        mSensorService = new SensorService(getCtx());
        mServiceIntent = new Intent(getCtx(), mSensorService.getClass());

        alarmTimePicker = (TimePicker) findViewById(R.id.timePicker);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        //TODO try to open sinon créer
        MyBDD database = new MyBDD(this);
        ///////////////////////////////////////////Initialisation/////////////////////////////////////////////


        //start timer
        if (!isMyServiceRunning(mSensorService.getClass())) {
            startService(mServiceIntent);
        }

        //convert database --> csv
        exportDB(database);
    }

    public Context getCtx() {
        return ctx;
    }


    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }
        Log.i ("isMyServiceRunning?", false+"");
        return false;
    }

    @Override
    protected void onDestroy() {
        stopService(mServiceIntent);
        Log.i("MAINACT", "onDestroy!");
        super.onDestroy();
    }

    /**
     * Bouton pour ouvrir les settings et autoriser l'appli à espionner
     * @param view
     */
    public void setting(View view)  {
        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void OnToggleClicked(View view)
    {
        long time;
        if (((ToggleButton) view).isChecked())
        {
            Toast.makeText(MainActivity.this, "ALARM ON", Toast.LENGTH_SHORT).show();
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, alarmTimePicker.getCurrentHour());
            calendar.set(Calendar.MINUTE, alarmTimePicker.getCurrentMinute());

            Intent intent = new Intent(this, AlarmReceiver.class);
            pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

            time=(calendar.getTimeInMillis()-(calendar.getTimeInMillis()%60000));
            if(System.currentTimeMillis()>time)
            {
                if (calendar.AM_PM == 0)
                    time = time + (1000*60*60*12);
                else
                    time = time + (1000*60*60*24);
            }
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent);
        }
        else
        {
            alarmManager.cancel(pendingIntent);
            Toast.makeText(MainActivity.this, "ALARM OFF", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Convert database --> CSV
     * @param database
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void exportDB(MyBDD database) {

        ///////////////////////////test////////////////////////////
        Application applic= new Application();
        database.addAppli(applic);
        String test = database.getTABLE_Application();
        Toast.makeText(this, test, Toast.LENGTH_LONG).show();
        ///////////////////////////test/////////////////////////

        File[] SDCARD = this.getExternalFilesDirs("");
        File mFile = new File(SDCARD[0] + "/" + "Timepassed");
        mFile.mkdir();


        //TODO nommer le csv par la date du jour
        File file = new File(mFile, "csvname.csv");
        try
        {
            file.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            SQLiteDatabase db = database.getReadableDatabase();
            Cursor curCSV = db.rawQuery("SELECT * FROM Applications",null);
            csvWrite.writeNext(curCSV.getColumnNames());
            while(curCSV.moveToNext())
            {
                //Which column you want to export
                String arrStr[] ={curCSV.getString(0),curCSV.getString(1), curCSV.getString(2), curCSV.getString(3), curCSV.getString(4), curCSV.getString(5)};
                csvWrite.writeNext(arrStr);
            }
            csvWrite.close();  // curCSV.close();
        }
        catch(Exception sqlEx)
        {
            Log.e("MainActivity", sqlEx.getMessage(), sqlEx);
        }
    }


    // Create and show a TimePickerDialog when click button.
    private void showTimePickerDialog()
    {

        // Get open TimePickerDialog button.
        Button timePickerDialogButton = (Button)findViewById(R.id.timePickerDialogButton);
        timePickerDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Create a new OnTimeSetListener instance. This listener will be invoked when user click ok button in TimePickerDialog.
                TimePickerDialog.OnTimeSetListener onTimeSetListener = new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                        StringBuffer strBuf = new StringBuffer();
                        strBuf.append("You select time is ");
                        strBuf.append(hour);
                        strBuf.append(":");
                        strBuf.append(minute);

                        TextView timePickerValueTextView = (TextView)findViewById(R.id.timePickerValue);
                        timePickerValueTextView.setText(strBuf.toString());
                    }
                };

                Calendar now = Calendar.getInstance();
                int hour = now.get(java.util.Calendar.HOUR_OF_DAY);
                int minute = now.get(java.util.Calendar.MINUTE);

                // Whether show time in 24 hour format or not.
                boolean is24Hour = true;

                //TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this, onTimeSetListener, hour, minute, is24Hour);
                TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this, android.R.style.Theme_Holo_Light_Dialog, onTimeSetListener, hour, minute, is24Hour);
                timePickerDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);


                //timePickerDialog.setIcon(R.drawable.if_snowman);
                timePickerDialog.setTitle("Please select time :");
                timePickerDialog.updateTime(0,0);
                timePickerDialog.show();
            }
        });
    }
}
