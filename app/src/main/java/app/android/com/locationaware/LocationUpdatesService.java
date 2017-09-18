package app.android.com.locationaware;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class LocationUpdatesService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private LocationRequest mLocationRequest;
    private Location mLocation;
    private GoogleApiClient mGoogleApiClient;
    private long mLocationUpdateTimeinMiliSec;
    private float mLocationSmallestDisplacementMeters;

    public LocationUpdatesService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("LocationAware", "LocationUpdatesService Started");
        mLocationUpdateTimeinMiliSec = Long.parseLong(intent.getExtras().getString(Constants.LOCATION_UPDATE_TIME));
        mLocationSmallestDisplacementMeters = Float.parseFloat(intent.getExtras().getString(Constants.LOCATION_UPDATE_DISPLACEMENT));
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        mGoogleApiClient.connect();
        return START_REDELIVER_INTENT;

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("LocationAware", "GoogleApiClient Connected");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        } else {

            //Requesting location updates using Google Fused API
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(mLocationUpdateTimeinMiliSec);
            mLocationRequest.setFastestInterval(mLocationUpdateTimeinMiliSec / 2);
            mLocationRequest.setSmallestDisplacement(mLocationSmallestDisplacementMeters);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("LocationAware", "GoogleApiClient Connection Suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("LocationAware", "GoogleApiClient Connection Failed");

    }

    @Override
    public void onDestroy() {
//      Disconnecting location updates
        super.onDestroy();
        stopSelf();
        mGoogleApiClient.disconnect();
        Log.d("LocationAware", "LocationUpdatesService Destroyed");
    }

    @Override
    public void onLocationChanged(Location location) {

        //Location Changed Callback
        //Sending Broacast to Activity
        mLocation = location;
        Log.d("LocationAware", "Got Location Changed" + mLocation);
        Intent intent = new Intent(Constants.LOCATION_AWARE_RECEIVING_LOCATION_UPDATE);
        intent.putExtra(Constants.LOCATION_LATLONG, location);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
