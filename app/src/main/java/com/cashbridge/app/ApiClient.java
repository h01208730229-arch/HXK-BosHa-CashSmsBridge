package com.cashbridge.app;

import android.content.Context;
import android.provider.Settings;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ApiClient {
    interface Callback { void done(Result result); }

    static final class Result {
        final boolean ok;
        final int code;
        final String message;

        Result(boolean ok, int code, String message) {
            this.ok = ok;
            this.code = code;
            this.message = message == null ? "" : message;
        }
    }

    private ApiClient() {}

    static void enqueueAndSend(Context context, CashMessage message) {
        PendingQueue.add(context, message);
        Prefs.put(context, "last_result", "تم حفظ الرسالة في انتظار الإرسال");
        RetryScheduler.runSoon(context);
    }

    static void flushPending(Context context, Runnable finished) {
        new Thread(() -> {
            try {
                List<JSONObject> items = PendingQueue.all(context);
                for (JSONObject item : items) {
                    Result result;
                    try {
                        JSONObject payload = new JSONObject(item.toString());
                        payload.put("device_id", deviceId(context));
                        result = post(context, payload);
                    } catch (Exception e) {
                        Prefs.put(context, "last_result", "خطأ إرسال: " + e.getClass().getSimpleName() + " — " + e.getMessage());
                        break;
                    }

                    Prefs.put(context, "last_result", "HTTP " + result.code + " — " + result.message);
                    if (result.ok) {
                        PendingQueue.removeTransaction(context, item.optString("transaction_id"));
                    } else {
                        break;
                    }
                }
            } finally {
                RetryScheduler.schedule(context);
                if (finished != null) finished.run();
            }
        }).start();
    }

    static void sendTest(Context context, Callback callback) {
        new Thread(() -> {
            Result result;
            try {
                JSONObject json = new JSONObject();
                json.put("test", true);
                result = post(context, json);
                if (!result.ok) {
                    Result ping = getPing(context);
                    if (ping.ok) {
                        result = new Result(false, result.code,
                                "السيرفر متصل لكن POST مرفوض: " + result.message + " | GET: " + ping.message);
                    }
                }
            } catch (Exception e) {
                result = new Result(false, 0, e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            callback.done(result);
        }).start();
    }

    private static Result post(Context context, JSONObject json) throws Exception {
        String endpoint = normalizeEndpoint(Prefs.get(context, "endpoint"));
        String apiKey = Prefs.get(context, "token").trim();
        if (!endpoint.startsWith("https://")) return new Result(false, 0, "الرابط لازم يبدأ بـ https://");
        if (apiKey.isEmpty()) return new Result(false, 0, "مفتاح الحماية فارغ");

        byte[] body = json.toString().getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(20000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("X-API-Key", apiKey);
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);

        try (OutputStream output = connection.getOutputStream()) {
            output.write(body);
        }

        int code = connection.getResponseCode();
        String response = readResponse(connection, code);
        connection.disconnect();
        return new Result(code >= 200 && code < 300, code, response);
    }

    private static Result getPing(Context context) throws Exception {
        String endpoint = normalizeEndpoint(Prefs.get(context, "endpoint"));
        String apiKey = Prefs.get(context, "token").trim();
        String url = endpoint + (endpoint.contains("?") ? "&" : "?") + "api_key=" +
                URLEncoder.encode(apiKey, "UTF-8");
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("X-API-Key", apiKey);
        int code = connection.getResponseCode();
        String response = readResponse(connection, code);
        connection.disconnect();
        return new Result(code >= 200 && code < 300, code, response);
    }

    private static String readResponse(HttpURLConnection connection, int code) {
        try {
            InputStream stream = code >= 200 && code < 400
                    ? connection.getInputStream() : connection.getErrorStream();
            if (stream == null) return "بدون رد من السيرفر";
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
            return result.toString();
        } catch (Exception e) {
            return "تعذر قراءة رد السيرفر: " + e.getMessage();
        }
    }

    private static String normalizeEndpoint(String endpoint) {
        String value = endpoint == null ? "" : endpoint.trim();
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }

    private static String deviceId(Context context) {
        String id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return id == null ? "android-device" : id;
    }
}
