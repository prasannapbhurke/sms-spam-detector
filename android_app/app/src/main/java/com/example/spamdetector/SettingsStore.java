package com.example.spamdetector;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsStore {
    private static final String PREFS = "phase_settings";
    private static final String KEY_AUTO_SCAN = "auto_scan";
    private static final String KEY_SCAN_SCOPE = "scan_scope";

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isAutoScanEnabled(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_SCAN, true);
    }

    public static void setAutoScanEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_AUTO_SCAN, value).apply();
    }

    public static String getScanScope(Context context) {
        return prefs(context).getString(KEY_SCAN_SCOPE, "All");
    }

    public static void setScanScope(Context context, String scope) {
        prefs(context).edit().putString(KEY_SCAN_SCOPE, scope).apply();
    }
}
