package com.example.elisa.urop2;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import static android.content.ContentValues.TAG;

public class MyService extends Service implements SensorEventListener{
    private static int incrementby = 1;
    private static boolean isRunning = false;

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;

    ArrayList<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track of all current registered clients.
    int mValue = 0; // Holds last value set by a client.
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_SET_INT_VALUE = 3;
    static final int MSG_SET_FLOAT_VALUE = 4;
    static final int MSG_SET_STRING_VALUE = 5;
    final Messenger mMessenger = new Messenger(new IncomingHandler()); // Target we publish for clients to send messages to IncomingHandler.


    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
    class IncomingHandler extends Handler { // Handler of incoming messages from clients.
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_SET_INT_VALUE:
                    incrementby = msg.arg1;
                    break;
                case MSG_SET_FLOAT_VALUE:
                    incrementby = msg.arg2;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    private void sendMessageToUI(int intvaluetosend, int floatvaluetosend) {

        int i = 1;
        try {
            // Send data as an Integer
            mClients.get(i).send(Message.obtain(null, MSG_SET_INT_VALUE, intvaluetosend, 0));
            mClients.get(i).send(Message.obtain(null, MSG_SET_INT_VALUE, floatvaluetosend, 0));

            //Send data as a String
            Bundle b = new Bundle();
            b.putString("flt1", "ab" + intvaluetosend + "cd" + floatvaluetosend + "ed");
            Message msg = Message.obtain(null, MSG_SET_STRING_VALUE);
            msg.setData(b);
            mClients.get(i).send(msg);

        }
        catch (RemoteException e) {
            // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
            mClients.remove(i);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("MyService", "Service Started.");
        isRunning = true;

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("MyService", "Received start id " + startId + ": " + intent);
        return START_STICKY; // run until explicitly stopped.
    }

    private long lastUpdate = 0;
    private int time = 0;
    private int duration = 0;
    public float max_acc = 0;
    public String max_accl;
    private float last_x, last_y, last_z, last_mag_acceleration;
    // sets the threshold of how sensitive you want the app to be to movement
    private static final int SHAKE_THRESHOLD = 600;
    Messenger replyMessanger;
    final static int MESSAGE = 1;

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            Log.d(TAG, "Inside onSensorChanged");
            // to take in the three co-ordinates of the position of the phone
            // x = horizontal movement of the phone
            // y = vertical movement of the phone
            // z = forward/backwards movement of the phone
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            float acceleration = (float) Math.sqrt((x*x)+(y*y)+(z*z));


            // constantly moving so to ensure it's not reading all the time set it to only
            // take in another reading if 100ms have gone by
            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {

                    // take in readings every 10ms if acc is above threshold
                    if ((curTime - lastUpdate) > 10) {
                        time++;
                        Log.d(TAG, "Recorded speed above threshold");
                        last_x = x;
                        last_y = y;
                        last_z = z;
                        last_mag_acceleration = acceleration;

                        //Assigns the new acceleration as maximum acceleration if the new one is higher
                        max_acc = Math.max (last_mag_acceleration, max_acc);

                        lastUpdate = curTime;
                    }
                    duration = time*10;
                    int max_accl = (int) max_acc*100;

                    sendMessageToUI(duration, max_accl);
                }
            }
        }
    }

    public static boolean isRunning()
    {
        return isRunning;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("MyService", "Service Stopped.");
        isRunning = false;
    }
}