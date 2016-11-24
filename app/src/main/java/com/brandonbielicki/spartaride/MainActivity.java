package com.brandonbielicki.spartaride;

import android.*;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.drive.internal.StringListResponse;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    GoogleMap map;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference fbTrips = database.getReference("trips");
    DatabaseReference fbStops = database.getReference("stops");
    private Button routesButton;
    private ImageButton toggleStopButton;
    private ArrayList<Marker> currentBusMarkers = new ArrayList();
    private ArrayList<Marker> stopMarkers = new ArrayList();
    private String route;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        route = "#";
        MobileAds.initialize(getApplicationContext(), getString(R.string.banner_ad_unit_id));
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        toggleStopButton = (ImageButton) findViewById(R.id.toggle_stop_button);
        toggleStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for(Marker item : stopMarkers) {
                    if(item.isVisible()) {
                        item.setVisible(false);
                    } else {
                        item.setVisible(true);
                    }
                }
            }
        });
        routesButton = (Button) findViewById(R.id.route_select_button);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                route = data.getStringExtra("route");
                displayRoute(route);
            }
        }
    }

    public void displayRoute(String route){
        routesButton.setText(route);
        final Query stopsQuery = fbStops.orderByKey().equalTo(route);
        final ValueEventListener stopsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                stopMarkers.clear();
                if(dataSnapshot.getValue() != null) {
                    for(DataSnapshot stopList : dataSnapshot.getChildren()) {
                        HashMap<String, HashMap<String, String> > stops = (HashMap<String, HashMap<String, String> >) stopList.getValue();
                        Iterator it = stops.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry pair = (Map.Entry)it.next();
                            String name = (String)pair.getKey();
                            HashMap<String, Object> x = (HashMap<String, Object>) pair.getValue();
                            String latitude = (String) x.get("latitude");
                            String longitude = (String) x.get("longitude");
                            String id = (String) x.get("id");
                            String code = (String) x.get("code");
                            Marker retMarker = map.addMarker(new MarkerOptions()
                                    .position(new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude)))
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.circle_stop_green))
                                    .anchor(0.5f,0.5f));
                            retMarker.setTag(code);
                            retMarker.setTitle("Arriving at:");
                            retMarker.setSnippet("No Time Available");
                            stopMarkers.add(retMarker);
                            it.remove();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        stopsQuery.addListenerForSingleValueEvent(stopsListener);

        final Query routesQuery = fbTrips.orderByChild("route").equalTo(route);
        final ValueEventListener routesListener = new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue() != null) {
                    for(Marker item : currentBusMarkers) {
                        item.remove();
                    }
                    currentBusMarkers.clear();
                    for(DataSnapshot route : dataSnapshot.getChildren()) {
                        MarkerOptions marker = new MarkerOptions()
                                .position(new LatLng(Double.parseDouble(route.child("latitude").getValue().toString()), Double.parseDouble(route.child("longitude").getValue().toString())))
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_bus_green));
                        String bearing = route.child("bearing").getValue().toString();
                        switch (bearing) {
                            case "0":
                                marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_bus_green_0));
                                break;
                            case "45.0":
                                marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_bus_green_45));
                                break;
                            case "90.0":
                                marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_bus_green_90));
                                break;
                            case "135.0":
                                marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_bus_green_135));
                                break;
                            case "180.0":
                                marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_bus_green_180));
                                break;
                            case "225.0":
                                marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_bus_green_225));
                                break;
                            case "270.0":
                                marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_bus_green_270));
                                break;
                            case "315.0":
                                marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_bus_green_315));
                                break;
                        }
                        Marker retMarker = map.addMarker(marker);
                        retMarker.setTag("Bus");
                        currentBusMarkers.add(retMarker);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        routesQuery.addValueEventListener(routesListener);

        routesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                map.clear();
                routesQuery.removeEventListener(routesListener);
                Intent intent = new Intent(getApplicationContext(), RouteSelect.class);
                startActivityForResult(intent, 1);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(42.7369792, -84.48386540000001))
                .zoom(15)
                .build();
        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        googleMap.getUiSettings().setRotateGesturesEnabled(false);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final Marker marker) {
                if(marker.getTag().equals("bus")) {
                    return true;
                }

                final Query stopTimeQuery = fbTrips.orderByChild("route").equalTo(route);
                final ValueEventListener stopTimeListener = new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        ArrayList<String> stopsArray = new ArrayList<String>();
                        if(dataSnapshot.getValue() != null) {
                            for(DataSnapshot bus : dataSnapshot.getChildren()) {
                                DataSnapshot stops = bus.child("stops");
                                for(DataSnapshot stop:stops.getChildren()){
                                    if(stop.child("stop_id").getValue().toString().equals(marker.getTag())){
                                        stopsArray.add(stop.child("arrival").getValue().toString());
                                    }
                                }
                            }
                            java.util.Collections.sort(stopsArray);

                            String currentTime = (String) DateFormat.format("hh:mm", new java.util.Date());
                            if(stopsArray.size() >= 1) {
                                marker.setSnippet(stopsArray.get(0));
                                if(stopsArray.get(0).compareTo(currentTime) < 0 && stopsArray.size() > 1 ) {
                                    marker.setSnippet(stopsArray.get(1));
                                }
                            }
                            marker.showInfoWindow();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                };
                stopTimeQuery.addListenerForSingleValueEvent(stopTimeListener);



                return false;
            }
        });

        displayRoute(route);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        try {
                            map.setMyLocationEnabled(true);
                        } catch (Exception e) {

                        }
                    }
                } else {

                }
                return;
            }
        }
    }
}
