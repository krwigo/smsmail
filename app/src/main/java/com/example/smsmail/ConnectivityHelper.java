package com.example.smsmail;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;
import android.util.Log;

public class ConnectivityHelper {

    private static boolean isRegistered;

    public static void register(Context context) {
        if (isRegistered) {
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.d("SMSMAIL", "ConnectivityHelper.register skipped on API < 24");
            return;
        }

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        );
        if (cm == null) {
            return;
        }

        isRegistered = true;

        cm.registerDefaultNetworkCallback(
            new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    Log.d("SMSMAIL", "ConnectivityHelper.onAvailable");
                    WorkerHelper.enqueueImmediateSync(context);
                }

                @Override
                public void onLost(Network network) {
                    Log.d("SMSMAIL", "ConnectivityHelper.onLost");
                }
            }
        );
    }
}
