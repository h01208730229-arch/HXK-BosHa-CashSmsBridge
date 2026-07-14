package com.cashbridge.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Small persistent FIFO queue stored in SharedPreferences. */
public final class PendingQueue {
    private static final String KEY = "pending_messages";

    private PendingQueue() {}

    public static synchronized void add(Context context, CashMessage message) {
        try {
            JSONArray queue = read(context);
            String tx = message.transactionId == null ? "" : message.transactionId;
            for (int i = 0; i < queue.length(); i++) {
                if (tx.equals(queue.optJSONObject(i).optString("transaction_id"))) return;
            }
            JSONObject item = new JSONObject();
            item.put("provider", message.provider);
            item.put("sms_sender", message.senderId);
            item.put("sender_phone", message.senderPhone);
            item.put("receiver_phone", message.receiverPhone);
            item.put("amount", message.amount);
            item.put("transaction_id", message.transactionId);
            item.put("sms_body", message.rawText);
            item.put("received_at", message.receivedAt);
            queue.put(item);
            write(context, queue);
        } catch (Exception ignored) {
        }
    }

    public static synchronized List<JSONObject> all(Context context) {
        List<JSONObject> result = new ArrayList<>();
        JSONArray queue = read(context);
        for (int i = 0; i < queue.length(); i++) {
            JSONObject item = queue.optJSONObject(i);
            if (item != null) result.add(item);
        }
        return result;
    }

    public static synchronized void removeTransaction(Context context, String transactionId) {
        JSONArray queue = read(context);
        JSONArray kept = new JSONArray();
        for (int i = 0; i < queue.length(); i++) {
            JSONObject item = queue.optJSONObject(i);
            if (item == null) continue;
            if (!transactionId.equals(item.optString("transaction_id"))) kept.put(item);
        }
        write(context, kept);
    }

    public static synchronized int size(Context context) {
        return read(context).length();
    }

    private static JSONArray read(Context context) {
        try {
            String raw = Prefs.get(context, KEY);
            return raw.isEmpty() ? new JSONArray() : new JSONArray(raw);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private static void write(Context context, JSONArray queue) {
        Prefs.put(context, KEY, queue.toString());
    }
}
