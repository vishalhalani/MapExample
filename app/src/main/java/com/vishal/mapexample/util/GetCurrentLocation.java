package com.vishal.mapexample.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;

import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.vishal.mapexample.R;
import com.vishal.mapexample.activity.MainActivity;
import com.vishal.mapexample.interfaces.OnSetCurrentLocation;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by vishal.halani on 30-Mar-18.
 * Project Name MapExample
 */

public class GetCurrentLocation implements EasyPermissions.PermissionCallbacks,  GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public static final int RC_LOCATION_PERM = 124;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private final static int PLAY_SERVICES_REQUEST = 1000;
    private FusedLocationProviderClient mFusedLocationClient;
    private Activity mContext;
//    private LocationManager locationManager;

    //    private Location mLastLocation;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private OnSetCurrentLocation mlistner;
    private static final String TAG = "GetCurrentLocation";
    private static final String[] LOCATION =
            {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    private GoogleApiClient mGoogleApiClient;

    public GetCurrentLocation(final Activity mContext, final OnSetCurrentLocation mlistner) {
        this.mContext = mContext;
        this.mlistner = mlistner;
//        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    // ...
//                    mlistner.currentLocation(new LatLng(location.getLatitude(), location.getLongitude()));
                }
            }

            ;
        };

//        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this); //You can also use LocationManager.GPS_PROVIDER and LocationManager.PASSIVE_PROVIDER
        if (checkPlayServices()) {
            initGoogleAPIClient();//Init Google API Client
        }

    }

    private void checkPermissions() {

        if (Build.VERSION.SDK_INT >= 23) {
            locationTask();
        } else
            showSettingDialog();

    }


    /* Initiate Google API Client  */
    private synchronized void initGoogleAPIClient() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);
        //Without Google API Client Auto Location Dialog will not work
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)

                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    public boolean hasLocationPermissions() {
        return EasyPermissions.hasPermissions(mContext, LOCATION);
    }

    /**
     * Method to verify google play services on the device
     */

    private boolean checkPlayServices() {

        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();

        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(mContext);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(mContext, resultCode,
                        PLAY_SERVICES_REQUEST).show();
            } else {
                Toast.makeText(mContext.getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                mContext.finish();
            }
            return false;
        }
        return true;
    }

    public boolean isPlayServiceAvailable() {
        return checkPlayServices();
    }

    public void removeLocationUpdates() {
        //stop location updates when Activity is no longer active
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.getFusedLocationProviderClient(mContext).removeLocationUpdates(new LocationCallback());
        }
    }


    @AfterPermissionGranted(RC_LOCATION_PERM)
    public void locationTask() {
        if (hasLocationPermissions()) {
            // Have permissions, do the thing!

            showSettingDialog();
        } else {
            // Ask for both permissions
            EasyPermissions.requestPermissions(
                    mContext,
                    mContext.getString(R.string.rationale_location),
                    RC_LOCATION_PERM,
                    LOCATION);
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
        switch (requestCode) {
            case RC_LOCATION_PERM: {
                // If request is cancelled, the result arrays are empty.

                    //If permission granted show location dialog if APIClient is not null

                    if (mGoogleApiClient == null) {
                        if (checkPlayServices()) {
                            initGoogleAPIClient();
                        }

                        showSettingDialog();
                    } else
                        showSettingDialog();




            }


        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());
        if (EasyPermissions.somePermissionPermanentlyDenied(mContext, perms)) {
            new AppSettingsDialog.Builder(mContext).build().show();
        }
    }

    public Address getAddress(double latitude, double longitude) {
        if(latitude != -1 && latitude != 0.0 && longitude != -1 && longitude != 0.0)
        {
            Geocoder geocoder;
            List<Address> addresses;
            geocoder = new Geocoder(mContext, Locale.getDefault());

            try {
                addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                return addresses.get(0);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        return null;

    }


    public String getAddress(LatLng latLng) {

        Address locationAddress = getAddress(latLng.latitude, latLng.longitude);

        if (locationAddress != null) {
            String address = locationAddress.getAddressLine(0);
            String address1 = locationAddress.getAddressLine(1);
            String city = locationAddress.getLocality();
//            String state = locationAddress.getAdminArea();
//            String country = locationAddress.getCountryName();
//            String postalCode = locationAddress.getPostalCode();

            String currentLocation;

            if (!TextUtils.isEmpty(address)) {
                currentLocation = address;

                if (!TextUtils.isEmpty(address1))
                    currentLocation += "\n" + address1;

                if (!TextUtils.isEmpty(city)) {
                    currentLocation += "\n" + city;

//                    if (!TextUtils.isEmpty(postalCode))
//                        currentLocation += " - " + postalCode;
                } else {
//                    if (!TextUtils.isEmpty(postalCode))
//                        currentLocation += "\n" + postalCode;
                }

//                if (!TextUtils.isEmpty(state))
//                    currentLocation += "\n" + state;
//
//                if (!TextUtils.isEmpty(country))
//                    currentLocation += "\n" + country;

//                tvEmpty.setVisibility(View.GONE);
//                tvAddress.setText(currentLocation);
//                tvAddress.setVisibility(View.VISIBLE);
//
//                if(!btnProceed.isEnabled())
//                    btnProceed.setEnabled(true);
                return currentLocation;


            }

        }
        return "";

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RC_LOCATION_PERM: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults.length > 0
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    //If permission granted show location dialog if APIClient is not null

                    if (mGoogleApiClient == null) {
                        if (checkPlayServices()) {
                            initGoogleAPIClient();
                        }

                        showSettingDialog();
                    } else
                        showSettingDialog();


                } else {
//                    updateGPSStatus("Location Permission denied.");
                    Toast.makeText(mContext, "Location Permission denied.", Toast.LENGTH_SHORT).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }


        }
    }

    /* Show Location Access Dialog */
    private void showSettingDialog() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);//Setting priotity of Location request to high
        locationRequest.setInterval(30 * 1000);
        locationRequest.setFastestInterval(5 * 1000);//5 sec Time interval for location update


        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true); //this is the key ingredient to show dialog always when GPS is off

