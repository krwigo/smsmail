package com.example.smsmail;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.util.Log;

public class ConnectivityHelper {

    public static void register(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        );

        cm.registerDefaultNetworkCallback(
            new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    Log.d("SMSMAIL", "ConnectivityHelper.onAvailable");
                    WorkerHelper.enqueueSmsWorker(context);
                }

                @Override
                public void onLost(Network network) {
                    Log.d("SMSMAIL", "ConnectivityHelper.onLost");
                }
            }
        );
    }
}
