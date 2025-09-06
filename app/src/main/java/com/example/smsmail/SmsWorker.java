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
            Log.d("SMSMAIL", "SmsWorker.doWork before");
            SyncManager.run(getApplicationContext());
            Log.d("SMSMAIL", "SmsWorker.doWork after");
            return Result.success();
        } catch (Exception e) {
            Log.d("SMSMAIL", "SmsWorker.doWork catch");
            e.printStackTrace();
            return Result.retry();
        }
    }
}
