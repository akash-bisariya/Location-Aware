package app.android.com.locationaware;

import android.location.Location;

/**
 * Created by akash on 29/8/17.
 */

public interface CurrentLocationCallback {
     void CurrentLocation(String locationAddress, Location location);
}
