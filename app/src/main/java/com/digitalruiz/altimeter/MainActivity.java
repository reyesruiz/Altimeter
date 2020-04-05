package com.digitalruiz.altimeter;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.lang.Math;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {
    // Declaring all needed variables and components.
    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;
    private final String TAG = "GPS";
    private double mslAltitude;
    private double speed;
    private double heading;
    private TextView altitudeText;
    private TextView speedText;
    private TextView headingText;
    private TextView angleText;
    private Button popUpButton;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private GnssStatus.Callback statusCallBack;
    private OnNmeaMessageListener messageListener;
    private String units;
    private long miliSeconds;
    private ArrayList dataAltitude = new ArrayList();
    private ArrayList dataSpeed = new ArrayList();


    @Override
    protected void onPause(){
        super.onPause();
        Log.v(TAG, "PAUSE");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Check if the application has draw over other apps permission or not?
        //This permission is by default available for API<23. But for API > 23
        //you have to ask for the permission in runtime.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {

            //If the draw over permission is not available open the settings screen
            //to grant the permission.
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
        }



        //Need to put an else, and then ask for location permission.

        altitudeText = findViewById(R.id.textViewAltitude);
        speedText = findViewById(R.id.textViewSpeed);
        headingText = findViewById(R.id.textViewHeading);
        angleText = findViewById(R.id.textViewAngle);
        popUpButton = findViewById(R.id.buttonPopUp);



        popUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService(new Intent(MainActivity.this, PopUpService.class));
                finish();
            }
        });

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
            //good

        } else {
            //Should not happen, since we already asked for permissions on both activities.
        }

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        locationManager.registerGnssStatusCallback(statusCallBack);
        locationManager.addNmeaListener(messageListener);


    }


    private void parseNmeaString(String line) {
      //  units = preferences.getString("metric", "test");
       // Log.v(TAG, "Preferences " + units);
        if (line.startsWith("$")) {
            String[] tokens = line.split(",");
            String type = tokens[0];

            if (dataAltitude.size() != 2){
                ArrayList dataEntryAltitude = new ArrayList();
                dataEntryAltitude.add(miliSeconds);
                dataEntryAltitude.add(0d);
                dataAltitude.add(dataEntryAltitude);
                dataAltitude.add(dataEntryAltitude);
            }

            if (dataSpeed.size() != 2){
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
                    Double avgSpeedMeterPerHour = ((speedOne + speedTwo)/2) * 1000;
                    Long timeOne = (Long) entryOneSpeed.get(0);
                    Long timeTwo = (Long) entryTwoSpeed.get(0);
                    Double timeDifference = timeTwo.doubleValue() - timeOne.doubleValue();
                    Double hypotenuse = (timeDifference * avgSpeedMeterPerHour)/3600000;
                    Log.v(TAG, "Hypotenuse " + hypotenuse);

                    Double angle = 0d;
                    if (opposite > 0d && hypotenuse == 0d  ){
                        angle = 0d;
                    }
                    else if (opposite < 0d && hypotenuse == 0d) {
                        angle = 0d;
                    }
                    else {
                        Double sin = opposite/hypotenuse;
                        Log.v(TAG, "sin " + sin);
                        Double angleRadians = Math.asin(sin);
                        Double angleDegrees = angleRadians / Math.PI * 180;
                        Log.v(TAG, "angleRadians " + angleRadians);
                        Log.v(TAG, "angleDegrees " + angleDegrees);
                        if (angleDegrees >= 0 || angleDegrees <= 0){
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
            else if (type.startsWith("$GNVTG")){
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

                }
                else {

                }
                if (!tokens[3].isEmpty()) {
                    heading = Double.parseDouble(tokens[3]);
                    headingText.setText(Double.toString(heading));
                    Log.v(TAG, "Heading " + tokens[3]);
                }
                else {

                }
            }

        }
    }




}
