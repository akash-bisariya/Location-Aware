package app.android.com.locationaware;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.os.ResultReceiver;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;


/**
 * Created by akash on 29/8/17.
 */

public class LocationAware {
    private FusedLocationProviderClient mFusedLocationClient;
    private static LocationAware ourInstance = new LocationAware();
    private LocationRequest mLocationRequest;
    private Location mLocation;
    private ResultReceiver mResultReceiver;
    private LocationCallback mLocationCallback;
    private String mAddressOutput;
    private CurrentLocationCallback currentLocationCallback;

    static LocationAware getInstance() {
        return ourInstance;
    }

    private LocationAware() {
    }


    /**
     * This method is used to get current location of user.
     *
     * @param currentLocationCallback Callback on which the current location will be passed.
     */
    void getCurrentLocation(final CurrentLocationCallback currentLocationCallback) {
        //fused client used to get current location
        if (mFusedLocationClient == null)
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient((Activity)currentLocationCallback);
        if (ActivityCompat.checkSelfPermission((Activity)currentLocationCallback, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission((Activity)currentLocationCallback, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        this.currentLocationCallback = currentLocationCallback;
        mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    mLocation = location;
                    Toast.makeText((Activity)currentLocationCallback, "" + mLocation.getLatitude() + " " + mLocation.getLongitude(), Toast.LENGTH_SHORT).show();
                    mResultReceiver = new ResultReceiverIntentService(new Handler());
                    startIntentService((Activity)currentLocationCallback);
                }


            }
        });
    }

    /**
     * Method to get location updates periodically.
     *
     * @param locationUpdateTimeinMiliSec minimum time after which location will be provided
     * @param smallestDisplacementMeters  smallest distance after which location will be updated
     */
    public void getLocationUpdates(final Activity activity, final long locationUpdateTimeinMiliSec, final float smallestDisplacementMeters) {

        if (mFusedLocationClient == null) {
            //fused client used to get current location
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);
        }

        /*
        Callback for getting location updates
         */
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                mLocation = locationResult.getLastLocation();
            }
        };

        /*
        Creating Location-Request for setting update-time, accuracy and fastest interval.
         */
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(locationUpdateTimeinMiliSec);
        mLocationRequest.setFastestInterval(locationUpdateTimeinMiliSec / 2);
        mLocationRequest.setSmallestDisplacement(smallestDisplacementMeters);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder requestBuilder = new LocationSettingsRequest.Builder();
        requestBuilder.addLocationRequest(mLocationRequest);

        /*
        Checking for the location settings
         */
        SettingsClient client = LocationServices.getSettingsClient(activity);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(requestBuilder.build());

        //registering success listener that is location setting are enabled
        task.addOnSuccessListener(activity, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                locationSettingsResponse.getLocationSettingsStates();
                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }


                // starting location updates service providing two extras in intent as -
                // 1.Location_Update_Interval and
                // 2.Smallest_Distance_Before_Location_Update

                Intent intent = new Intent(activity, LocationUpdatesService.class);
                intent.putExtra(Constants.LOCATION_UPDATE_TIME, String.valueOf(locationUpdateTimeinMiliSec));
                intent.putExtra(Constants.LOCATION_UPDATE_DISPLACEMENT, String.valueOf(smallestDisplacementMeters));
                activity.startService(intent);
                Toast.makeText(activity,"Requesting location updates",Toast.LENGTH_SHORT).show();
            }
        });

        //registering failure listener that is location setting are disabled
        task.addOnFailureListener(activity, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                    case CommonStatusCodes.RESOLUTION_REQUIRED:
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        try {
                            //starting resolution to get location settings enabled
                            resolvable.startResolutionForResult(activity, Constants.LOCATION_AWARE_SETTINGS_CHANGE_REQUEST_CODE);
                        } catch (IntentSender.SendIntentException e1) {
                            e1.printStackTrace();
                        }
                        break;

                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Toast.makeText(activity, "Location settings are not available", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void startIntentService(Activity activity) {
        Intent intent = new Intent(activity, ReverseGeocodingIntentService.class);
        intent.putExtra(Constants.LOCATION_RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLocation);
        activity.startService(intent);
    }


    /**
     * Method to remove location updates
     */
    public void removeLocationUpdates(Activity activity) {
        if (mLocationCallback != null) {
            activity.stopService(new Intent(activity, LocationUpdatesService.class));
            Toast.makeText(activity,"Location updates removed",Toast.LENGTH_SHORT).show();
        }
    }

    private class ResultReceiverIntentService extends ResultReceiver {

        /**
         * Create a new ResultReceive to receive results.  Your
         * {@link #onReceiveResult} method will be called from the thread running
         * <var>handler</var> if given, or from an arbitrary thread if null.
         *
         * @param handler
         */
        public ResultReceiverIntentService(Handler handler) {
            super(handler);

        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            // Display the address string
            // or an error message sent from the intent service.
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);


            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
                currentLocationCallback.CurrentLocation(mAddressOutput,mLocation);
            }
        }
    }
}
