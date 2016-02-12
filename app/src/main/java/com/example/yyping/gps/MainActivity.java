package com.example.yyping.gps;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener,GoogleMap.OnCameraChangeListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnMyLocationChangeListener {


    //     * Geofences Array    ======================================================================
    ArrayList<Geofence> mGeofences;

    //     * Geofence Coordinates
    ArrayList<LatLng> mGeofenceCoordinates;

    //     * Geofence Radius'
    ArrayList<Integer> mGeofenceRadius;

    // ==================================================================
    float latitude,longitude ;

    //     * Geofence Store Java Class ======================================================================
    private GeofenceStore mGeofenceStore;


    private static final int INITIAL_REQUEST = 1;
    private static final int LOCATION_REQUEST = INITIAL_REQUEST + 3;
    private static final int LASTKNOWN_REQUEST = INITIAL_REQUEST + 4;

    private final int CURRENTLOCATION = 1;
    private final int POI = 2;
    private final int AUTOCLOCKIN = 3;

    private SharedPreferences pref;

    private Intent locationIntent;
    private Handler handler;
    //LocationManager locationmanager;
    int userChoice;

    private Calendar cal;
    private MarkerOptions markerCurrentLocation;
    private LocationManager locationManager;
    final Context context = this;
    private static final float MINIMUM_DISTANCECHANGE_FOR_UPDATE = 1; // in Meters
    private static final long MINIMUM_TIME_BETWEEN_UPDATE = 60000 * 5; //location update every 5 minutes (60000 x 5). ( 60000 = 1 minute)
    private static long POINT_RADIUS = 500; // in Meters
    private static final long PROX_ALERT_EXPIRATION = -1;
    private static final String POINT_LATITUDE_KEY = "POINT_LATITUDE_KEY";
    private static final String POINT_LONGITUDE_KEY = "POINT_LONGITUDE_KEY";
    private boolean broadcastReceiverRegistered = false;
    private ProximityIntentReceiver proximityReceiver;
    private static final String PROX_ALERT_INTENT = "com.example.yyping.gps.MainActivity";
    private int uniqueId, logNumber, cpNumber;

    private TextView POI1, information, tvCurrentLocation;
    private Button btnClockIn, btnClear, btnSearch, btnLog;
    private ListView lvNearByList;
    private LinearLayout layoutList;

    private String locationName;
    public static Context AlretRegion;

    ArrayList<LatLng> locationList = new ArrayList<>();
    ArrayList<String> location_Name_list = new ArrayList<>();

    //private GoogleMap googleMap;
    private GoogleMap mMap;

    //Google Place API
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap.OnMyLocationChangeListener listener;
    private Location mLocation;
    private String mAddress;
    private boolean isGooglePlayReady = false, isLocationReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AlretRegion = this;

        initLoad();
        initUI();

    }



    private void initLoad() {
        pref = this.getSharedPreferences("TimeTec_GPS", MODE_PRIVATE);
        logNumber = pref.getInt("lognumber", 0);
        cpNumber = pref.getInt("cpnumber", 0);

        /*String log = pref.getString("log", "");
        if(log.length() == 0) {
            log = "You have Enter 'iTech Tower' region on 26/01/2016 08:46:12\n" + "You have Exit 'iTech Tower' region on 26/01/2016 18:31:54";
            pref.edit().putString("log", log).commit();
        }*/

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .enableAutoManage(this, 0, this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        handler = new Handler();

        startLocationService();
    }

    private void startLocationService() {
        locationIntent = new Intent(context, LocationService.class);
        if (isServiceExisted(context, "com.example.yyping.gps.LocationService") == null)
            this.startService(locationIntent);
        else {
            try {
                this.stopService(locationIntent);
                this.startService(locationIntent);
            } catch (Exception e) {
                new AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage("Unable to start service")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();
            }
        }
    }

    public static ComponentName isServiceExisted(Context context, String className) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(Integer.MAX_VALUE);

        if (!(serviceList.size() > 0))
            return null;

        for (int i = 0; i < serviceList.size(); i++) {
            ActivityManager.RunningServiceInfo serviceInfo = serviceList.get(i);
            ComponentName serviceName = serviceInfo.service;
            if (serviceName.getClassName().equals(className))
                return serviceName;
        }

        return null;
    }

    private void initUI() {
        information = (TextView) findViewById(R.id.information);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST);
        } else {
            try {
                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        MINIMUM_TIME_BETWEEN_UPDATE, MINIMUM_DISTANCECHANGE_FOR_UPDATE,
                        new MyLocationListener()
                );
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        // The notification textView
        POI1 = (TextView) findViewById(R.id.POI1);
        POI1.setText("Loading...");

        // TextView location_status = (TextView) findViewById(R.id.location_status);

        Spinner spinner = (Spinner) findViewById(R.id.dropdown);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.M_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d("mode", String.valueOf(i));
                pref.edit().putInt("mode", i).commit();

                switch (i) {
                    //Defalut Mode
                    case 0:
                        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                        alertDialog.setTitle("Auto Clock In Distance");
                        alertDialog.setMessage("You Auto Clock In Distance On Default Mode Now!");
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog.show();

                        break;

                    //100 Meter
                    case 1:
                        userChoice = 1;
                        setup(userChoice);

                        AlertDialog alertDialog2 = new AlertDialog.Builder(MainActivity.this).create();
                        alertDialog2.setTitle("Auto Clock In Distance");
                        alertDialog2.setMessage("Your Auto Clock In Distance Set On 100 Meter!!");
                        alertDialog2.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog2.show();

                        break;
                    // 500 meter
                    case 2:
                        userChoice = 2;
                        setup(userChoice);

                        AlertDialog alertDialog3 = new AlertDialog.Builder(MainActivity.this).create();
                        alertDialog3.setTitle("Auto Clock In Distance");
                        alertDialog3.setMessage("Your Auto Clock In Distance Set On 500 Meter!!");
                        alertDialog3.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog3.show();

                        break;


                    //1000 meter
                    case 3:
                        userChoice = 3;
                        setup(userChoice);

                        AlertDialog alertDialog4 = new AlertDialog.Builder(MainActivity.this).create();
                        alertDialog4.setTitle("Auto Clock In Distance");
                        alertDialog4.setMessage("Your Auto Clock In Distance Set On 1000 Meter!!");
                        alertDialog4.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog4.show();


                        break;

                    //2000 meter
                    case 4:
                        userChoice = 4;
                        setup(userChoice);

                        AlertDialog alertDialog5 = new AlertDialog.Builder(MainActivity.this).create();
                        alertDialog5.setTitle("Auto Clock In Distance");
                        alertDialog5.setMessage("Your Auto Clock In Distance Set On 2000 Meter!!");
                        alertDialog5.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog5.show();
                        break;

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        spinner.setSelection(2);
        spinner.setEnabled(false);

        tvCurrentLocation = (TextView) findViewById(R.id.tv_currentlocation);
        btnClockIn = (Button) findViewById(R.id.btnClockIn);
        btnClockIn.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                String url = "https://maps.googleapis.com/maps/api/place/search/json?location=" + mLocation.getLatitude() + ","
                        + mLocation.getLongitude() + "&radius=500&sensor=true&key=AIzaSyAhCYwkFcx2Wl-I5I2vPFf11Lw-8BsVGrk";
                Log.i("place api", url);
                googleplaces gp = new googleplaces();
                gp.isClockIn = true;
                gp.execute(url);
            }
        });
        btnClockIn.setEnabled(false);

        btnClear = (Button) findViewById(R.id.btnClear);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Clear all marker
                mMap.clear();

                lvNearByList.setAdapter(null);
                layoutList.setVisibility(View.GONE);

                //set current location marker
                LatLng latlng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
                addMarker(latlng, "Current Location - " + mLocation.getLatitude() + "," + mLocation.getLongitude(), CURRENTLOCATION);
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(latlng)             // Sets the center of the map to Mountain View
                        .zoom(17)                   // Sets the zoom
                        .bearing(0)                // Sets the orientation of the camera to east
                        .tilt(0)                   // Sets the tilt of the camera to 30 degrees
                        .build();                   // Creates a CameraPosition from the builder
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                POI1.setText("");
                locationList = new ArrayList<>();

                //Clear checkpoint number and all checkpoint in Shared preference
                cpNumber = 0;
                SharedPreferences.Editor e = pref.edit();
                e.putInt("cpnumber", 0);
                e.putString("log", "");
                e.commit();
            }
        });
        btnClear.setEnabled(false);

        btnSearch = (Button) findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddressDialog();
            }
        });
        btnSearch.setEnabled(false);

        btnLog = (Button) findViewById(R.id.btnLog);
        btnLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLogDialog();
            }
        });

        layoutList = (LinearLayout) findViewById(R.id.layoutList);
        lvNearByList = (ListView) findViewById(R.id.listView);
        layoutList.setVisibility(View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LASTKNOWN_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                }
                return;
            }
            case LOCATION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    try {
                        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                MINIMUM_TIME_BETWEEN_UPDATE, MINIMUM_DISTANCECHANGE_FOR_UPDATE,
                                new MyLocationListener()
                        );
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
                return;
            }
        }
    }

    //setup radius....
    private long POINT_RADIUS1() {
        POINT_RADIUS = 100;
        return POINT_RADIUS;
    }

    private long POINT_RADIUS2() {
        POINT_RADIUS = 500;
        return POINT_RADIUS;
    }

    private long POINT_RADIUS3() {
        POINT_RADIUS = 1000;
        return POINT_RADIUS;
    }

    private long POINT_RADIUS4() {
        POINT_RADIUS = 2000;
        return POINT_RADIUS;
    }

    //setup POI radius....
    public void setup(int userChoice) {
        if (userChoice == 1) {
            POINT_RADIUS1();
        } else if (userChoice == 2) {
            POINT_RADIUS2();
        } else if (userChoice == 3) {
            POINT_RADIUS3();
        } else if (userChoice == 4) {
            POINT_RADIUS4();
        }

    }


    //show the information clock in
    public void showAlertDialog(Context context, String message) {

        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        // Setting Dialog Title
        alertDialog.setTitle("Information Auto Clock In");

        // Setting Dialog Message
        alertDialog.setMessage(message);

        // Setting OK Button
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Okay",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.dismiss();
                    }
                });

        // Showing Alert Message
        alertDialog.show();
    }


    private void addProximity(LatLng latLng, String checkpoint, boolean isAdd) {
        Intent intent = new Intent(PROX_ALERT_INTENT);
        intent.putExtra("checkpoint", checkpoint);

        Log.d("debug:" + getClass().getSimpleName(), "checkpoint: " + checkpoint);

        PendingIntent proximityIntent = PendingIntent.getBroadcast(this, uniqueId++, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        try {
            locationManager.addProximityAlert(
                    latLng.latitude, // the latitude of the central point of the alert region
                    latLng.longitude, // the longitude of the central point of the alert region
                    POINT_RADIUS, // the radius of the central point of the alert region, in meters
                    PROX_ALERT_EXPIRATION, // time for this proximity alert, in milliseconds, or -1 to indicate no expiration
                    proximityIntent // will be used to generate an Intent to fire when entry to or exit from the alert region is detected
            );

        } catch (SecurityException e) {
            e.printStackTrace();
        }
        if (!broadcastReceiverRegistered) {
            IntentFilter filter = new IntentFilter(PROX_ALERT_INTENT);
            proximityReceiver = new ProximityIntentReceiver();
            registerReceiver(proximityReceiver, filter);
            broadcastReceiverRegistered = true;
            Log.d("debug:" + getClass().getSimpleName(), "registering broadcast receiver");
        }

        //Save into share preferences
        if (isAdd) {
            String name = "checkpoint" + String.valueOf(cpNumber);
            String info = checkpoint + "_" + String.valueOf(latLng.latitude) + "_" + String.valueOf(latLng.longitude);
            cpNumber = cpNumber + 1;

            SharedPreferences.Editor e = pref.edit();
            e.putInt("cpnumber", cpNumber);
            e.putString(name, info);
            e.commit();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    //formula to find the distance with two point..
    public static float distFrom(float lat1, float lng1, float lat2, float lng2) {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        float dist = (float) (earthRadius * c);
        return dist;
    }


    //show nearest distance from point...
    class Distance {
        double dis;
        String locname;

        public Distance(double j, String k) {
            dis = j;
            locname = k;
        }

        public double GetDistance() {
            return dis;
        }

        public String GetLocation() {
            return locname;
        }

    }

    public class MyLocationListener implements LocationListener {
        public void onLocationChanged(Location location) {

            Location pointLocation = new Location("POINT_LOCATION");  //retrievelocationFromPreferences();
            String fullText = "";
            List<Distance> lstDistance = new ArrayList<Distance>();
            LatLng latlng = null;
            int counter = 0;


            // LOCATION ARRAY LIST
            for (int i = 0; i < locationList.size(); i++) {

                latlng = locationList.get(i);
                latitude = (float) location.getLatitude();
                longitude = (float) location.getLongitude();

                pointLocation.setLatitude(latlng.latitude);
                pointLocation.setLongitude(latlng.longitude);

                //set fix POI
                float latfixpoit = (float) latlng.latitude;
                float longfixpoint = (float) latlng.longitude;

                //float distance = location.distanceTo(pointLocation);
                double distance = distFrom(latitude, longitude, latfixpoit, longfixpoint);
                lstDistance.add(new Distance(distance, location_Name_list.get(i)));

            }




            //find nearest point..
            for (int i = 0; i < lstDistance.size(); i++) {
                Distance dc = lstDistance.get(i);

                for (int y = i + 1; y < lstDistance.size(); y++) {
                    Distance dc2 = lstDistance.get(y);
                    if (dc.GetDistance() > dc2.GetDistance()) {
                        lstDistance.remove(y);
                        lstDistance.add(y, new Distance(dc.GetDistance(), dc.GetLocation()));

                        lstDistance.remove(i);
                        lstDistance.add(i, new Distance(dc2.GetDistance(), dc2.GetLocation()));
                        dc = lstDistance.get(i);
                    }

                }
            }

            DecimalFormat df = new DecimalFormat("0.00");
            for (int i = 0; i < lstDistance.size(); i++) {

                if (counter <= 4) {
                    Distance dc = lstDistance.get(i);
                    fullText += "Distance From " + dc.GetLocation() + ": " + getDistanceInStr(dc.GetDistance()) + "\n";
                    // break;
                }

                counter++;
            }

            POI1.setText(fullText);
            information.setText("");

            LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());
            //mMarker = mMap.addMarker(new MarkerOptions().position(loc));
            if (mMap != null) {
                float zoom = mMap.getCameraPosition().zoom;
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, zoom));
            }

            //Close the keep update location function for 30 seconds
            /*mMap.setOnMyLocationChangeListener(null);
            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    mMap.setOnMyLocationChangeListener(listener);
                }

            }, 30000);*/
        }

        public void onStatusChanged(String s, int i, Bundle b) {
        }

        public void onProviderDisabled(String s) {
            showSettingsAlert();
        }

        public void onProviderEnabled(String s) {
        }
    }

   /* public static <K extends Comparable,V extends Comparable> Map<K,V> sortByValues(Map<K,V> map){
        List<Map.Entry<K,V>> entries = new LinkedList<Map.Entry<K,V>>(map.entrySet());

        Collections.sort(entries, new Comparator<Map.Entry<K,V>>() {

            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        //LinkedHashMap will keep the keys in the order they are inserted
        //which is currently sorted on natural ordering
        Map<K,V> sortedMap = new LinkedHashMap<K,V>();

        for(Map.Entry<K,V> entry: entries){
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }*/


    /*
     * Function to show settings alert dialog
     * On pressing Settings button will launch Settings Options
     * */
    public void showSettingsAlert() {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        // Setting Dialog Title
        alertDialog.setTitle("GPS settings");

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want enable it at Setting Menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }


    private void addMarker(LatLng latLng, String title, int index) {
        MarkerOptions marker;
        if (index == CURRENTLOCATION) {
            marker = new MarkerOptions().position(latLng).title(title).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            markerCurrentLocation = marker;
        } else if (index == POI)
            marker = new MarkerOptions().position(latLng).title(title).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        else
            marker = new MarkerOptions().position(latLng).title(title).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

        mMap.addMarker(marker);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMyLocationChangeListener(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
        listener = this;
        //googleMap.setMyLocationEnabled(true);

        //Criteria criteria = new Criteria();
        /*Location location = null;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LASTKNOWN_REQUEST);
        }
        else{
            try {
                location = locationManager.getLastKnownLocation(locationManager.GPS_PROVIDER);
                //location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        if (location != null) {

            /*float DefaultLatitude = (float) 2.922750;
            float DefaultLongitude = (float) 101.641596;

           // LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            LatLng latLng = new LatLng(DefaultLatitude,DefaultLongitude);


            // add a marker for current location, and associate it with a proximity alert
            locationList.add(latLng);
            location_Name_list.add("Home");


            addMarker(latLng, "Home");
            addProximityAlert(latLng, "Home");//Default home location!! change it later.. yin ping

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latLng)             // Sets the center of the map to Mountain View
                    .zoom(17)                   // Sets the zoom
                    .bearing(0)                // Sets the orientation of the camera to east
                    .tilt(0)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else
            Toast.makeText(getApplicationContext(), "Current location not found", Toast.LENGTH_LONG).show();*/


    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        showLocationDialog(latLng);

    }


    private void showLocationDialog(final LatLng latLng) {
        // get prompts.xml view
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.location_name_prompt, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                context);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView
                .findViewById(R.id.editTextDialogUserInput);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // get user input and set it to result
                                // edit text
                                locationName = userInput.getText().toString();
                                Log.d("debug:" + getClass().getSimpleName(), locationName);
                                locationList.add(latLng);
                                location_Name_list.add(locationName);
                                addMarker(latLng, locationName, AUTOCLOCKIN);
                                addProximity(latLng, locationName, true);
                                information.setText("Loading...");
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }


    //Get Current location address
    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(proximityReceiver);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // Connected to Google Play services!
        // The good stuff goes here.
        /*try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    public void requestPermissions(@NonNull String[] permissions, int requestCode)
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for Activity#requestPermissions for more details.
                return;
            }


        }
        catch(Exception exp){
            Log.e("onConnected exception", exp.getMessage());
        }*/

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location testLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (testLocation == null) {
            Log.e("testLocation", "location is null");
        } else {
            Log.e("testLocation", "Latitude = " + testLocation.getLatitude() + ", Longitude = " + testLocation.getLongitude());
        }

        isGooglePlayReady = true;
        if(isLocationReady)
            btnClockIn.setEnabled(true);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // This callback is important for handling errors that
        // may occur while attempting to connect with Google.
        //
        // More about this in the 'Handle Connection Failures' section.

    }


