package com.example.smsmail;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class SmsWorker extends Worker {

    // called by:
    // OneTimeWorkRequest

    public SmsWorker(
        @NonNull Context context,
        @NonNull WorkerParameters params
    ) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("SMSMAIL", "SmsWorker.doWork");
        try {
            SyncManager.SyncResult result = SyncManager.run(
                getApplicationContext()
            );
            switch (result) {
                case SUCCESS:
                    return Result.success();
                case CONFIG_ERROR:
                    return Result.failure();
                case RETRY:
                default:
                    return Result.retry();
            }
        } catch (Exception e) {
            Log.e("SMSMAIL", "SmsWorker.doWork failed", e);
            return Result.retry();
        }
    }
}
