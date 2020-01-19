package com.ogulcan.whereismypetapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "";
    private GoogleMap mMap;
    private FirebaseDatabase db;
    private int isOn;
    private Button dButton, dogButton, catButton;
    private Marker lastMarker;
    private boolean isDogSelected;
    private String longd, latd, longdauto, latdauto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        dButton = (Button) findViewById(R.id.dbutton);
        dogButton = (Button) findViewById(R.id.buttondog);
        catButton = (Button) findViewById(R.id.buttoncat);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        isDogSelected = settings.getBoolean("isDogSelected", true);

        dogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDogSelected = true;
                lastMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.dog));
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("isDogSelected", true);
                editor.apply();
            }
        });

        catButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDogSelected = false;
                lastMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.cat));
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("isDogSelected", false);
                editor.apply();
            }
        });

        dButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(longd!=null && latd!=null){
                    if(!longd.equals("") && !latd.equals("")) {
                        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + latd + "," + longd);
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                        mapIntent.setPackage("com.google.android.apps.maps");
                        if (mapIntent.resolveActivity(getPackageManager()) != null) {
                            startActivity(mapIntent);
                        }
                    }
                }
                else if(longdauto!=null && latdauto!=null){
                    if(!longdauto.equals("") && !latdauto.equals("")) {
                        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + latdauto + "," + longdauto);
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                        mapIntent.setPackage("com.google.android.apps.maps");
                        if (mapIntent.resolveActivity(getPackageManager()) != null) {
                            startActivity(mapIntent);
                        }
                    }
                }
                else{
                    Toast.makeText(getApplicationContext(), "No location available.", Toast.LENGTH_LONG).show();
                }
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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

    private Date yesterday(){
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);

        return cal.getTime();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Intent chosenIntent = getIntent();
        isOn = Objects.requireNonNull(chosenIntent.getExtras()).getInt("isOn");
        mMap = googleMap;
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                marker.showInfoWindow();
                LatLng latLng = marker.getPosition();
                longd = Double.toString(latLng.longitude);
                latd = Double.toString(latLng.latitude);
                return true;
            }
        });
        mMap.setMaxZoomPreference(16);

        db = FirebaseDatabase.getInstance();
        if(isOn==1) {
            catButton.setVisibility(View.GONE);
            dogButton.setVisibility(View.GONE);

            final Query locRef = db.getReference("location").orderByKey();

            final Date yesterdayDate = yesterday();
            final List<Marker> al = new ArrayList<>();

            locRef.addChildEventListener(new ChildEventListener() {
                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                @SuppressLint("SimpleDateFormat")
                @Override
                public void onChildAdded(@NonNull DataSnapshot doc, @Nullable String s) {
                    try {
                        if (!(new SimpleDateFormat("MM/dd/yyyy").parse(doc.child("date").getValue().toString()).before(yesterdayDate))) {
                            LatLng currentPosition = new LatLng(Double.parseDouble(Objects.requireNonNull(doc.child("lat").getValue()).toString()), Double.parseDouble(Objects.requireNonNull(doc.child("long").getValue()).toString()));
                            Marker lastMarker = mMap.addMarker(new MarkerOptions().title(timeFix(Objects.requireNonNull(doc.child("date").getValue()).toString())).position(currentPosition));
                            al.add(lastMarker);
                            longdauto = Double.toString(currentPosition.longitude);
                            latdauto = Double.toString(currentPosition.latitude);
                            lastMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                            lastMarker.showInfoWindow();
                            if (al.size() > 1) {
                                al.get(al.size() - 2).setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                                mMap.addPolyline(new PolylineOptions()
                                        .add(al.get(al.size() - 2).getPosition(), lastMarker.getPosition()).width(5).color(Color.RED));
                                al.get(al.size() - 2).hideInfoWindow();
                            }
                            float zoomLevel = 12.0f; //This goes up to 21
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, zoomLevel));
                        }

                        else{
                            db.getReference("location").child(Objects.requireNonNull(doc.getKey())).removeValue();
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot doc, @Nullable String s) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot doc) {

                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot doc, @Nullable String s) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        else{
            catButton.setVisibility(View.VISIBLE);
            dogButton.setVisibility(View.VISIBLE);
            Query locRef = db.getReference("location").orderByKey().limitToLast(1);

            locRef.addChildEventListener(new ChildEventListener() {
                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                @Override
                public void onChildAdded(@NonNull DataSnapshot doc, @Nullable String s) {
                    mMap.clear();
                    LatLng currentPosition = new LatLng(Double.parseDouble(Objects.requireNonNull(doc.child("lat").getValue()).toString()), Double.parseDouble(doc.child("long").getValue().toString()));
                    Marker marker = mMap.addMarker(new MarkerOptions().title(timeFix(Objects.requireNonNull(doc.child("date").getValue()).toString())).position(currentPosition));
                    if(isDogSelected)
                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.dog));
                    else
                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.cat));
                    lastMarker = marker;
                    marker.showInfoWindow();
                    longdauto = Double.toString(currentPosition.longitude);
                    latdauto = Double.toString(currentPosition.latitude);
                    float zoomLevel = 16.0f; //This goes up to 21
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, zoomLevel));
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    private String timeFix(String timeEntered) {
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("MM/dd/yyyy, hh:mm:ss aa");
        @SuppressLint("SimpleDateFormat") DateFormat outputformat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String output = null;
        Date date = null;
        try{
            //Conversion of input String to date
            date= df.parse(timeEntered);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.HOUR, 3);
            //old date format to new date format
            output = outputformat.format(calendar.getTime());
        }catch(ParseException pe){
            pe.printStackTrace();
        }
        return output;
    }
}
