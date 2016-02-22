package com.example.yyping.gps;

/**
 * Created by Admin on 5/2/2016.
 */

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

public class GeofenceIntentService extends IntentService {


    private final String TAG = this.getClass().getCanonicalName();

    public GeofenceIntentService() {
        super("GeofenceIntentService");
        Log.v(TAG, "Constructor.");
    }

    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate");
    }

    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        Log.v(TAG, "onHandleIntent");
        if(!geofencingEvent.hasError()) {
            int transition = geofencingEvent.getGeofenceTransition();
            String notificationTitle;

                switch (transition) {
                    case Geofence.GEOFENCE_TRANSITION_ENTER:
                        notificationTitle = "You Have Entered";
                        Log.v(TAG, "Geofence Entered");

                        break;
                    case Geofence.GEOFENCE_TRANSITION_DWELL:
                        notificationTitle = "You Already Entered";
                        Log.v(TAG, "Dwelling in Geofence");
                        break;
                    case Geofence.GEOFENCE_TRANSITION_EXIT:
                        notificationTitle = "You Have Exited";
                        Log.v(TAG, "Geofence Exited");
                        break;
                    default:
                        notificationTitle = "Geofence Unknown";
                }
                sendNotification(this, getTriggeringGeofences(intent), notificationTitle);

            }
        }

    //SEND NOTIFICATION METHOD
    private void sendNotification(Context context, String notificationText, String notificationTitle) {

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
        wakeLock.acquire();

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setSound(defaultSoundUri)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(false);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notificationBuilder.build());

        wakeLock.release();
    }

    private String getTriggeringGeofences(Intent intent) {
        GeofencingEvent geofenceEvent = GeofencingEvent.fromIntent(intent);
        List<Geofence> geofences = geofenceEvent
                .getTriggeringGeofences();

        String[] geofenceIds = new String[geofences.size()];

        for (int i = 0; i < geofences.size(); i++) {
            geofenceIds[i] = geofences.get(i).getRequestId();
        }

        return TextUtils.join(", ", geofenceIds);
    }
}
