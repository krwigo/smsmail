package com.example.smsmail;

import android.content.Context;
import android.util.Log;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public class WorkerHelper {

    private static final String IMMEDIATE_WORK_NAME = "sync_worker_immediate";
    private static final String PERIODIC_WORK_NAME = "sync_worker_periodic";

    public static void enqueueImmediateSync(Context context) {
        Log.d("SMSMAIL", "WorkerHelper.enqueueImmediateSync");

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(
            SmsWorker.class
        )
            .setConstraints(buildNetworkConstraints())
            .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        );
    }

    public static void ensurePeriodicSync(Context context) {
        Log.d("SMSMAIL", "WorkerHelper.ensurePeriodicSync");

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
            SmsWorker.class,
            15,
            TimeUnit.MINUTES
        )
            .setConstraints(buildNetworkConstraints())
            .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        );
    }

    public static void enqueueSmsWorker(Context context) {
        enqueueImmediateSync(context);
    }

    private static Constraints buildNetworkConstraints() {
        return new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();
    }
}
