package com.kikappsmx.dondeestamicuy.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.kikappsmx.dondeestamicuy.Cuy;
import com.kikappsmx.dondeestamicuy.MapsActivity;
import com.kikappsmx.dondeestamicuy.R;

public class MyService extends Service implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final String TAG ="Tag" ;
    private static DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
    public static final String GEO_FIRE = "geoFire";
    public static final String CUYS = "cuys";
    public double lat, lng;


    GoogleApiClient mLocationClient;
    LocationRequest mLocationRequest = new LocationRequest();

    public static final String CHANNEL_ID = "Channel 1";


    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        pendingNotificacion();
        super.onCreate();
        Cuy cuy = new Cuy();
        cuy.id = String.valueOf(1);
        updateLocation(cuy);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mLocationClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        int priority = LocationRequest.PRIORITY_HIGH_ACCURACY; //by default
        mLocationRequest.setInterval(2000);
//        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(priority);
        mLocationClient.connect();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            Log.d(TAG, "== Error On onConnected() Permission not granted");
            //Permission not granted by user so cancel the further execution.

            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mLocationClient, mLocationRequest, this);

        Log.d(TAG, "Connected to Google API");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "Failed to connect to Google API");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location changed");

        if (location != null) {
            Log.d(TAG, "== location != null");
            lat = location.getLatitude();
            lng = location.getLongitude();

            //Send result to activities
//            sendMessageToUI(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()));
            Log.d(TAG, String.valueOf(location.getLatitude()) + "," + String.valueOf(location.getLongitude()));
        }
    }

    private void updateLocation(Cuy cuy) {
        reference.child(CUYS).child(cuy.id).setValue(cuy, (DatabaseError databaseError, DatabaseReference databaseReference) -> {
            GeoFire geoFire = new GeoFire(reference.child(GEO_FIRE));
            geoFire.setLocation(cuy.id, new GeoLocation(cuy.latitude, cuy.longitude), (key, error) -> {
                cuy.latitude = lat;
                cuy.longitude = lng;
                updateLocation(cuy);
            });
        });
    }


    public void pendingNotificacion() {

        // ForeGround Service Notification
        Intent notificationIntent = new Intent(this, MapsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Cuy tracker")
                .setContentText("Estas siendo reastreado por Cuy celoso")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
        startForeground(1, notification);

        // Pending Notification for Oreo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID, "Example Service Channel", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }

    }
}
