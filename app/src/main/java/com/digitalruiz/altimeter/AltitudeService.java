package com.digitalruiz.altimeter;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Set;

public class AltitudeService extends Service{

    private WindowManager mWindowManager;
    private View mAltitudeHeadView;
    private final String TAG = "GPS";
    private double mslAltitude;
    private double speed;
    private double heading;
    private TextView altitudeText;
    private TextView speedText;
    private TextView headingText;
    private TextView angleText;
    private Button getAltitudeButton;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private GnssStatus.Callback statusCallBack;
    private OnNmeaMessageListener messageListener;
    private String units;
    private long miliSeconds;
    private ArrayList dataAltitude = new ArrayList();
    private ArrayList dataSpeed = new ArrayList();

    public AltitudeService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Inflate the chat head layout we created
        mAltitudeHeadView = LayoutInflater.from(this).inflate(R.layout.activity_altitude_overlay, null);

        //Add the view to the window.

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        //Specify the chat head position
        params.gravity = Gravity.TOP | Gravity.LEFT;        //Initially view will be added to top-left corner
        params.x = 0;
        params.y = 100;

        //Add the view to the window
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mAltitudeHeadView, params);

     //   Set the close button.
        ImageView closeButton = (ImageView) mAltitudeHeadView.findViewById(R.id.close_btn);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //close the service and remove the chat head from the window
                stopSelf();
            }
        });

        //Drag and move chat head using user's touch action.
        final TextView chatHeadImage = (TextView) mAltitudeHeadView.findViewById(R.id.textViewAltitude);

        chatHeadImage.setOnTouchListener(new View.OnTouchListener() {
            private int lastAction;
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        //remember the initial position.
                        initialX = params.x;
                        initialY = params.y;

                        //get the touch location
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();

                        lastAction = event.getAction();
                        return true;
                    case MotionEvent.ACTION_UP:
                        //As we implemented on touch listener with ACTION_MOVE,
                        //we have to check if the previous action was ACTION_DOWN
                        //to identify if the user clicked the view or not.
                        if (lastAction == MotionEvent.ACTION_DOWN) {
                            //Open the chat conversation click.
                            Intent intent = new Intent(AltitudeService.this, AltitudeActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);

                            //close the service and remove the chat heads
                            stopSelf();
                        }
                        lastAction = event.getAction();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        //Calculate the X and Y coordinates of the view.
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);

                        //Update the layout with new X & Y coordinate
                        mWindowManager.updateViewLayout(mAltitudeHeadView, params);
                        lastAction = event.getAction();
                        return true;
                }
                return false;
            }
        });

        altitudeText = mAltitudeHeadView.findViewById(R.id.textViewAltitude);
        speedText = mAltitudeHeadView.findViewById(R.id.textViewSpeed);
        headingText = mAltitudeHeadView.findViewById(R.id.textViewHeading);
        angleText = mAltitudeHeadView.findViewById(R.id.textViewAngle);
        getAltitudeButton = mAltitudeHeadView.findViewById(R.id.buttonGetAltitude);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.v(TAG, "Location update: " + location);
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
        };

        statusCallBack = new GnssStatus.Callback() {
            @Override
            public void onStarted() {
                super.onStarted();
            }

            @Override
            public void onStopped() {
                super.onStopped();
            }

            @Override
            public void onFirstFix(int ttffMillis) {
                super.onFirstFix(ttffMillis);
            }

            @Override
            public void onSatelliteStatusChanged(GnssStatus status) {
                super.onSatelliteStatusChanged(status);
            }
        };

        messageListener = new OnNmeaMessageListener() {
            @Override
            public void onNmeaMessage(String message, long timestamp) {
                Log.v(TAG, "NMEA: " + message);
                parseNmeaString(message);
            }
        };
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
           // ActivityCompat.requestPermissions(this.mAltitudeHeadView, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        locationManager.registerGnssStatusCallback(statusCallBack);
        locationManager.addNmeaListener(messageListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAltitudeHeadView != null) mWindowManager.removeView(mAltitudeHeadView);
    }

    private void parseNmeaString(String line) {
        //  units = preferences.getString("metric", "test");
        // Log.v(TAG, "Preferences " + units);
        if (line.startsWith("$")) {
            String[] tokens = line.split(",");
            String type = tokens[0];

            if (dataAltitude.size() != 2) {
                ArrayList dataEntryAltitude = new ArrayList();
                dataEntryAltitude.add(miliSeconds);
                dataEntryAltitude.add(0d);
                dataAltitude.add(dataEntryAltitude);
                dataAltitude.add(dataEntryAltitude);
            }

            if (dataSpeed.size() != 2) {
                ArrayList dataEntrySpeed = new ArrayList();
                dataEntrySpeed.add(miliSeconds);
                dataEntrySpeed.add(0d);
                dataSpeed.add(dataEntrySpeed);
                dataSpeed.add(dataEntrySpeed);
            }


            // Parse altitude above sea level, Detailed description of NMEA string here http://aprs.gids.nl/nmea/#gga
            if (type.startsWith("$GNGGA")) {
                if (!tokens[9].isEmpty()) {
                    mslAltitude = Double.parseDouble(tokens[9]);
                    altitudeText.setText(Double.toString(mslAltitude));
                    Log.v(TAG, "MSL Altitude " + mslAltitude);
                    Log.v(TAG, "data altitude" + dataAltitude);
                    miliSeconds = System.currentTimeMillis();

                    ArrayList dataEntry = new ArrayList();
                    dataEntry.add(miliSeconds);
                    dataEntry.add(mslAltitude);
                    dataAltitude.add(dataEntry);
                    dataAltitude.remove(0);


                    //calculate height gain or lost (opposite side in triangle)
                    ArrayList entryOneAltitude = (ArrayList) dataAltitude.get(0);
                    ArrayList entryTwoAltitude = (ArrayList) dataAltitude.get(1);
                    Double altitudeOne = (Double) entryOneAltitude.get(1);
                    Double altitudeTwo = (Double) entryTwoAltitude.get(1);

                    Double opposite = altitudeTwo - altitudeOne;

                    Log.v(TAG, "Opposite is " + opposite);

                    //calculate distance traveled (adjacent in triangle)
                    Log.v(TAG, "dspeed " + dataSpeed);
                    ArrayList entryOneSpeed = (ArrayList) dataSpeed.get(0);
                    ArrayList entryTwoSpeed = (ArrayList) dataSpeed.get(1);
                    Double speedOne = (Double) entryOneSpeed.get(1);
                    Double speedTwo = (Double) entryTwoSpeed.get(1);
                    Log.v(TAG, "Speed One " + speedOne);
                    Double avgSpeedMeterPerHour = ((speedOne + speedTwo) / 2) * 1000;
                    Long timeOne = (Long) entryOneSpeed.get(0);
                    Long timeTwo = (Long) entryTwoSpeed.get(0);
                    Double timeDifference = timeTwo.doubleValue() - timeOne.doubleValue();
                    Double hypotenuse = (timeDifference * avgSpeedMeterPerHour) / 3600000;
                    Log.v(TAG, "Hypotenuse " + hypotenuse);

                    Double angle = 0d;
                    if (opposite > 0d && hypotenuse == 0d) {
                        angle = 0d;
                    } else if (opposite < 0d && hypotenuse == 0d) {
                        angle = 0d;
                    } else {
                        Double sin = opposite / hypotenuse;
                        Log.v(TAG, "sin " + sin);
                        Double angleRadians = Math.asin(sin);
                        Double angleDegrees = angleRadians / Math.PI * 180;
                        Log.v(TAG, "angleRadians " + angleRadians);
                        Log.v(TAG, "angleDegrees " + angleDegrees);
                        if (angleDegrees >= 0 || angleDegrees <= 0) {
                            angle = angleDegrees;
                        }

                    }
                    Log.v(TAG, "angle " + angle);
                    String angleFormat = new DecimalFormat("#.##").format(angle);
                    Log.v(TAG, "angle format " + angleFormat);
                    angleText.setText(angleFormat);

                }
            }
            // Parse speed in KM per hour nad heading (course) magnetic, Detailed descriptions of NMEA string here http://aprs.gids.nl/nmea/#vtg
            else if (type.startsWith("$GNVTG")) {
                if (!tokens[7].isEmpty()) {
                    speed = Double.parseDouble(tokens[7]);
                    speedText.setText(Double.toString(speed));
                    Log.v(TAG, "Speed " + speed);
                    Log.v(TAG, "data speed" + dataSpeed);
                    miliSeconds = System.currentTimeMillis();


                    ArrayList dataEntry = new ArrayList();
                    dataEntry.add(miliSeconds);
                    dataEntry.add(speed);
                    dataSpeed.add(dataEntry);
                    dataSpeed.remove(0);

                } else {

                }
                if (!tokens[3].isEmpty()) {
                    heading = Double.parseDouble(tokens[3]);
                    headingText.setText(Double.toString(heading));
                    Log.v(TAG, "Heading " + tokens[3]);
                } else {

                }
            }

        }
    }
}
