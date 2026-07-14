package com.cashbridge.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class SmsReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;
        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages == null || messages.length == 0) return;

        String sender = messages[0].getDisplayOriginatingAddress();
        long receivedMillis = messages[0].getTimestampMillis();
        StringBuilder body = new StringBuilder();
        for (SmsMessage message : messages) body.append(message.getMessageBody());

        CashMessage parsed = CashParser.parse(sender, body.toString());
        if (parsed != null) {
            parsed.receivedAt = isoTime(receivedMillis);
            ApiClient.enqueueAndSend(context.getApplicationContext(), parsed);
        }
    }

    private static String isoTime(long millis) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
        format.setTimeZone(TimeZone.getDefault());
        return format.format(new Date(millis));
    }
}
