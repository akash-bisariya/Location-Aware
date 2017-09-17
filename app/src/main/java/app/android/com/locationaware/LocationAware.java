package app.android.com.locationaware;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.os.ResultReceiver;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

/**
 * Created by akash on 29/8/17.
 */

public class LocationAware {
    private static FusedLocationProviderClient mFusedLocationClient;
    private static final LocationAware ourInstance = new LocationAware();
    private Activity mActivity;
    private LocationRequest mLocationRequest;
    private Location mLocation;
    private ResultReceiver mResultReceiver;
    private LocationCallback mLocationCallback;
    private String mAddressOutput;
    private CurrentLocationCallback currentLocationCallback;

    static LocationAware getInstance(Activity activity) {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);

        return ourInstance;
    }

    private LocationAware() {
    }

    void getCurrentLocation(CurrentLocationCallback currentLocationCallback, final Activity activity) {
        mActivity =activity;
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        this.currentLocationCallback = currentLocationCallback;
        mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    mLocation = location;
                    Toast.makeText(activity, "" + mLocation.getLatitude() + " " + mLocation.getLongitude(), Toast.LENGTH_SHORT).show();
                    mResultReceiver = new ResultReceiverIntentService(new Handler());
                    startIntentService();
                }
//                else {
//                    createLocationRequest();
//                }


            }
        });
    }

    private void startIntentService() {
        Intent intent = new Intent(mActivity, ReverseGeocodingIntentService.class);
        intent.putExtra(Constants.LOCATION_RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLocation);
        mActivity.startService(intent);
    }


    private class ResultReceiverIntentService extends ResultReceiver
    {

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
                currentLocationCallback.CurrentLocation(mAddressOutput);
            }
        }
    }
}
