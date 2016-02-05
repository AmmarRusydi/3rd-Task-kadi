package com.example.yyping.gps;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Calendar;

/*
 * Created by yyping on 25/11/2015.
 */

public class LocationService extends Service {
    private static final int INITIAL_REQUEST = 1;
    private static final int LOCATION_REQUEST = INITIAL_REQUEST + 3;
    private static final int LASTKNOWN_REQUEST = INITIAL_REQUEST + 4;

    public static final int SECONDS_TO_UP = 1000 * 10; //10 seconds
    public static final int METERS_TO_UP = 1;

    int mIsFinish = 0;
    Location newLocation;
    String provider;
    boolean isNetwork = false;

    private LocationManager mLocationMgr;
    private ArrayList<Checkpoint> locationList = new ArrayList<>();
    private Context context = this;

    class Checkpoint {
        LatLng coordinate;
        double dis;
        String locname;

        public Checkpoint(LatLng ll, double j, String k) {
            coordinate = ll;
            dis = j;
            locname = k;
        }

        public double getDistance() {
            return dis;
        }

        public void setDistance(double j) {
            dis = j;
        }

        public String getLocation() {
            return locname;
        }

        public LatLng getCoordinate() {
            return coordinate;
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mLocationMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
        setup();
    }

    @Override
    public void onDestroy() {
        Log.e("service", "OnDestroystart");
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
        mLocationMgr.removeUpdates(mGpslistener);
        mLocationMgr.removeUpdates(mNetworklistener);
        super.onDestroy();
    }

    private final LocationListener mGpslistener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            updateUILocation(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
            provider = LocationManager.NETWORK_PROVIDER;
            Log.e("Service GPS", "Gps disabled");
        }

        public void onProviderEnabled(String provider) {
            provider = LocationManager.GPS_PROVIDER;
            Log.e("Service GPS", "Gps enabled");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e("Service GPS", "provider - " + provider);
        }
    };

    private final LocationListener mNetworklistener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (!provider.equals(LocationManager.GPS_PROVIDER)) {
                updateUILocation(location);
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mLocationMgr.removeUpdates(mNetworklistener);
            isNetwork = false;
        }

