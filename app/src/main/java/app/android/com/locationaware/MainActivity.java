package app.android.com.locationaware;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderApi;
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

import org.w3c.dom.Text;

import java.security.Permissions;

public class MainActivity extends AppCompatActivity implements CurrentLocationCallback {
    public static final int LOCATION_AWARE_ACCESS_PERMISSION_REQUEST_CODE = 111111;
    private boolean mRequestingLocationUpdates = false;
    private Button btnSetLocationUpdates;
    private Button btnGetCurrentLocation;
    private Button btnRemoveLocationUpdates;
    private TextView tvLocation;
    private LocationReceiver mLocationReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnSetLocationUpdates = (Button) findViewById(R.id.btn_set_location_updates);
        btnRemoveLocationUpdates = (Button) findViewById(R.id.btn_remove_location_updates);
        btnGetCurrentLocation = (Button) findViewById(R.id.btn_get_current_location);
        tvLocation = (TextView) findViewById(R.id.tv_location);

        //initializing broadcast receiver
        mLocationReceiver = new LocationReceiver();


        btnSetLocationUpdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRequestingLocationUpdates = true;

                //calling for location updates with time interval and minimum displacement distance
                LocationAware.getInstance().getLocationUpdates(MainActivity.this, 5000, 10);
            }
        });


        btnGetCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermissionGranted()) {
                    LocationAware.getInstance().getCurrentLocation(MainActivity.this);
                }
            }
        });


        btnRemoveLocationUpdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRequestingLocationUpdates)
                    LocationAware.getInstance().removeLocationUpdates(MainActivity.this);
            }
        });
    }


    private boolean checkPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_AWARE_ACCESS_PERMISSION_REQUEST_CODE);
                return false;
            } else
                return true;

        } else
            return true;
    }


    @Override
    protected void onStart() {
        super.onStart();

        //Registering receiver for getting location updates
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocationReceiver, new IntentFilter(Constants.LOCATION_AWARE_RECEIVING_LOCATION_UPDATE));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            LocationAware.getInstance().getCurrentLocation(MainActivity.this);


        } else {

            // permission denied, boo! Disable the
            // functionality that depends on this permission.
            showSnackbar(R.string.txt_permission_denied_request, R.string.app_name,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Build intent that displays the App settings screen.
                            Intent intent = new Intent();
                            intent.setAction(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package",
                                    BuildConfig.APPLICATION_ID, null);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    });
        }
    }


    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stopping/unregistering the location receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocationReceiver);
        LocationAware.getInstance().removeLocationUpdates(this);
        mRequestingLocationUpdates = false;

    }


    @Override
    public void CurrentLocation(String locationAddress, Location location) {
        tvLocation.setText(locationAddress);
    }


    //Broadcast receiver to get location updates with defined Intent Filter
    private class LocationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (null != intent && intent.getAction().equals(Constants.LOCATION_AWARE_RECEIVING_LOCATION_UPDATE)) {

                Location location = intent.getParcelableExtra(Constants.LOCATION_LATLONG);

                tvLocation.setText("Location Changed " + location.getLatitude() + " " + location.getLongitude());
            }

        }
    }
}
