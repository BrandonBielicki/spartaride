package com.brandonbielicki.spartaride;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    GoogleMap map;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference fbTrips = database.getReference("trips");
    DatabaseReference fbStops = database.getReference("stops");
    DatabaseReference fbBuses = database.getReference("buses");
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

    public void displayBusStops(DataSnapshot dataSnapshot){
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
                    String code = (String) x.get("description");
                    Marker retMarker = map.addMarker(new MarkerOptions()
                            .position(new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude)))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.circle_stop_green))
                            .anchor(0.5f,0.5f));
                    retMarker.setTag(id);
                    retMarker.setTitle("Arriving at stop #" + id + " at:");
                    retMarker.setSnippet("No Time Available");
                    stopMarkers.add(retMarker);
                    it.remove();
                }
            }
        }
    }
    public void getBusStops() {
        final Query stopsQuery = fbStops.orderByKey().equalTo(route);
        final ValueEventListener stopsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                displayBusStops(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        stopsQuery.addListenerForSingleValueEvent(stopsListener);
    }

    public void busUpdateEvent(DataSnapshot dataSnapshot) {
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
                    case "0.0":
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
                //retMarker.setTitle(route.getKey());
                currentBusMarkers.add(retMarker);
            }
        }
    }

    public ValueEventListener getInitialBusPositionsListener() {
        final ValueEventListener busesListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue() != null) {
                    for(DataSnapshot route : dataSnapshot.getChildren()) {
                        busUpdateEvent(route);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        return busesListener;
    }

    public ChildEventListener createBusUpdateListener() {

        final ChildEventListener busesListener = new ChildEventListener() {

            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                busUpdateEvent(dataSnapshot);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        return busesListener;
    }

    public void displayRoute(String route){
        //Update route button to display currently selected route
        routesButton.setText(route);

        //Event listener for bus stop markers
        getBusStops();

        //Event listener for buses where "route" is equal to currently selected route
        final Query busesQuery = fbBuses.orderByKey().equalTo(route);
        final ChildEventListener busesListener = createBusUpdateListener();
        busesQuery.addChildEventListener(busesListener);
        busesQuery.addListenerForSingleValueEvent(getInitialBusPositionsListener());

        //When route select button is clicked, clear map icons and open route selection
        routesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                map.clear();
                busesQuery.removeEventListener(busesListener);
                Intent intent = new Intent(getApplicationContext(), RouteSelect.class);
                startActivityForResult(intent, 1);
            }
        });
    }

    public String getCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat mdformat = new SimpleDateFormat("hh:mm");
        String time = mdformat.format(calendar.getTime());
        return time;
    }

    public ValueEventListener createStopClickListener(final Marker marker) {
        final ValueEventListener stopTimeListener = new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                stopClickEvent(dataSnapshot, marker);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        return stopTimeListener;
    }
    public void stopClickEvent(DataSnapshot dataSnapshot, Marker marker) {
        ArrayList<String> stopsArray = new ArrayList<>();
        for(DataSnapshot stop_id : dataSnapshot.getChildren()) {
            for(DataSnapshot trip:stop_id.getChildren()){
                String arrival_time = trip.child("arrival").getValue().toString();
                int delay = Integer.parseInt(trip.child("delay").getValue().toString());
                SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm");
                Calendar calendar = Calendar.getInstance();
                try {
                    Date parsedDate = dateFormat.parse(arrival_time);
                    calendar.setTime(parsedDate);
                    calendar.add(Calendar.SECOND ,delay);
                    stopsArray.add(dateFormat.format(calendar.getTime()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

        java.util.Collections.sort(stopsArray);
        String currentTime = getCurrentTime();
        if(stopsArray.size() >= 1) {

            if(stopsArray.get(0).compareTo(currentTime) < 0 ) {
                if(stopsArray.size() > 1) {
                    marker.setSnippet(stopsArray.get(1));
                } else {
                    marker.setSnippet("No Time Available");
                }
            } else {
                marker.setSnippet(stopsArray.get(0));
            }
        }
        marker.showInfoWindow();
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

        //Event listener for clicking on map markers, bus or stops
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final Marker marker) {
                if(marker.getTag().equals("bus")) {
                    //marker.showInfoWindow();
                    return true;
                }

                final Query stopTimeQuery = fbTrips.child(route).orderByKey().equalTo(marker.getTag().toString());
                ValueEventListener stopClickListener = createStopClickListener(marker);
                stopTimeQuery.addListenerForSingleValueEvent(stopClickListener);
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
