package com.example.smsmail;

import android.Manifest;
import android.app.Activity;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {

    private EditText host, port, user, pass, from, to, subject, lastTimestamp;
    private SharedPreferences prefs;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("SMSMAIL", "MainActivity.onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("config", MODE_PRIVATE);

        host = findViewById(R.id.input_host);
        port = findViewById(R.id.input_port);
        user = findViewById(R.id.input_user);
        pass = findViewById(R.id.input_pass);
        from = findViewById(R.id.input_from);
        to = findViewById(R.id.input_to);
        subject = findViewById(R.id.input_subject);
        lastTimestamp = findViewById(R.id.input_last_timestamp);

        Button save = findViewById(R.id.button_save);
        Button sync = findViewById(R.id.button_sync);

        loadConfig();

        requestSmsPermissions();

        ConnectivityHelper.register(this);

        save.setOnClickListener(v -> {
            saveConfig();
            Toast.makeText(this, "saveConfig", Toast.LENGTH_SHORT).show();
        });

        sync.setOnClickListener(v -> {
            Log.d("SMSMAIL", "sync.setOnClickListener");
            WorkerHelper.enqueueSmsWorker(this);
            Toast.makeText(this, "enqueueSmsWorker", Toast.LENGTH_SHORT).show();
        });

        listener = (sharedPrefs, key) -> {
            if ("lastTimestamp".equals(key)) {
                lastTimestamp.setText(prefs.getString("lastTimestamp", "0"));
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }

    private void loadConfig() {
        host.setText(prefs.getString("host", ""));
        port.setText(prefs.getString("port", "587"));
        user.setText(prefs.getString("user", ""));
        pass.setText(prefs.getString("pass", ""));
        from.setText(prefs.getString("from", ""));
        to.setText(prefs.getString("to", ""));
        subject.setText(prefs.getString("subject", "SMS"));
        lastTimestamp.setText(prefs.getString("lastTimestamp", "0"));
    }

    private void saveConfig() {
        prefs
            .edit()
            .putString("host", host.getText().toString())
            .putString("port", port.getText().toString())
            .putString("user", user.getText().toString())
            .putString("pass", pass.getText().toString())
            .putString("from", from.getText().toString())
            .putString("to", to.getText().toString())
            .putString("subject", subject.getText().toString())
            .putString("lastTimestamp", lastTimestamp.getText().toString())
            .apply();
    }

    private void requestSmsPermissions() {
        if (
            checkSelfPermission(Manifest.permission.RECEIVE_SMS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                new String[] {
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS,
                },
                123
            );
        }
    }

    /*
    public static String getLastTimestamp(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(
            "config",
            MODE_PRIVATE
        );
        return prefs.getString("lastTimestamp", "0");
    }

    public static void setLastTimestamp(Activity activity, String value) {
        SharedPreferences prefs = activity.getSharedPreferences(
            "config",
            MODE_PRIVATE
        );
        prefs.edit().putString("lastTimestamp", value).apply();
    }
    */
}
