package com.example.yyping.gps;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;


public class ProximityIntentReceiver extends BroadcastReceiver {
    private static final  int NOTIFICATION_ID = 1000;


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onReceive(Context context, Intent intent) {

        /*String key = LocationManager.KEY_PROXIMITY_ENTERING;
        String enterOrExit = "Entered";
        Boolean entering = intent.getBooleanExtra(key, false);
        String checkpoint = intent.getExtras().getString("checkpoint");
        String message;

        MainActivity ac= new MainActivity ();
        if (!entering) {
            enterOrExit = "Exit";
        }

        SharedPreferences pref = MainActivity.AlretRegion.getSharedPreferences("TimeTec_GPS", Context.MODE_PRIVATE);
        Calendar c = Calendar.getInstance();
        String name = checkpoint + String.valueOf(android.text.format.DateFormat.format("yyyyMMdd", c)) + enterOrExit;
        String exist = pref.getString(name, "");
        Log.d("exist - " + exist, name);
        if(exist.length() == 0){
            Log.d(getClass().getSimpleName(), enterOrExit);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Notification.Builder builder = new Notification.Builder(context)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Checkpoint")
                    .setAutoCancel(true)
                    .setVibrate(new long[] { 1000, 1000, 200, 1000})
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent)
                    .setContentText("You have " + enterOrExit + " a '"+ checkpoint +"' region");

            Log.d("debug:" + getClass().getSimpleName(), "You have " + enterOrExit + " a '" + checkpoint + "' region");

            //Notification notification = createNotification(context, pendingIntent, enterOrExit);
            notificationManager.notify(NOTIFICATION_ID,builder.build());
            message = "You Are " + enterOrExit + " Region!!!";
            ac.showAlertDialog(MainActivity.AlretRegion, message);

            //Save into log
            String currentDatetime = String.valueOf(android.text.format.DateFormat.format("dd/MM/yyyy HH:mm:ss", c));
            String log = pref.getString("log", "");
            if(log.length() > 0)
                log = log + "\n";

            log = log + "You have " + enterOrExit + " a '"+ checkpoint +"' region on " + currentDatetime;
            SharedPreferences.Editor e = pref.edit();
            e.putString("log", log);
            e.putString(name, "got");

            //Remove entered record if user exiting/remove exit record if user entering
            if (!entering)
                enterOrExit = "Entered";
            else
                enterOrExit = "Exit";

            String removelog = checkpoint + String.valueOf(android.text.format.DateFormat.format("yyyyMMdd", c)) + enterOrExit;
            Log.d("removelog", removelog);
            e.putString(removelog, "");
            e.commit();

            Toast.makeText(MainActivity.AlretRegion, log, Toast.LENGTH_SHORT).show();
        }*/
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private Notification createNotification(Context context, PendingIntent pendingIntent, String enterOrExit) {
        // Use new API
        Notification.Builder builder = new Notification.Builder(context)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Checkpoint")
                .setContentText("You have "+enterOrExit+" a designated region");
        return builder.build();
    }




}
