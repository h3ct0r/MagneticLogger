package br.ufmg.dcc.verlab.maglogger;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;


public class SensorHelper implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mAcc, mGy, mMag;
    private double accx, accy, accz;
    private double gyx, gyy, gyz;
    private double magx, magy, magz;

    private final Context context;
    private Boolean isRunning = false;

    public SensorHelper(Context context) {
        this.context = context;
        mSensorManager = (SensorManager) context.getSystemService(context.SENSOR_SERVICE);

        mAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGy = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        // Look for the right mag sensor
        mMag = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mSensorManager.registerListener(this, mAcc, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mGy, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMag, SensorManager.SENSOR_DELAY_NORMAL);
    }


    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        // The light sensor returns a single value.
        // Many sensors return 3 values, one for each axis. Sensor sensor = event.sensor;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accx = event.values[0];
            accy = event.values[1];
            accz = event.values[2];
        }else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyx = event.values[0];
            gyy = event.values[1];
            gyz = event.values[2];
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magx = event.values[0];
            magy = event.values[1];
            magz = event.values[2];
        }
    }

    public double getAccx(){return accx;}
    public double getAccy(){return accy;}
    public double getAccz(){return accz;}

    public double getGyx(){return gyx;}
    public double getGyy(){return gyy;}
    public double getGyz(){return gyz;}

    public double getMagx(){return magx;}
    public double getMagy(){return magy;}
    public double getMagz(){return magz;}
}
