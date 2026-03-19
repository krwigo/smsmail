package com.example.smsmail;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.BatteryManager;
import android.provider.CallLog;
import android.os.Build;
import android.util.Log;

public class SyncManager {

    private static final String PREF_NAME = "config";
    private static final String KEY_LAST_TIMESTAMP = "lastTimestamp";
    private static final String KEY_LAST_ID = "lastId";
    private static final String KEY_LAST_CALL_TIMESTAMP = "lastCallTimestamp";
    private static final String KEY_LAST_CALL_ID = "lastCallId";
    private static final String KEY_BATTERY_LOW_PENDING = "batteryLowPending";
    private static final String KEY_BATTERY_ALERT_SENT = "batteryAlertSent";
    private static final int BATTERY_RECOVERY_PERCENT = 20;

    public enum SyncResult {
        SUCCESS,
        RETRY,
        CONFIG_ERROR,
    }

    public static synchronized SyncResult run(Context context) {
        Log.d("SMSMAIL", "SyncManager.run");
        SharedPreferences prefs = context.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        );
        refreshBatteryAlertState(context, prefs);

        SyncResult batteryResult = processPendingBatteryAlert(context, prefs);
        if (batteryResult != SyncResult.SUCCESS) {
            return batteryResult;
        }

        SyncResult smsResult = processSmsInbox(context, prefs);
        if (smsResult != SyncResult.SUCCESS) {
            return smsResult;
        }

