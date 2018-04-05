package com.vishal.mapexample.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.BounceInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.vishal.mapexample.R;
import com.vishal.mapexample.interfaces.OnSetCurrentLocation;

import com.vishal.mapexample.util.GetCurrentLocation;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, OnSetCurrentLocation, View.OnClickListener, GoogleMap.OnCameraIdleListener {
    public static final String BROADCAST_ACTION = "android.location.PROVIDERS_CHANGED";
    private static final int RC_PICKUP_LOCATION = 111;
    private static final int RC_DROP_LOCATION = 222;
    private Button btnRideNow, btnRideLater;

    private boolean isPickUpLocationEnable = true;

    private GoogleMap mMap;
    private ImageView markerImg;
    private LatLng pickupLocation, dropLocation;
    private LinearLayout pickupHeader, dropHeader;
    private TextView etPickup, etDropLocation;
    private GetCurrentLocation getCurrentLocation;
    private LinearLayout pickupLayout, dropLayout;
    private TabLayout tabLayout;
    private int tabIcons[] = {R.drawable.cycle, R.drawable.motorcycle, R.drawable.car1, R.drawable.car2, R.drawable.car3, R.drawable.helicopter};
    private String tabTitle[] = {"Bicycle", "Bike", "Mini", "Luxury", "Rent", "Airway"};
    private String tabTime[] = {"1 Min", "2 Min", "5 Min", "10 Min", "30 Min", "1 Hour"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // bind all views
        bindWidgets();


        // set click listeners
        pickupLayout.setOnClickListener(this);
        dropLayout.setOnClickListener(this);
        etPickup.setOnClickListener(this);
        etDropLocation.setOnClickListener(this);
        btnRideNow.setOnClickListener(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        getCurrentLocation = new GetCurrentLocation(MainActivity.this, this);

        if(getCurrentLocation.isPlayServiceAvailable())
        {
            getCurrentLocation.connectGoogleApiClient();
        }

        // create  tabs
        addTabs();


        registerReceiver(getCurrentLocation.getGPSLocationReciever(), new IntentFilter(BROADCAST_ACTION));//Register broadcast receiver to check the status of GPS


    }

    // add tabs to tablayout
    private void addTabs() {
        for (int i = 0; i < tabIcons.length; i++) {
            if (i == 2) {
                tabLayout.addTab(tabLayout.newTab(), true);
            } else {
                tabLayout.addTab(tabLayout.newTab(), false);
            }

        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // custom tabs
        createTabIcons();
    }

    // bind all UI widgets
    private void bindWidgets() {
        pickupLayout = (LinearLayout) findViewById(R.id.pickup_layout);
        dropLayout = (LinearLayout) findViewById(R.id.drop_layout);

        tabLayout = (TabLayout) findViewById(R.id.item_tabLayout);
        btnRideNow = (Button) findViewById(R.id.btn_ride_now);

        etPickup = (TextView) findViewById(R.id.pickup_location_et);
        etDropLocation = (TextView) findViewById(R.id.drop_location_et);


        markerImg = (ImageView) findViewById(R.id.marker_imageview);
        pickupHeader = (LinearLayout) findViewById(R.id.pickup_layout_heading);
        dropHeader = (LinearLayout) findViewById(R.id.drop_layout_heading);

    }

    // create custom tabs
    private void createTabIcons() {


        for (int i = 0; i < tabIcons.length; i++) {

            LinearLayout layout = (LinearLayout) LayoutInflater.from(MainActivity.this).inflate(R.layout.item_tab, null);

            TextView tabContent = (TextView) layout.findViewById(R.id.tabContent);
            TextView tabDuration = (TextView) layout.findViewById(R.id.tabTime);
            ImageView imageView = (ImageView) layout.findViewById(R.id.tab_icon);
            imageView.setImageResource(tabIcons[i]);
            tabContent.setText(tabTitle[i]);
            tabDuration.setText(tabTime[i]);
//            tabContent.setCompoundDrawablesWithIntrinsicBounds(tabIcons[i], 0, 0, 0);

            TabLayout.Tab tab = tabLayout.getTabAt(i);
            tab.setCustomView(layout);
        }


    }


    @Override
    protected void onResume() {

        super.onResume();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (getCurrentLocation != null) {
            getCurrentLocation.stopGoogleApiClient();
            getCurrentLocation.removeLocationUpdates();
        }
        //Unregister receiver on destroy
        if (getCurrentLocation != null && getCurrentLocation.getGPSLocationReciever() != null)
            unregisterReceiver(getCurrentLocation.getGPSLocationReciever());
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    @SuppressLint("MissingPermission")
    public void onMapReady(GoogleMap googleMap) {

        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.map_style));

            if (!success) {
                Log.e("TAG", "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("TAG", "Can't find style. Error: ", e);
        }
        mMap = googleMap;

        mMap.setBuildingsEnabled(false);  // To set 3D Views  make true

        mMap.setMinZoomPreference(10.0f);
        mMap.setMaxZoomPreference(20.0f);
//        if (getCurrentLocation != null) {
//            if (getCurrentLocation.hasLocationPermissions()) {
//                mMap.setMyLocationEnabled(true);
//            } else {
//                mMap.setMyLocationEnabled(false);
//            }
//        }


        // called when move camera animation completed
        mMap.setOnCameraIdleListener(this); // listener to get event when camera stopped moving

    }

    @Override
    public void currentLocation(LatLng currentLocation) {
        if (currentLocation != null && currentLocation.longitude != 0.0 && currentLocation.latitude != 0.0) {
            if (getCurrentLocation != null) {
                if (isPickUpLocationEnable) {
                    pickupLocation = currentLocation;
                }
//                else {
//                    dropLocation = currentLocation;
//                }

                // set camera to current location
                moveMarkerToLocation(currentLocation);
//                locationManager.removeUpdates(this);
            }
        }

    }


    /**
     * Open Search view to find places
     *
     * @param requestCode To identify request
     */
    public void findPlace(int requestCode) {
        try {
            Intent intent =
                    new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                            .build(MainActivity.this);
            startActivityForResult(intent, requestCode);
        } catch (GooglePlayServicesRepairableException e) {
            // TODO: Handle the error.
        } catch (GooglePlayServicesNotAvailableException e) {
            // TODO: Handle the error.
        }
    }

    /**
     * To Handle view on click of pickup and drop location
     *
     * @param isPickUpClick flag to identify user click
     */
    private void pickupAndDropViewHandler(final boolean isPickUpClick) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (isPickUpClick) {
                pickupLayout.setElevation(8);
//                        pickupLayout.setClipToOutline(true);
                dropLayout.setTranslationZ(0);
                pickupLayout.setTranslationZ(8);
                dropLayout.setElevation(0);
                pickupLayout.setBackground(getResources().getDrawable(R.drawable.round_corner_white_layout));
                dropLayout.setBackground(getResources().getDrawable(R.drawable.round_corner_grey_layout));
                pickupHeader.setVisibility(View.VISIBLE);

                isPickUpLocationEnable = true;
                etPickup.setClickable(true);
                etDropLocation.setClickable(false);

                markerImg.setImageDrawable(getResources().getDrawable(R.drawable.pickup_marker));

                TranslateAnimation translation;
                translation = new TranslateAnimation(0f, 0F, -50, 0f);
                translation.setStartOffset(500);
                translation.setDuration(2000);
                translation.setFillAfter(true);
                translation.setRepeatMode(Animation.ABSOLUTE);
                translation.setInterpolator(new BounceInterpolator());
                markerImg.startAnimation(translation);
                if (pickupLocation != null) {
                    moveMarkerToLocation(pickupLocation);
                }

            } else {
                isPickUpLocationEnable = false;
                dropLayout.setElevation(8);
                pickupLayout.setTranslationZ(0);
                dropHeader.setVisibility(View.VISIBLE);
                pickupHeader.setVisibility(View.GONE);
                dropLayout.setTranslationZ(8);
                dropLayout.setBackground(getResources().getDrawable(R.drawable.round_corner_white_layout));
                pickupLayout.setBackground(getResources().getDrawable(R.drawable.round_corner_grey_layout));
//                        dropLayout.setClipToOutline(true);
                etPickup.setClickable(false);
                etDropLocation.setClickable(true);
                pickupLayout.setElevation(0);
                markerImg.setImageDrawable(getResources().getDrawable(R.drawable.drop_marker));
                TranslateAnimation translation;
                translation = new TranslateAnimation(0f, 0F, -50, 0f);
                translation.setStartOffset(500);
                translation.setDuration(2000);
                translation.setFillAfter(true);
                translation.setRepeatMode(Animation.ABSOLUTE);
                translation.setInterpolator(new BounceInterpolator());
                markerImg.startAnimation(translation);

                if (dropLocation != null) {
                    moveMarkerToLocation(dropLocation);
                }
//                        pickupLayout.setClipToOutline(false);

            }
        } else {
            if (isPickUpClick) {
                isPickUpLocationEnable = true;

                pickupLayout.bringToFront();
                TranslateAnimation translation;
                translation = new TranslateAnimation(0f, 0F, -50, 0f);
                translation.setStartOffset(500);
                translation.setDuration(2000);
                translation.setFillAfter(true);
                translation.setRepeatMode(Animation.ABSOLUTE);
                translation.setInterpolator(new BounceInterpolator());
                markerImg.startAnimation(translation);
                if (pickupLocation != null) {
                    moveMarkerToLocation(pickupLocation);
                }
            } else {
                isPickUpLocationEnable = false;
                dropLayout.bringToFront();
                TranslateAnimation translation;
                translation = new TranslateAnimation(0f, 0F, -50, 0f);
                translation.setStartOffset(500);
                translation.setDuration(2000);
                translation.setFillAfter(true);
                translation.setRepeatMode(Animation.ABSOLUTE);
                translation.setInterpolator(new BounceInterpolator());
                markerImg.startAnimation(translation);
                if (dropLocation != null) {
                    moveMarkerToLocation(dropLocation);
                }


            }
        }


    }

    /**
     * Move Camera position to particular location
     *
     * @param currentLocation location latlng where camera will be moved
     */
    private void moveMarkerToLocation(LatLng currentLocation) {
        if (currentLocation != null && currentLocation.longitude != 0.0 && currentLocation.latitude != 0.0) {

            // Construct a CameraPosition focusing on Mountain View and animate the camera to that position.
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(currentLocation)      // Sets the center of the map to Mountain View
                    .zoom(18)                   // Sets the zoom
                    .bearing(90)                // Sets the orientation of the camera to east
                    .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder


            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));


        }

    }

    @Override
    protected void onPause() {
        super.onPause();


    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.pickup_layout:
//                isClickOnPickup = true;
                isPickUpLocationEnable = true;
                if (etPickup.getText().equals("")) {
                    findPlace(RC_PICKUP_LOCATION);
                } else {
                    pickupAndDropViewHandler(true);
                }


                break;
            case R.id.drop_layout:
//                isClickOnPickup = false;
                isPickUpLocationEnable = false;
                if (etDropLocation.getText().equals("")) {
                    findPlace(RC_DROP_LOCATION);
                } else {
                    pickupAndDropViewHandler(false);
                }


                break;
            case R.id.pickup_location_et:
//                isClickOnPickup = true;
//                if (isPickUpLocationEnable) {
//                    findPlace(RC_PICKUP_LOCATION);
//                } else {
//
//                    pickupAndDropViewHandler(true);
//                }
                if (etPickup.getText().equals("")) {
                    isPickUpLocationEnable = true;
                    pickupAndDropViewHandler(false);
                    findPlace(RC_PICKUP_LOCATION);

                } else {
                    if (isPickUpLocationEnable) {
                        findPlace(RC_PICKUP_LOCATION);
                    } else {

                        pickupAndDropViewHandler(false);
                    }

                }

                break;
            case R.id.drop_location_et:
//                isClickOnPickup = false;
                if (etDropLocation.getText().equals("")) {
                    isPickUpLocationEnable = false;
                    pickupAndDropViewHandler(false);
                    findPlace(RC_DROP_LOCATION);

                } else {
                    if (!isPickUpLocationEnable) {
                        findPlace(RC_DROP_LOCATION);
                    } else {

                        pickupAndDropViewHandler(false);
                    }

                }


                break;
            case R.id.btn_ride_now:
                Intent intent = new Intent(MainActivity.this, MapActivity.class);
                Bundle args = new Bundle();
                args.putParcelable("from_position", pickupLocation);
                args.putParcelable("to_position", dropLocation);
                intent.putExtra("bundle", args);
                startActivity(intent);
                break;
        }
    }

    @Override
    public void onCameraIdle() {

        LatLng currentCameraLocation = mMap.getCameraPosition().target;
        if (isPickUpLocationEnable) {


            if (pickupLocation != null && pickupLocation.latitude == currentCameraLocation.latitude && pickupLocation.longitude == currentCameraLocation.longitude) {
                pickupLocation = currentCameraLocation;
                etPickup.setText(getCurrentLocation.getAddress(pickupLocation));
//                        getCurrentLocation.getAddress(pickupLocation,new GeocoderHandler());
                moveMarkerToLocation(pickupLocation);


            } else if (pickupLocation != null && pickupLocation.latitude != currentCameraLocation.latitude && pickupLocation.longitude != currentCameraLocation.longitude) {
                pickupLocation = currentCameraLocation;
                etPickup.setText(getCurrentLocation.getAddress(pickupLocation));

//                getCurrentLocation.getAddress(pickupLocation,new GeocoderHandler());
                moveMarkerToLocation(pickupLocation);
            }

        } else {
            if (dropLocation != null && dropLocation.latitude == currentCameraLocation.latitude && dropLocation.longitude == currentCameraLocation.longitude) {
                dropLocation = currentCameraLocation;
                etDropLocation.setText(getCurrentLocation.getAddress(dropLocation));
//                getCurrentLocation.getAddress(dropLocation,new GeocoderHandler());
                moveMarkerToLocation(dropLocation);
            } else if (dropLocation != null && dropLocation.latitude != currentCameraLocation.latitude && dropLocation.longitude != currentCameraLocation.longitude) {
                dropLocation = currentCameraLocation;
                etDropLocation.setText(getCurrentLocation.getAddress(dropLocation));
//                getCurrentLocation.getAddress(dropLocation,new GeocoderHandler());
                moveMarkerToLocation(dropLocation);
            }


        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        if (getCurrentLocation != null) {
            EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, getCurrentLocation);
        }


    }

    // A place has been received; use requestCode to track the request.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RC_PICKUP_LOCATION:
                if (resultCode == RESULT_OK) {
                    Place place = PlaceAutocomplete.getPlace(this, data);
                    Log.e("Tag", "Place: " + place.getAddress());

                    etPickup.setText(place.getName() + ",\n" +
                            place.getAddress() + "\n");
                    pickupLocation = place.getLatLng();
                    moveMarkerToLocation(pickupLocation);

                } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                    Status status = PlaceAutocomplete.getStatus(this, data);
                    // TODO: Handle the error.
                    Log.e("Tag", status.getStatusMessage());

                } else if (resultCode == RESULT_CANCELED) {
                    // The user canceled the operation.
                }
                break;
            case RC_DROP_LOCATION:
                if (resultCode == RESULT_OK) {
                    Place place = PlaceAutocomplete.getPlace(this, data);
                    Log.e("Tag", "Place: " + place.getAddress());


                    etDropLocation.setText(place.getName() + ",\n" +
                            place.getAddress() + "\n");
                    dropLocation = place.getLatLng();
                    moveMarkerToLocation(dropLocation);

                } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                    Status status = PlaceAutocomplete.getStatus(this, data);
                    // TODO: Handle the error.
                    Log.e("Tag", status.getStatusMessage());

                } else if (resultCode == RESULT_CANCELED) {
                    // The user canceled the operation.
                }
                break;
        }

    }


}

