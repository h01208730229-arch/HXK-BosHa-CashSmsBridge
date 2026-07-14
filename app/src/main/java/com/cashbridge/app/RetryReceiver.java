package com.cashbridge.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RetryReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        final PendingResult pendingResult = goAsync();
        ApiClient.flushPending(context.getApplicationContext(), pendingResult::finish);
    }
}
