package com.cashbridge.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private EditText endpoint, token;
    private TextView status;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        endpoint = findViewById(R.id.endpoint);
        token = findViewById(R.id.token);
        status = findViewById(R.id.status);
        endpoint.setText(Prefs.get(this, "endpoint"));
        token.setText(Prefs.get(this, "token"));
        RetryScheduler.schedule(this);
        RetryScheduler.runSoon(this);
        refreshStatus();

        findViewById(R.id.save).setOnClickListener(v -> {
            Prefs.put(this, "endpoint", endpoint.getText().toString().trim());
            Prefs.put(this, "token", token.getText().toString().trim());
            Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show();
            RetryScheduler.runSoon(this);
            refreshStatus();
        });

        findViewById(R.id.permissions).setOnClickListener(v -> requestPermissions(
                new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS}, 10));

        findViewById(R.id.battery).setOnClickListener(v -> requestBatteryExemption());

        findViewById(R.id.test).setOnClickListener(v -> {
            status.setText("الحالة: جاري اختبار الاتصال...");
            ApiClient.sendTest(this, result -> runOnUiThread(() ->
                    status.setText(result.ok
                            ? "الحالة: الاتصال ناجح\nHTTP " + result.code + "\n" + result.message
                            : "الحالة: فشل الاتصال\nHTTP " + result.code + "\n" + result.message)));
        });
    }

    @Override protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void requestBatteryExemption() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        if (power != null && power.isIgnoringBatteryOptimizations(getPackageName())) {
            Toast.makeText(this, "التطبيق مستثنى بالفعل من توفير البطارية", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        }
    }

    private void refreshStatus() {
        boolean configured = !Prefs.get(this, "endpoint").isEmpty() && !Prefs.get(this, "token").isEmpty();
        boolean granted = checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
        String last = Prefs.get(this, "last_result");
        int pending = PendingQueue.size(this);
        status.setText("الحالة: " + (configured ? "تم إدخال الرابط" : "أدخل الرابط والمفتاح") +
                " — الرسائل: " + (granted ? "مسموح" : "غير مسموح") +
                "\nانتظار الإرسال: " + pending +
                (last.isEmpty() ? "" : "\nآخر إرسال: " + last));
    }
}
