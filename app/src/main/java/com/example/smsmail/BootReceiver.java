package com.example.smsmail;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("SMSMAIL", "BootReceiver.BootReceiver");
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("SMSMAIL", "BootReceiver.BootReceiver (equals)");
            WorkerHelper.enqueueSmsWorker(context);
        }
    }
}
