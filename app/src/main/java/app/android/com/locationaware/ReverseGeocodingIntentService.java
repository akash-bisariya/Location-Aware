package app.android.com.locationaware;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class ReverseGeocodingIntentService extends IntentService {

    public static final String RESULT_SUCCESS = "app.android.com.locationaware.action.resultSuccess";
    public static final String RESULT_FAILURE = "app.android.com.locationaware.action.resultFailure";
    public static final String LOCATION_DATA_EXTRA = "location_data";
    protected ResultReceiver mReceiver;

    public ReverseGeocodingIntentService() {
        super("ReverseGeocodingIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());


            Location location = intent.getExtras().getParcelable(Constants.LOCATION_DATA_EXTRA);
            mReceiver = intent.getExtras().getParcelable(Constants.LOCATION_RECEIVER);
//            mReceiver = intent.getExtras(Constants.LOCATION_RECEIVER);
            List<Address> addresses = null;

            try {
                addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                if (addresses == null || addresses.size() == 0) {
                    deliverResultToReceiver(Constants.FAILURE_RESULT, "Error");
                } else {
                    Address address = addresses.get(0);
                    ArrayList<String> addressFragments = new ArrayList<String>();

                    // Fetch the address lines using getAddressLine,
                    // join them, and send them to the thread.
                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                        addressFragments.add(address.getAddressLine(i));
                    }
                    Log.i("TAG", "address_found");
                    deliverResultToReceiver(Constants.SUCCESS_RESULT,
                            TextUtils.join(System.getProperty("line.separator"),
                                    addressFragments));
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("TAG", e.toString() + ". " +
                        "Latitude = " + location.getLatitude() +
                        ", Longitude = " +
                        location.getLongitude(), e);
            }


        }
    }


    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, message);
        mReceiver.send(resultCode, bundle);
    }


    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo(String param1, String param2) {
        // TODO: Handle action Foo
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionBaz(String param1, String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