        public void onProviderEnabled(String provider) {
            if (mLocationMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                isNetwork = true;
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                mLocationMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, SECONDS_TO_UP, METERS_TO_UP, mNetworklistener);
            } else {
                isNetwork = false;
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    private void updateUILocation(Location location) {
        SharedPreferences pref = getSharedPreferences("TimeTec_GPS", Context.MODE_PRIVATE);
        /*SharedPreferences.Editor editor = pref.edit();
        editor.putString("current_lat", String.valueOf(location.getLatitude()));
        editor.putString("current_lng", String.valueOf(location.getLongitude()));
        editor.commit();*/

        Checkpoint cp = null;
        LatLng latlng = null;
        for (int i = 0; i < locationList.size(); i++) {
            cp = locationList.get(i);
            latlng = cp.getCoordinate();

            float latitude = (float) location.getLatitude();
            float longitude = (float) location.getLongitude();

            float latfixpoit = (float) latlng.latitude;
            float longfixpoint = (float) latlng.longitude;

            double distance = distFrom(latitude, longitude, latfixpoit, longfixpoint);
            locationList.get(i).setDistance(distance);
        }

        boolean result = false;
        Calendar cal = Calendar.getInstance();
        String currentDate = String.valueOf(android.text.format.DateFormat.format("yyyyMMdd", cal));
        String currentTime = String.valueOf(android.text.format.DateFormat.format("HHmm", cal));
        String currentDatetime = String.valueOf(android.text.format.DateFormat.format("dd/MM/yyyy HH:mm:ss", cal));

        for (int i = 0; i < locationList.size(); i++) {
            cp = locationList.get(i);
            String info = cp.getLocation() + currentDate;
            String enterOrExit = "Enter";
            String enterExist = pref.getString(info + "Enter", "");
            String lastEnterTime = pref.getString(info + "LastEnter", "");
            String exitExist = pref.getString(info + "Exit", "");
            String lastExitTime = pref.getString(info + "LastExit", "");
            Log.d("service distance - " + String.valueOf(cp.getDistance()), enterOrExit);
            Log.d("service enter - " + lastEnterTime, "exit - " + lastExitTime);

            if (cp.getDistance() >= 500) { //outside the region now
                //Only trigger when user got enter this region before and haven't exit before
                if (enterExist.length() > 0 && exitExist.length() == 0 && (lastExitTime.length() == 0 || !lastExitTime.equalsIgnoreCase(currentTime))) {
                    result = true;
                    enterOrExit = "Exit";
                    Log.d("service exit - " + currentTime, "trigger - " + String.valueOf(!exitExist.equalsIgnoreCase(currentTime)));
                }
            } else { //within the region now
                //Only trigger if user no enter this region before
                if (enterExist.length() == 0 && (lastEnterTime.length() == 0 || !lastEnterTime.equalsIgnoreCase(currentTime))) {
                    result = true;
                    enterOrExit = "Enter";
                    Log.d("service enter - " + currentTime, "trigger - " + String.valueOf(!enterExist.equalsIgnoreCase(currentTime)));
                }
            }

            if (result) {
                //alert user and save into log
                String msg = "You have " + enterOrExit + " '" + cp.getLocation() + "' region on " + currentDatetime;
                //toastAndNotify(msg);

                String log = pref.getString("log", "");
                if (log.length() > 0)
                    log = log + "\n";

                log = log + msg;
                SharedPreferences.Editor e = pref.edit();
                e.putString("log", log);
                if (enterOrExit.equalsIgnoreCase("Enter")) {
                    e.putString(info + "LastEnter", currentTime);
                    e.putString(info + "Enter", "got");
                    e.putString(info + "Exit", "");
                } else {
                    e.putString(info + "LastExit", currentTime);
                    e.putString(info + "Enter", "");
                    e.putString(info + "Exit", "got");
                }

                e.commit();
            } else {
                //toastAndNotify("Latitude = " + location.getLatitude() + ", Longitude = " + location.getLongitude());
            }
        }

        Log.e("Service-listener", "(" + location.getLatitude() + "," + location.getLongitude() + ")");
    }

    private void setup() {
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

        newLocation = mLocationMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (newLocation == null)
            newLocation = mLocationMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (mLocationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER))
            provider = LocationManager.GPS_PROVIDER;
        else
            provider = LocationManager.NETWORK_PROVIDER;

        mLocationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, SECONDS_TO_UP, METERS_TO_UP, mGpslistener);
        if (mLocationMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            isNetwork = true;
            mLocationMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, SECONDS_TO_UP, METERS_TO_UP, mNetworklistener);
        }
        else
            isNetwork = false;

        if (newLocation != null)
            updateUILocation(newLocation);
        else
            Log.d("location service", "empty location");

        if(!isNetwork)
            Log.d("location service", "network false");

        //Setup location list
        SharedPreferences pref = getSharedPreferences("TimeTec_GPS", Context.MODE_PRIVATE);
        int cpNumber = pref.getInt("cpnumber", 0);
        if(cpNumber > 0){
            for(int i = 0; i < cpNumber; i++){
                String name = "checkpoint" + String.valueOf(i);
                String info = pref.getString(name, "");

                Log.e(name, info);

                String[] checkpoint = info.split("_");
                LatLng ll = new LatLng(Double.parseDouble(checkpoint[1]), Double.parseDouble(checkpoint[2]));
                locationList.add(new Checkpoint(ll, 0, checkpoint[0]));
            }
        }
    }

    private float distFrom(float lat1, float lng1, float lat2, float lng2) {
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

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void toastAndNotify(String msg){
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Checkpoint")
                .setAutoCancel(true)
                .setVibrate(new long[] { 1000, 1000, 200, 1000})
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setContentText(msg);

        Log.d("debug:" + getClass().getSimpleName(), msg);
        notificationManager.notify(1000, builder.build());
    }
}
