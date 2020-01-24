package com.digitalruiz.altimeter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    // Declaring all needed variables and components.
    private final String TAG = "GPS";
    private double mslAltitude;
    private double speed;
    private TextView altitudeText;
    private TextView speedText;
    private Button getAltitudeButton;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private GnssStatus.Callback statusCallBack;
    private OnNmeaMessageListener messageListener;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        altitudeText = findViewById(R.id.textViewAltitude);
        speedText = findViewById(R.id.textViewSpeed);
        getAltitudeButton = findViewById(R.id.buttonGetAltitude);

        getAltitudeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                altitudeText.setText("Getting altitude from GPS");
                speedText.setText("Getting speed from GPS");
            }
        });

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION},1);
        }




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

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        locationManager.registerGnssStatusCallback(statusCallBack);
        locationManager.addNmeaListener(messageListener);




    }

    private void parseNmeaString(String line) {
        if (line.startsWith("$")) {
            String[] tokens = line.split(",");
            String type = tokens[0];

            // Parse altitude above sea level, Detailed description of NMEA string here http://aprs.gids.nl/nmea/#gga
            if (type.startsWith("$GNGGA")) {
                if (!tokens[9].isEmpty()) {
                    mslAltitude = Double.parseDouble(tokens[9]);
                    altitudeText.setText(Double.toString(mslAltitude));
                    Log.v(TAG, "MSL Altitude " + mslAltitude);
                }
            }
            // Parse speed in KM per hour, Detailed descriptions of NMEA string here http://aprs.gids.nl/nmea/#vtg
            else if (type.startsWith("$GNVTG")){
                if (!tokens[7].isEmpty()){
                    speed = Double.parseDouble(tokens[7]);
                    speedText.setText(Double.toString(speed));
                    Log.v(TAG, "Speed " + speed);

                }
            }

        }
    }




}
