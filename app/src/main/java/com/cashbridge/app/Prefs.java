package com.cashbridge.app;
import android.content.Context;
public final class Prefs {
    static String get(Context c, String key) { return c.getSharedPreferences("cash_bridge", 0).getString(key, ""); }
    static void put(Context c, String key, String value) { c.getSharedPreferences("cash_bridge", 0).edit().putString(key, value).apply(); }
}
