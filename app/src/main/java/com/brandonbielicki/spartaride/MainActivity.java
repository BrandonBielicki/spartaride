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
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;

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
                            Marker retMarker = map.addMarker(new MarkerOptions()
                                    .position(new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude)))
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.circle_stop_green))
                                    .anchor(0.5f,0.5f)
                                    .title(""));
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
                                .title("bus")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_bus_green));
                        Marker retMarker = map.addMarker(marker);

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
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if(marker.getTitle().equals("bus")) {
                    return true;
                }
                //CODE TO SET MARKER TITLE TO NEXT 3 STOP TIMES
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
