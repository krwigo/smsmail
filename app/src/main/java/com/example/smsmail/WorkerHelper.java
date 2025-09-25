package com.example.smsmail;

import android.content.Context;
import android.util.Log;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class WorkerHelper {

    // called by:
    // BOOT_COMPLETED
    // MainActivity
    // SmsReceiver

    public static void enqueueSmsWorker(Context context) {
        Log.d("SMSMAIL", "WorkerHelper.enqueueSmsWorker");

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(
            SmsWorker.class
        ).build();

        // KEEP: if a worker with the same name is running or queued, ignore this new request
        // APPEND: queue this request after the existing work chain, ensuring sequential execution

        WorkManager.getInstance(context).enqueueUniqueWork(
            "sms_worker",
            // ExistingWorkPolicy.KEEP,
            ExistingWorkPolicy.APPEND,
            request
        );
    }
}
