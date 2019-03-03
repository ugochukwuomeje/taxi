package com.finnacle.finnaclecustomer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.github.glomadrian.materialanimatedswitch.MaterialAnimatedSwitch;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleMap mMap;

    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private Marker currentUserLocationMaker;
    private LatLng pickupLocation;
    private Boolean requestBol = false;
    private Marker pickupMarker;

    private Button mLogout, mRequest, mSettings, mHistory;

    private Boolean isLoggingOut = false;

    private String destination, requestService;
    MaterialAnimatedSwitch location_switch;
    SupportMapFragment mapFragment;

    static  final int Request_user_location_code = 99;
    private static  final int PLAY_SERVICE_RES_REQUEST = 7001;

    private LinearLayout mDriverInfo;
    private ImageView mDriverProfileImage;

    private TextView mDriverName, mDriverCar, mDriverPhone;
    LatLng destinationLatLng;
    RadioGroup mRadioGroup;

    private RatingBar mRatingBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            checkUserLocationPermission();
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mDriverInfo  = (LinearLayout) findViewById(R.id.driverinfo);

        mDriverProfileImage = (ImageView) findViewById(R.id.driverProfileImage);


        mDriverName = (TextView) findViewById(R.id.driverName);
        mDriverPhone = (TextView) findViewById(R.id.driverPhone);
        mDriverCar = (TextView) findViewById(R.id.driverCar);

        mRatingBar = (RatingBar) findViewById(R.id.ratingBar);

        mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        mRadioGroup.check(R.id.UberX);

        destinationLatLng = new LatLng(0.0, 0.0);

        ///////////////////////////this section is for logout
        mLogout = findViewById(R.id.logout);
        mRequest = findViewById(R.id.request);
        mSettings = findViewById(R.id.settings);
        mHistory = findViewById(R.id.history);
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                isLoggingOut = true;

                disConnectDriver();

                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(CustomerMapActivity.this, CustomerSettingsActivity.class);
                startActivity(intent);
                finish();
                return;

            }
        });

        mHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(CustomerMapActivity.this, HistoryActivity.class);
                intent.putExtra("customerOrDriver", "Customers");
                startActivity(intent);
                return;

            }
        });

        ///////////////////////////////this section is for making request from the customer for driver
        mRequest = findViewById(R.id.request);
        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(requestBol){

                    endRide();


                }else{

                    int selectedId  = mRadioGroup.getCheckedRadioButtonId();

                    final RadioButton radioButton = (RadioButton) findViewById(selectedId);

                    if(radioButton.getText()== null){

                        return;
                    }

                    requestService = radioButton.getText().toString();

                    requestBol = true;
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("CustomerRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLongitude(),mLastLocation.getLatitude()));

                    pickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup Here"));

                    mRequest.setText("Getting your drive");
                    getClosestDriver(); ///////////////this line calls the get closest driver method
                }

            }
        });

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {

                // TODO: Get info about the selected place.
                destination = place.getName().toString();
                destinationLatLng = place.getLatLng();

            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.

            }
        });
    }

    private void endRide(){

        requestBol = false;
        geoQuery.removeAllListeners();
        driverLoationRef.removeEventListener(driverLocationRefListener);
        driveHasEndedRef.removeEventListener(driveHasEndedRefListener);

        if(driverFoundID != null){
            DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("CustomerRequest");
            driverRef.removeValue();
            driverFoundID = null;
        }
        driverFound = false;
        radius = 1;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);

        //////////////////////////////////////////////////this point removes marker
        if(pickupMarker != null){

            pickupMarker.remove();
        }
        if(mDrivermaker != null){
            mDrivermaker.remove();
        }
        mRequest.setText("call Uber");
        mDriverInfo.setVisibility(View.GONE);
        mDriverName.setText("");
        mDriverPhone.setText("");
        mDriverCar.setText("");
        mDriverProfileImage.setImageResource(R.mipmap.user);
    }

    /////////////////////////////////////////this function gets the closest driver and creates the child the driver must pick
    private int radius = 1;
    private Boolean driverFound  = false;
    private String driverFoundID;
    GeoQuery geoQuery;
    private void getClosestDriver() {

        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailabe");
        GeoFire geoFire = new GeoFire(driverLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {

            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverFound && requestBol){
                    DatabaseReference mCustomerDatabas = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(key);
                    mCustomerDatabas.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                                Map<String, Object> driverMap = (Map<String, Object>) dataSnapshot.getValue();

                                if(driverFound){
                                    return;
                                }
                                ////////////////////////////////////this line checks whether the driver service matches the request
                                if(driverMap.get("service").equals(requestService)){

                                    driverFound = true;
                                    driverFoundID = dataSnapshot.getKey();

                                    DatabaseReference driverRef =  FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("customerRequest");
                                    String customer = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    HashMap map = new HashMap();
                                    map.put("customerRideId", customer);
                                    map.put("destination", destination);
                                    map.put("destinationLat", destinationLatLng.latitude);
                                    map.put("destinationLng", destinationLatLng.longitude);
                                    driverRef.updateChildren(map);///////////////////////////this line add the customer the driver must pcik up

                                    getDriverLocation();
                                    getDriverInfo();
                                    getHasRideEnded();
                                    mRequest.setText("Looking for Driver Location...");
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });


                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

                if(!driverFound)
                {
                    radius++;
                    getClosestDriver();
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {


            }
        });
    }

    private DatabaseReference driveHasEndedRef;
    private ValueEventListener driveHasEndedRefListener;
    private void getHasRideEnded(){

        driveHasEndedRef =  FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("customerRequest").child("customerRideId");
        driveHasEndedRefListener = driveHasEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ///////////////////////////////////////////////////////// this section removes the driver from working into available driver
                if (dataSnapshot.exists()) {


                }else{

                    endRide();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    private Marker mDrivermaker;
    private DatabaseReference driverLoationRef;
    private ValueEventListener driverLocationRefListener;


    private void getDriverLocation() {

        //////////////////////////////////////////////////////////////this function is called everytime the driver changes location
        driverLoationRef = FirebaseDatabase.getInstance().getReference().child("driverWorking").child(driverFoundID).child("l");
        driverLocationRefListener = driverLoationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && requestBol){

                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    mRequest.setText("Driver found");

                    if(map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1) != null){
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatLng =  new LatLng(locationLat, locationLng);
                    if(mDrivermaker != null){
                        mDrivermaker.remove();
                    }

                    Location loc1 = new Location("");
                    loc1.setLatitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);

                    Location loc2 = new Location(" ");
                    loc2.setLatitude(driverLatLng.latitude);
                    loc2.setLongitude(driverLatLng.longitude);

                    float distance = loc1.distanceTo(loc2);

                    if(distance < 100){

                        mRequest.setText("Driver is Here");
                    }else{

                        mRequest.setText("driver Found: "+ String.valueOf(distance));
                    }

                    mDrivermaker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("your driver").icon(BitmapDescriptorFactory.fromResource(R.mipmap.car)));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){

            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

    }

    public boolean checkUserLocationPermission(){

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Request_user_location_code);
            }else {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Request_user_location_code);
            }
            return false;
        }else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch(requestCode){
            case Request_user_location_code:

                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

                        if(mGoogleApiClient == null){

                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                }
                else{

                    Toast.makeText(this, "Permission Denied...", Toast.LENGTH_SHORT).show();
                }

                return;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest, this);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient  = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {


        mLastLocation = location;
        if(currentUserLocationMaker != null){

            currentUserLocationMaker.remove();
        }

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("your location");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        currentUserLocationMaker = mMap.addMarker(markerOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(19));

       /* if(mGoogleApiClient != null){
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }*/

    }

    private void disConnectDriver(){

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable");

        GeoFire geoFireAvailable = new GeoFire(refAvailable);
        geoFireAvailable.removeLocation(userId);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(!isLoggingOut)
        {
            disConnectDriver();
        }
    }

    private void getDriverInfo(){

        mDriverInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabse = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);

        mCustomerDatabse.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()&& dataSnapshot.getChildrenCount()>0){

                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name")!=null){
                        mDriverName.setText(map.get("name").toString());

                    }
                    if(map.get("phone")!=null){
                        mDriverPhone.setText(map.get("Phone").toString());

                    }
                    if(map.get("car")!=null){
                        mDriverCar .setText(map.get("car").toString());

                    }
                    if(map.get("profileImageUrl")!=null){

                        Glide.with(getApplicationContext()).load( map.get("profileImageUrl").toString()).into(mDriverProfileImage);
                    }

                    int ratingSum = 0;
                    float ratingTotal = 0;
                    float ratingAvg = 0;
                    for(DataSnapshot child: dataSnapshot.child("rating").getChildren())
                    {
                        ratingSum = ratingSum + Integer.valueOf(child.getValue().toString());
                        ratingTotal++;
                    }
                    if(ratingTotal != 0){
                        ratingAvg = ratingSum/ratingTotal;
                        mRatingBar.setRating(ratingAvg);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