//        PendingResult<LocationSettingsResult> result =
//                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        final Task<LocationSettingsResponse> task =
                LocationServices.getSettingsClient(mContext).checkLocationSettings(builder.build());


        task.addOnSuccessListener(mContext, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                // ...
                getLocation();
            }
        });

        task.addOnFailureListener(mContext, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(mContext,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                        Log.e(TAG, sendEx.getMessage());
                    }
                }
            }
        });


//        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
//            @Override
//            public void onComplete(Task<LocationSettingsResponse> task) {
//                try {
//                    LocationSettingsResponse response = task.getResult(ApiException.class);
//                    // All location settings are satisfied. The client can initialize location
//
//                    final LocationSettingsStates state = response.getLocationSettingsStates();
//                    // requests here.
//                    getLocation();
//
//                } catch (ApiException exception) {
//                    switch (exception.getStatusCode()) {
//                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
//                            // Location settings are not satisfied. But could be fixed by showing the
//                            // user a dialog.
//                            try {
//                                // Cast to a resolvable exception.
//                                ResolvableApiException resolvable = (ResolvableApiException) exception;
//                                // Show the dialog by calling startResolutionForResult(),
//                                // and check the result in onActivityResult().
//                                resolvable.startResolutionForResult(
//                                        mContext,
//                                        REQUEST_CHECK_SETTINGS);
//                            } catch (IntentSender.SendIntentException e) {
//                                // Ignore the error.
//                            } catch (ClassCastException e) {
//                                // Ignore, should be an impossible error.
//                            }
//                            break;
//                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
//                            // Location settings are not satisfied. However, we have no way to fix the
//                            // settings so we won't show the dialog.
//
//                            break;
//                    }
//                }
//            }
//        });

//        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
//            @Override
//            public void onResult(LocationSettingsResult result) {
//                final Status status = result.getStatus();
//                final LocationSettingsStates state = result.getLocationSettingsStates();
//                switch (status.getStatusCode()) {
//                    case LocationSettingsStatusCodes.SUCCESS:
//                        // All location settings are satisfied. The client can initialize location
//                        // requests here.
////                        updateGPSStatus("GPS is Enabled in your device");
//                        // All required changes were successfully made
//                        getLocation();
//                        break;
//                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
//                        // Location settings are not satisfied. But could be fixed by showing the user
//                        // a dialog.
//                        try {
//                            // Show the dialog by calling startResolutionForResult(),
//                            // and check the result in onActivityResult().
//                            status.startResolutionForResult(mContext, REQUEST_CHECK_SETTINGS);
//                        } catch (IntentSender.SendIntentException e) {
//                            e.printStackTrace();
//                            // Ignore the error.
//                        }
//                        break;
//                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
//                        // Location settings are not satisfied. However, we have no way to fix the
//                        // settings so we won't show the dialog.
//                        break;
//                }
//            }
//        });
    }


    /**
     * Method to display the location on UI
     */

    private void getLocation() {


        try {

            mFusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    null /* Looper */);

            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(mContext, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations, this can be null.
                            if (location != null && mlistner != null) {
                                // Logic to handle location object
//                                mLastLocation = location;
                                Log.i(TAG, "LATITUDE=>" + location.getLatitude());
                                Log.i(TAG, "LONGITUDE=>" + location.getLongitude());
                                mlistner.currentLocation(new LatLng(location.getLatitude(), location.getLongitude()));

                            }
                        }
                    });
//                mLastLocation = LocationServices.FusedLocationApi
//                        .getLastLocation(mGoogleApiClient);
        } catch (SecurityException e) {
            e.printStackTrace();
        }


    }


    /* Broadcast receiver to check status of GPS */
    private BroadcastReceiver gpsLocationReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            //If Action is Location
            if (intent.getAction().matches(MainActivity.BROADCAST_ACTION)) {
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                //Check if GPS is turned ON or OFF
                if (locationManager != null) {
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        Log.e("About GPS", "GPS is Enabled in your device");
//                        updateGPSStatus("GPS is Enabled in your device");
                        getLocation();
                    } else {
                        //If GPS turned OFF show Location Dialog
                        new Handler().postDelayed(sendUpdatesToUI, 10);
                        // showSettingDialog();
//                        updateGPSStatus("GPS is Disabled in your device");
                        Log.e("About GPS", "GPS is Disabled in your device");
                    }
                }

            }
        }
    };




    public void connectGoogleApiClient() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
            if (hasLocationPermissions()) {

               showSettingDialog();
            } else {
                return;
            }


        }
    }

    public void stopGoogleApiClient() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }


    public BroadcastReceiver getGPSLocationReciever() {
        return gpsLocationReceiver;
    }


    //Run on UI
    private Runnable sendUpdatesToUI = new Runnable() {
        public void run() {
            showSettingDialog();
        }
    };

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // Once connected with google api, get the location
        checkPermissions(); // check permission

    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }

//    @Override
//    public void onLocationChanged(Location location) {
//
//        mlistner.currentLocation(new LatLng(location.getLatitude(), location.getLongitude()));
//
//    }
}