//    ===============================================ADD GEOFENCE HERE BY AMMAR ======================================================
    @Override
    public void onMyLocationChange(Location location){
        mLocation = location;
        if(mLocation != null) {
            Log.e("onmylocationchange", "Latitude = " + mLocation.getLatitude() + ", Longitude = " + mLocation.getLongitude());

            tvCurrentLocation.setText(getAddress(mLocation));
            if(markerCurrentLocation == null){
                LatLng latlng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
                addMarker(latlng, "Current Location - " + mLocation.getLatitude() + "," + mLocation.getLongitude(), CURRENTLOCATION);
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(latlng)             // Sets the center of the map to Mountain View
                        .zoom(17)                   // Sets the zoom
                        .bearing(0)                // Sets the orientation of the camera to east
                        .tilt(0)                   // Sets the tilt of the camera to 30 degrees
                        .build();                   // Creates a CameraPosition from the builder
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                //First time, show location which set before
                if(cpNumber > 0){
                    Log.e("onmylocationchange", "keep add");
                    for(int i = 0; i < cpNumber; i++){
                        String name = "checkpoint" + String.valueOf(i);
                        String info = pref.getString(name, "");

                        Log.e(name, info);

                        String[] checkpoint = info.split("_");
                        locationName = checkpoint[0];
                        LatLng ll = new LatLng(Double.parseDouble(checkpoint[1]), Double.parseDouble(checkpoint[2]));
                        locationList.add(ll);
                        location_Name_list.add(locationName);
                        addMarker(ll, locationName, AUTOCLOCKIN);
                        addProximity(ll, locationName, false);
                        information.setText("Loading...");
                    }
                }
            }

            btnClockIn.setEnabled(true);
            btnClear.setEnabled(true);
            btnSearch.setEnabled(true);

            //Keep to update distance
            List<Distance> lstDistance = new ArrayList<Distance>();
            LatLng latlng = null;
            int counter = 0;

            for (int i = 0; i < locationList.size(); i++) {
                latlng = locationList.get(i);
                float latitude = (float) location.getLatitude();
                float longitude = (float) location.getLongitude();

                float latfixpoit = (float) latlng.latitude;
                float longfixpoint = (float) latlng.longitude;

                double distance = distFrom(latitude, longitude, latfixpoit, longfixpoint);
                lstDistance.add(new Distance(distance, location_Name_list.get(i)));
            }


//            ======================================================== ADDED LOOP ON 11/2 6.16PM ========================================

            //Loop to get location list
            for (int i = 0; i < locationList.size(); i++) {
                latlng = locationList.get(i);
                float latitude = (float) location.getLatitude();
                float longitude = (float) location.getLongitude();


                // Initializing variables
                mGeofences = new ArrayList<Geofence>();
                mGeofenceCoordinates = new ArrayList<LatLng>();
                mGeofenceRadius = new ArrayList<Integer>();


                // Adding geofence coordinates to array.
                mGeofenceCoordinates.add(new LatLng(latitude,longitude));
//              mGeofenceCoordinates.add(new LatLng(3.9230733, 101.6613033)); //FingerTec R&D Centre

                // Adding associated geofence radius' to array.
                mGeofenceRadius.add(100);
                // mGeofenceRadius.add(60);


                // Bulding the geofences and adding them to the geofence array.
                // HALLOOO R&D Centre
                mGeofences.add(new Geofence.Builder().setRequestId(location_Name_list.get(i))//set name in the notification
                        // The coordinates of the center of the geofence and the radius in meters.
                        .setCircularRegion(locationList.get(i).latitude, locationList.get(i).longitude, mGeofenceRadius.get(0).intValue())
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                                // Required when we use the transition type of GEOFENCE_TRANSITION_DWELL
                        .setLoiteringDelay(10000) // (60000 = 1 minute Delay)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT).build());


                // FingerTec R&D Centre
//                mGeofences.add(new Geofence.Builder()
//                        .setRequestId("FingerTec R&D Centre")
//                                // The coordinates of the center of the geofence and the radius in meters.
//                        .setCircularRegion(mGeofenceCoordinates.get(0).latitude, mGeofenceCoordinates.get(0).longitude, mGeofenceRadius.get(0).intValue())
//                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
//                                // Required when we use the transition type of GEOFENCE_TRANSITION_DWELL
//                        .setLoiteringDelay(30000) // (60000 = 1 minute Delay)
//                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT).build());



                // Add the geofences to the GeofenceStore.Java object.
                mGeofenceStore = new GeofenceStore(this, mGeofences);


                //   ==========================================END OF AMMAR'S GEOFENCE ===============================================

            } //  =================================================== END OF LOCATION LIST LOOP ADDED  ======================================

            boolean result = false;
            cal = Calendar.getInstance();
            String currentDate = String.valueOf(android.text.format.DateFormat.format("yyyyMMdd", cal));
            String currentTime = String.valueOf(android.text.format.DateFormat.format("HHmm", cal));
            String currentDatetime = String.valueOf(android.text.format.DateFormat.format("dd/MM/yyyy HH:mm:ss", cal));

            DecimalFormat df = new DecimalFormat("0.00");
            String fullText = "";
            for (int i = 0; i < lstDistance.size(); i++) {
                if (counter <= 4) {
                    Distance dc = lstDistance.get(i);
                    fullText += "Distance From " + dc.GetLocation() + ": " + getDistanceInStr(dc.GetDistance()) + "\n";

                    String info = dc.GetLocation() + currentDate;
                    String enterOrExit = "Enter";
                    String enterExist = pref.getString(info + "Enter", "");
                    String lastEnterTime = pref.getString(info + "LastEnter", "");
                    String exitExist = pref.getString(info + "Exit", "");
                    String lastExitTime = pref.getString(info + "LastExit", "");
                    Log.d("distance - " + String.valueOf(dc.GetDistance()), enterOrExit);
                    Log.d("enter - " + lastEnterTime, "exit - " + lastExitTime);

                    if(dc.GetDistance() >= 500){ //outside the region now
                        //Only trigger when user got enter this region before and haven't exit before
                        if(enterExist.length() > 0 && exitExist.length() == 0 && (lastExitTime.length() == 0 || !lastExitTime.equalsIgnoreCase(currentTime))){
                            result = true;
                            enterOrExit = "Exit";
                            Log.d("exit - " + currentTime, "trigger - " + String.valueOf(!exitExist.equalsIgnoreCase(currentTime)));
                        }
                    }
                    else{ //within the region now
                        //Only trigger if user no enter this region before
                        if(enterExist.length() == 0 && (lastEnterTime.length() == 0 || !lastEnterTime.equalsIgnoreCase(currentTime))){
                            result = true;
                            enterOrExit = "Enter";
                            Log.d("enter - " + currentTime, "trigger - " + String.valueOf(!enterExist.equalsIgnoreCase(currentTime)));
                        }
                    }

                    if(result){
                        //alert user and save into log
                        String msg = "You have "+ enterOrExit + " '" + dc.GetLocation() +"' region on " + currentDatetime;
                        toastAndNotify(msg, true);

                        String log = pref.getString("log", "");
                        if(log.length() > 0)
                            log = log + "\n";

                        log = log + msg;
                        SharedPreferences.Editor e = pref.edit();
                        e.putString("log", log);
                        if(enterOrExit.equalsIgnoreCase("Enter")) {
                            e.putString(info + "LastEnter", currentTime);
                            e.putString(info + "Enter", "got");
                            e.putString(info + "Exit", "");
                        }
                        else {
                            e.putString(info + "LastExit", currentTime);
                            e.putString(info + "Enter", "");
                            e.putString(info + "Exit", "got");
                        }

                        e.commit();
                    }
                    else{
                        //,toastAndNotify("Latitude = " + mLocation.getLatitude() + ", Longitude = " + mLocation.getLongitude(), false);
                    }
                }

                counter++;
            }

            POI1.setText(fullText);
            information.setText("");

        }else
            Log.e("onmylocationchange", "null location");

        //isLocationReady = true;
        //if(isGooglePlayReady)
        //    btnClockIn.setEnabled(true);
    }

    /*protected void startIntentService() {
        Intent intent = new Intent(this, AddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLocation);
        startService(intent);
    }*/

    private String getAddress(Location location){
        String result = "No location found";

        List<Address> addresses = null;
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            Log.e("handleintent", "Latitude = " + location.getLatitude() +
                    ", Longitude = " + location.getLongitude());
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        }
        catch (IOException ioException) {
            // Catch network or other I/O problems.
            result = "Service no available";
            Log.e("getAddress", result, ioException);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            result = "invalid coordinate";
            Log.e("getAddress", result + ". " + "Latitude = " + location.getLatitude() +
                    ", Longitude = " + location.getLongitude(), illegalArgumentException);
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size()  == 0) {
            if (result.isEmpty()) {
                result = "No address found";
                Log.e("getAddress", result);
            }
            //deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
        }
        else {
            Address address = addresses.get(0);
            result = "";
            //ArrayList<String> addressFragments = new ArrayList<String>();

            // Fetch the address lines using getAddressLine,
            // join them, and send them to the thread.
            for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                if(result.length() > 0)
                    result = result + ", ";

                result = result + address.getAddressLine(i);
            }

            Log.i("getAddress", "Address found - " + result);
        }

        return result;
    }

    public class googleplaces extends AsyncTask<String, Void, String> {
        public boolean isClockIn;
        String temp;

        @Override
        protected String doInBackground(String... params) {
            // make Call to the url
            temp = makeCall(params[0]);

            //print the call in the console
            //System.out.println("https://maps.googleapis.com/maps/api/place/search/json?location=" + latitude + "," + longtitude + "&radius=500&sensor=true&key=AIzaSyAhCYwkFcx2Wl-I5I2vPFf11Lw-8BsVGrk");
            return "";
        }

        @Override
        protected void onPreExecute() {
            // we can start a progress bar here
            super.onPreExecute();
            Log.d("onPreExecute", "enter");
        }

        @Override
        protected void onPostExecute(String result) {
            if (temp == null) {
                // we have an error to the call
                // we can also stop the progress bar
            } else {
                // all things went right
                // parse Google places search result
                if(isClockIn)
                    getNearbyPOIList(temp);
                else {
                    List<PlaceInfo>venuesList = (ArrayList) parseGoogleParse(temp);
                    setAutoClockInPoint(venuesList.get(0));
                }
            }
        }
    }

    public static String makeCall(String urlstring) {
        // string buffers the url
        StringBuffer buffer_string = new StringBuffer("");
        String replyString = "";

        try{
            URL url = new URL(urlstring);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestProperty("User-Agent", "");
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.connect();

            // buffer input stream the result
            InputStream inputStream = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
            String line = "";
            while((line = rd.readLine()) != null){
                buffer_string.append(line);
            }

            // the result as a string is ready for parsing
            replyString = buffer_string.toString();
        }
        catch(IOException e){
            e.printStackTrace();
        }

        // trim the whitespaces
        return replyString.trim();
    }

    private ArrayList parseGoogleParse(final String response) {
        ArrayList temp = new ArrayList();
        try {
            // make an jsonObject in order to parse the response
            JSONObject jsonObject = new JSONObject(response);
            // make an jsonObject in order to parse the response
            if (jsonObject.has("results")) {
                JSONArray jsonArray = jsonObject.getJSONArray("results");
                for (int i = 0; i < jsonArray.length(); i++) {
                    PlaceInfo poi = new PlaceInfo();
                    if (jsonArray.getJSONObject(i).has("name")) {
                        poi.placename = jsonArray.getJSONObject(i).optString("name");
                        poi.rating = jsonArray.getJSONObject(i).optString("rating", " ");

                        if (jsonArray.getJSONObject(i).has("opening_hours")) {
                            if (jsonArray.getJSONObject(i).getJSONObject("opening_hours").has("open_now")) {
                                if (jsonArray.getJSONObject(i).getJSONObject("opening_hours").getString("open_now").equals("true")) {
                                    poi.opennow = "YES";
                                } else {
                                    poi.opennow = "NO";
                                }
                            }
                        } else {
                            poi.opennow = "NO KNOWN";
                        }

                        if (jsonArray.getJSONObject(i).has("types")) {
                            poi.category = "";
                            JSONArray typesArray = jsonArray.getJSONObject(i).getJSONArray("types");
                            for (int j = 0; j < typesArray.length(); j++) {
                                if(poi.category.length() > 0)
                                    poi.category = poi.category + ",";

                                poi.category = poi.category + typesArray.getString(j);
                            }
                        }

                        if (jsonArray.getJSONObject(i).has("vicinity")) {
                            poi.address = jsonArray.getJSONObject(i).optString("vicinity");
                        }

                        if (jsonArray.getJSONObject(i).has("geometry")) {
                            JSONObject object = jsonArray.getJSONObject(i).getJSONObject("geometry").getJSONObject("location");
                            poi.latitude = object.getDouble("lat");
                            poi.longitude = object.getDouble("lng");
                            poi.distance = distFrom((float) poi.latitude, (float) poi.longitude,
                                    (float) mLocation.getLatitude(), (float) mLocation.getLongitude());
                        }
                    }
                    temp.add(poi);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList();
        }

        return temp;
    }

    private void getNearbyPOIList(String temp){
        List<PlaceInfo>venuesList = (ArrayList) parseGoogleParse(temp);
        Collections.sort(venuesList, ListComparator.DistanceComparator);

        List listTitle = new ArrayList();
        for (int i = 0; i < venuesList.size(); i++) {
            // make a list of the venus that are loaded in the list.
            // show the name, the category and the city
            String info = "Name: " + venuesList.get(i).placename + "\nAddress: " + venuesList.get(i).address + "\nCategory: " + venuesList.get(i).category;
            info = info + "\nCoordinate: " + venuesList.get(i).latitude + ", " + venuesList.get(i).longitude
                    + "\nDistance: " + getDistanceInStr(venuesList.get(i).distance);
            Log.i("POI", info);
            addMarker(new LatLng(venuesList.get(i).latitude, venuesList.get(i).longitude), venuesList.get(i).placename, POI);
            listTitle.add(i, info);
        }

        // set the results to the list
        // and show them in the xml
        //myAdapter = new ArrayAdapter(GooglePlacesExample.this, R.layout.row_layout, R.id.listText, listTitle);
        //setListAdapter(myAdapter);
        layoutList.setVisibility(View.VISIBLE);
        ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, android.R.id.text1, listTitle);
        lvNearByList.setAdapter(listAdapter);
        listAdapter.notifyDataSetChanged();
    }

    private void showAddressDialog(){
        // get prompts.xml view
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.location_name_prompt, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);
        TextView title = (TextView) promptsView.findViewById(R.id.tvDialogTitle);
        title.setText("Key in the address(Must include Country/State for more precise result)");

        final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // get user input and set it to result
                                // edit text
                                if(userInput.getText().toString().length() > 0){
                                    try{
                                        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query="
                                                + URLEncoder.encode(userInput.getText().toString(), "UTF-8") + "&key=AIzaSyAhCYwkFcx2Wl-I5I2vPFf11Lw-8BsVGrk";
                                        Log.i("search api", url);
                                        googleplaces gp = new googleplaces();
                                        gp.isClockIn = false;
                                        gp.execute(url);
                                    }
                                    catch(UnsupportedEncodingException e){
                                        e.printStackTrace();
                                    }
                                }


                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    private void setAutoClockInPoint(PlaceInfo poi){
        locationName = poi.placename;
        Log.d("debug:" + getClass().getSimpleName(), locationName);
        LatLng latLng = new LatLng(poi.latitude, poi.longitude);

        locationList.add(latLng);
        location_Name_list.add(locationName);
        addMarker(latLng, locationName, AUTOCLOCKIN);
        addProximity(latLng, locationName, true);
        information.setText("");
    }

    private String getDistanceInStr(double distance){
        if(distance > 1000){
            distance = distance / 1000.00;
            return String.format("%.2f", distance) + "km";
        }
        else{
            return String.format("%.2f", distance) + "m";
        }
    }

    private void showLogDialog(){
        // get prompts.xml view
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.log, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);
        TextView content = (TextView) promptsView.findViewById(R.id.tv_logcontent);
        Log.d("log", pref.getString("log", ""));
        content.setText(pref.getString("log", ""));

        // set dialog message
        alertDialogBuilder
            .setCancelable(false)
            .setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void toastAndNotify(String msg, boolean NeedNotify){
        if(NeedNotify){
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Notification.Builder builder = new Notification.Builder(context)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Checkpoint")
                    .setAutoCancel(true)
                    .setVibrate(new long[]{1000, 1000, 200, 1000})
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent)
                    .setContentText(msg);

            Log.d("debug:" + getClass().getSimpleName(), msg);
            notificationManager.notify(1000, builder.build());
        }

        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

//======================================= AMMAR'S GEOFENCE ==========================================
    @Override
    public void onCameraChange(CameraPosition position) {
        // Makes sure the visuals remain when zoom changes.
        for(int i = 0; i < locationList.size(); i++) {
            mMap.addCircle(new CircleOptions().center(locationList.get(i))
                    .radius(mGeofenceRadius.get(0).intValue())
//                    .fillColor(0x40ff0000)
                    .strokeColor(Color.RED).strokeWidth(5));

        }
    }

    //======================================= AMMAR'S GEOFENCE ==========================================
}

class ListComparator{
    public static Comparator<PlaceInfo> DistanceComparator = new Comparator<PlaceInfo>(){
        @Override
        public int compare(PlaceInfo p1, PlaceInfo p2){
            return Double.compare(p1.distance, p2.distance);
        }
    };
}