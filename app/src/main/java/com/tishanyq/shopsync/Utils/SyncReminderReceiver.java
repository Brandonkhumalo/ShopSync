package com.tishanyq.shopsync.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.tishanyq.shopsync.Database.SyncManager;
import java.util.Calendar;

public class SyncReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "sync_reminder";

    @Override
    public void onReceive(Context context, Intent intent) {
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        // Monday = 2, Wednesday = 4, Friday = 6
        if (dayOfWeek == Calendar.MONDAY || dayOfWeek == Calendar.WEDNESDAY || dayOfWeek == Calendar.FRIDAY) {
            SyncManager syncManager = new SyncManager(context);
            if (syncManager.getDaysSinceLastSync() > 0) {
                showSyncNotification(context);
            }
        }
    }

    private void showSyncNotification(Context context) {
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Sync Reminders", NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("FrostSync POS")
                .setContentText("Please sync your data to avoid data loss")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        manager.notify(1, builder.build());
    }
}
