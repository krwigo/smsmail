package com.example.smsmail;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BatteryLowReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BATTERY_LOW.equals(intent.getAction())) {
            return;
        }

        Log.d("SMSMAIL", "BatteryLowReceiver.onReceive");
        SyncManager.markBatteryLowPending(context);
        WorkerHelper.enqueueImmediateSync(context);
    }
}
