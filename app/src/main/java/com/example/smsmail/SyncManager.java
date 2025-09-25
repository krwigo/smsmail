package com.example.smsmail;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class SyncManager {

    // called by:
    // SmsWorker

    // the execution chain is:
    // BOOT_COMPLETED / SMS_RECEIVED / Button press
    //   → WorkerHelper.enqueueSmsWorker()
    //     → WorkManager.enqueueUniqueWork()
    //       → SmsWorker.doWork()
    //         → SyncManager.run(context)

    // https://android.googlesource.com/platform/packages/providers/TelephonyProvider/%2B/master/src/com/android/providers/telephony/MmsSmsDatabaseHelper.java?utm_source=chatgpt.com#1095
    // public static String CREATE_SMS_TABLE_STRING =
    //         "CREATE TABLE sms (" +
    //         "_id INTEGER PRIMARY KEY," +
    //         "thread_id INTEGER," +
    //         "address TEXT," +
    //         "person INTEGER," +
    //         "date INTEGER," +
    //         "date_sent INTEGER DEFAULT 0," +
    //         "protocol INTEGER," +
    //         "read INTEGER DEFAULT 0," +
    //         "status INTEGER DEFAULT -1," + // a TP-Status value or -1 if status hasn't been received
    //         "type INTEGER," +
    //         "reply_path_present INTEGER," +
    //         "subject TEXT," +
    //         "body TEXT," +
    //         "service_center TEXT," +
    //         "locked INTEGER DEFAULT 0," +
    //         "sub_id INTEGER DEFAULT " + SubscriptionManager.INVALID_SUBSCRIPTION_ID + ", " +
    //         "error_code INTEGER DEFAULT " + NO_ERROR_CODE + ", " +
    //         "creator TEXT," +
    //         "seen INTEGER DEFAULT 0" +
    //         ");";

    public static void run(Context context) {
        Log.d("SMSMAIL", "SyncManager.run");
        SharedPreferences prefs = context.getSharedPreferences(
            "config",
            Context.MODE_PRIVATE
        );
        long lastTs = Long.parseLong(prefs.getString("lastTimestamp", "0"));
        long lastId = prefs.getLong("lastId", 0);

        Uri smsUri = Uri.parse("content://sms/inbox");
        // String[] projection = new String[] { "address", "date", "body" };
        String[] projection = new String[] { "_id", "address", "date", "body" };
        String selection = "date > ?";
        // String selection = "_id > ?";
        String[] selectionArgs = new String[] { String.valueOf(lastTs) };
        // String[] selectionArgs = new String[] { String.valueOf(lastId) };
        String sortOrder = "date ASC";
        // String sortOrder = "_id ASC";

        try (
            Cursor cursor = context
                .getContentResolver()
                .query(smsUri, projection, selection, selectionArgs, sortOrder)
        ) {
            if (cursor == null) return;

            while (cursor.moveToNext()) {
                long _id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
                String from = cursor.getString(
                    cursor.getColumnIndexOrThrow("address")
                );
                long timestamp = cursor.getLong(
                    cursor.getColumnIndexOrThrow("date")
                );
                String body = cursor.getString(
                    cursor.getColumnIndexOrThrow("body")
                );

                boolean success = Mailer.sendMail(
                    context,
                    from,
                    body,
                    timestamp
                );
                if (success) {
                    // save last success
                    prefs
                        .edit()
                        .putString("lastTimestamp", Long.toString(timestamp))
                        .putLong("lastId", _id)
                        .apply();

                    // mark as read
                    try {
                        Uri uri = Uri.withAppendedPath(
                            smsUri,
                            String.valueOf(_id)
                        );
                        ContentValues values = new ContentValues();
                        values.put("read", 1);
                        context
                            .getContentResolver()
                            .update(uri, values, null, null);
                    } catch (Exception e) {
                        Log.e(
                            "SyncManager",
                            "Exception while marking as read",
                            e
                        );
                    }
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("SyncManager", "Exception in run()", e);
            // ui thread
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() ->
                Toast.makeText(
                    context,
                    "Error: " + e.getMessage(),
                    Toast.LENGTH_LONG
                ).show()
            );
        }
    }
}
