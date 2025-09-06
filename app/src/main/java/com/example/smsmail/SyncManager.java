package com.example.smsmail;

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

    public static void run(Context context) {
        Log.d("SMSMAIL", "SyncManager.run");
        SharedPreferences prefs = context.getSharedPreferences(
            "config",
            Context.MODE_PRIVATE
        );
        long lastTs = Long.parseLong(prefs.getString("lastTimestamp", "0"));

        Uri smsUri = Uri.parse("content://sms/inbox");
        String[] projection = new String[] { "address", "date", "body" };
        String selection = "date > ?";
        String[] selectionArgs = new String[] { String.valueOf(lastTs) };
        String sortOrder = "date ASC";

        try (
            Cursor cursor = context
                .getContentResolver()
                .query(smsUri, projection, selection, selectionArgs, sortOrder)
        ) {
            if (cursor == null) return;

            while (cursor.moveToNext()) {
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
                    prefs
                        .edit()
                        .putString("lastTimestamp", Long.toString(timestamp))
                        .apply();
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
