package com.cashbridge.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public final class RetryScheduler {
    private static final long INTERVAL_MS = 15L * 60L * 1000L;

    private RetryScheduler() {}

    public static void schedule(Context context) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm == null) return;
        PendingIntent pending = PendingIntent.getBroadcast(
                context,
                7001,
                new Intent(context, RetryReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        long first = System.currentTimeMillis() + 30_000L;
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, first, INTERVAL_MS, pending);
    }

    public static void runSoon(Context context) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm == null) return;
        PendingIntent pending = PendingIntent.getBroadcast(
                context,
                7002,
                new Intent(context, RetryReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 2_000L, pending);
    }
}