        return processMissedCalls(context, prefs);
    }

    public static void markBatteryLowPending(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        );
        prefs.edit().putBoolean(KEY_BATTERY_LOW_PENDING, true).apply();
    }

    private static SyncResult processPendingBatteryAlert(
        Context context,
        SharedPreferences prefs
    ) {
        if (!prefs.getBoolean(KEY_BATTERY_LOW_PENDING, false)) {
            return SyncResult.SUCCESS;
        }

        if (prefs.getBoolean(KEY_BATTERY_ALERT_SENT, false)) {
            prefs.edit().putBoolean(KEY_BATTERY_LOW_PENDING, false).apply();
            return SyncResult.SUCCESS;
        }

        int batteryPercent = getBatteryPercent(context);
        if (batteryPercent > BATTERY_RECOVERY_PERCENT) {
            prefs
                .edit()
                .putBoolean(KEY_BATTERY_LOW_PENDING, false)
                .putBoolean(KEY_BATTERY_ALERT_SENT, false)
                .apply();
            return SyncResult.SUCCESS;
        }

        Mailer.DeliveryResult result = Mailer.sendBatteryLowMail(
            context,
            batteryPercent
        );
        SyncResult mappedResult = mapDeliveryResult(result);
        if (mappedResult == SyncResult.SUCCESS) {
            prefs
                .edit()
                .putBoolean(KEY_BATTERY_LOW_PENDING, false)
                .putBoolean(KEY_BATTERY_ALERT_SENT, true)
                .apply();
        }
        return mappedResult;
    }

    private static SyncResult processSmsInbox(
        Context context,
        SharedPreferences prefs
    ) {
        if (!hasPermission(context, Manifest.permission.READ_SMS)) {
            Log.w("SMSMAIL", "Skipping SMS sync: READ_SMS not granted");
            return SyncResult.SUCCESS;
        }

        long lastTs = getLongPreference(prefs, KEY_LAST_TIMESTAMP, 0);
        long lastId = prefs.getLong(KEY_LAST_ID, 0);
        Uri smsUri = Uri.parse("content://sms/inbox");
        String[] projection = new String[] { "_id", "address", "date", "body" };
        String selection = "(date > ?) OR (date = ? AND _id > ?)";
        String[] selectionArgs = new String[] {
            String.valueOf(lastTs),
            String.valueOf(lastTs),
            String.valueOf(lastId),
        };
        String sortOrder = "date ASC, _id ASC";

        try (
            Cursor cursor = context
                .getContentResolver()
                .query(smsUri, projection, selection, selectionArgs, sortOrder)
        ) {
            if (cursor == null) {
                return SyncResult.SUCCESS;
            }

            while (cursor.moveToNext()) {
                long smsId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
                String from = cursor.getString(
                    cursor.getColumnIndexOrThrow("address")
                );
                long timestamp = cursor.getLong(
                    cursor.getColumnIndexOrThrow("date")
                );
                String body = cursor.getString(
                    cursor.getColumnIndexOrThrow("body")
                );

                SyncResult sendResult = mapDeliveryResult(
                    Mailer.sendSmsMail(context, from, body, timestamp)
                );
                if (sendResult != SyncResult.SUCCESS) {
                    return sendResult;
                }

                prefs
                    .edit()
                    .putString(KEY_LAST_TIMESTAMP, Long.toString(timestamp))
                    .putLong(KEY_LAST_ID, smsId)
                    .apply();

                markSmsRead(context, smsId);
            }
        } catch (Exception e) {
            Log.e("SyncManager", "Exception while syncing SMS", e);
            return SyncResult.RETRY;
        }

        return SyncResult.SUCCESS;
    }

    private static SyncResult processMissedCalls(
        Context context,
        SharedPreferences prefs
    ) {
        if (!hasPermission(context, Manifest.permission.READ_CALL_LOG)) {
            Log.w("SMSMAIL", "Skipping missed-call sync: READ_CALL_LOG not granted");
            return SyncResult.SUCCESS;
        }

        if (!prefs.contains(KEY_LAST_CALL_TIMESTAMP) || !prefs.contains(KEY_LAST_CALL_ID)) {
            initializeMissedCallCheckpoint(context, prefs);
            return SyncResult.SUCCESS;
        }

        long lastCallTs = prefs.getLong(KEY_LAST_CALL_TIMESTAMP, 0);
        long lastCallId = prefs.getLong(KEY_LAST_CALL_ID, 0);
        String[] projection = new String[] {
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.DATE,
        };
        String selection =
            CallLog.Calls.TYPE +
            " = ? AND ((" +
            CallLog.Calls.DATE +
            " > ?) OR (" +
            CallLog.Calls.DATE +
            " = ? AND " +
            CallLog.Calls._ID +
            " > ?))";
        String[] selectionArgs = new String[] {
            String.valueOf(CallLog.Calls.MISSED_TYPE),
            String.valueOf(lastCallTs),
            String.valueOf(lastCallTs),
            String.valueOf(lastCallId),
        };
        String sortOrder = CallLog.Calls.DATE + " ASC, " + CallLog.Calls._ID + " ASC";

        try (
            Cursor cursor = context
                .getContentResolver()
                .query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )
        ) {
            if (cursor == null) {
                return SyncResult.SUCCESS;
            }

            while (cursor.moveToNext()) {
                long callId = cursor.getLong(
                    cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
                );
                String number = cursor.getString(
                    cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                );
                String cachedName = cursor.getString(
                    cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                );
                long timestamp = cursor.getLong(
                    cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                );

                SyncResult sendResult = mapDeliveryResult(
                    Mailer.sendMissedCallMail(
                        context,
                        number != null ? number : "Unknown",
                        cachedName,
                        timestamp
                    )
                );
                if (sendResult != SyncResult.SUCCESS) {
                    return sendResult;
                }

                prefs
                    .edit()
                    .putLong(KEY_LAST_CALL_TIMESTAMP, timestamp)
                    .putLong(KEY_LAST_CALL_ID, callId)
                    .apply();
            }
        } catch (Exception e) {
            Log.e("SyncManager", "Exception while syncing missed calls", e);
            return SyncResult.RETRY;
        }

        return SyncResult.SUCCESS;
    }

    private static void initializeMissedCallCheckpoint(
        Context context,
        SharedPreferences prefs
    ) {
        String[] projection = new String[] {
            CallLog.Calls._ID,
            CallLog.Calls.DATE,
        };

        try (
            Cursor cursor = context
                .getContentResolver()
                .query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    CallLog.Calls.TYPE + " = ?",
                    new String[] { String.valueOf(CallLog.Calls.MISSED_TYPE) },
                    CallLog.Calls.DATE + " DESC, " + CallLog.Calls._ID + " DESC"
                )
        ) {
            if (cursor == null || !cursor.moveToFirst()) {
                prefs
                    .edit()
                    .putLong(KEY_LAST_CALL_TIMESTAMP, 0)
                    .putLong(KEY_LAST_CALL_ID, 0)
                    .apply();
                return;
            }

            prefs
                .edit()
                .putLong(
                    KEY_LAST_CALL_TIMESTAMP,
                    cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE))
                )
                .putLong(
                    KEY_LAST_CALL_ID,
                    cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls._ID))
                )
                .apply();
        } catch (Exception e) {
            Log.e("SyncManager", "Exception while initializing missed-call checkpoint", e);
        }
    }

    private static void refreshBatteryAlertState(
        Context context,
        SharedPreferences prefs
    ) {
        int batteryPercent = getBatteryPercent(context);
        if (batteryPercent > BATTERY_RECOVERY_PERCENT) {
            prefs
                .edit()
                .putBoolean(KEY_BATTERY_LOW_PENDING, false)
                .putBoolean(KEY_BATTERY_ALERT_SENT, false)
                .apply();
        }
    }

    private static void markSmsRead(Context context, long smsId) {
        try {
            Uri uri = Uri.parse("content://sms/" + smsId);
            ContentValues values = new ContentValues();
            values.put("read", 1);
            values.put("seen", 1);
            int updatedRows = context
                .getContentResolver()
                .update(uri, values, null, null);
            Log.d(
                "SyncManager",
                "markSmsRead updatedRows=" + updatedRows + " smsId=" + smsId
            );
        } catch (Exception e) {
            Log.e("SyncManager", "Exception while marking SMS as read", e);
        }
    }

    private static SyncResult mapDeliveryResult(Mailer.DeliveryResult result) {
        switch (result) {
            case SUCCESS:
                return SyncResult.SUCCESS;
            case CONFIG_ERROR:
                return SyncResult.CONFIG_ERROR;
            case RETRYABLE_FAILURE:
            default:
                return SyncResult.RETRY;
        }
    }

    private static long getLongPreference(
        SharedPreferences prefs,
        String key,
        long defaultValue
    ) {
        try {
            return Long.parseLong(prefs.getString(key, Long.toString(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean hasPermission(Context context, String permission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        return (
            context.checkSelfPermission(permission) ==
            PackageManager.PERMISSION_GRANTED
        );
    }

    private static int getBatteryPercent(Context context) {
        Intent intent = context.registerReceiver(
            null,
            new IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        );
        if (intent == null) {
            return 100;
        }

        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level < 0 || scale <= 0) {
            return 100;
        }
        return Math.round((level * 100f) / scale);
    }
}
