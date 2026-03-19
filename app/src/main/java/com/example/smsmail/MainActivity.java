package com.example.smsmail;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

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

        requestRuntimePermissions();
        WorkerHelper.ensurePeriodicSync(this);

        ConnectivityHelper.register(getApplicationContext());

        save.setOnClickListener(v -> {
            saveConfig();
            Toast.makeText(this, "saveConfig", Toast.LENGTH_SHORT).show();
        });

        sync.setOnClickListener(v -> {
            Log.d("SMSMAIL", "sync.setOnClickListener");
            WorkerHelper.enqueueImmediateSync(this);
            Toast.makeText(this, "enqueueImmediateSync", Toast.LENGTH_SHORT).show();
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
        loadConfig();
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }

    private void loadConfig() {
        host.setText(prefs.getString("host", "smtppro.zoho.com"));
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

    private void requestRuntimePermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        String[] permissions = new String[] {
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE,
        };
        List<String> missingPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        if (!missingPermissions.isEmpty()) {
            requestPermissions(missingPermissions.toArray(new String[0]), 123);
        }
    }
}
