package com.onsku29.fileshare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.Settings;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.onsku29.fileshare.storing.DeviceManager;
import com.onsku29.fileshare.storing.PairedDevice;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FileShare";
    private final Gson gson = new Gson();
    private DeviceManager deviceManager;
    private DeviceAdapter adapter;

    private static class PairingRequest {
        String token;
        String deviceId;
        String deviceName;

        PairingRequest(String token, String deviceId, String deviceName) {
            this.token = token;
            this.deviceId = deviceId;
            this.deviceName = deviceName;
        }
    }

    private static class PairingResponse {
        String status;
        String serverName;
        String reason;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        final boolean isFirstRun = prefs.getBoolean("isFirstRun", true);

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
            }
        }

        deviceManager = new DeviceManager(getApplicationContext());

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout), (v, insets) -> {
            WindowInsetsCompat insets2 = WindowInsetsCompat.toWindowInsetsCompat(getWindow().getDecorView().getRootWindowInsets());
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    v.getPaddingLeft(),
                    Objects.requireNonNull(insets2.getDisplayCutout()).getSafeInsetTop(),
                    v.getPaddingRight(),
                    systemBars.bottom
            );
            return insets;
        });
        Button scanButton = findViewById(R.id.addDeviceButton);
        scanButton.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt(getString(R.string.scan_qr_code));
            integrator.setBeepEnabled(true);
            integrator.setOrientationLocked(true);
            integrator.setCaptureActivity(CaptureActivityPortrait.class);
            integrator.initiateScan();
        });

        RecyclerView recyclerView = findViewById(R.id.deviceRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ImageView helpIcon = findViewById(R.id.helpIcon);
        helpIcon.setOnClickListener(v -> startTutorial());

        adapter = new DeviceAdapter(
                deviceManager.getPairedDevices(),
                device -> {
                    runOnUiThread(() -> {
                        deviceManager.removeDevice(device);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, getString(R.string.removed) + " " + device.getName(), Toast.LENGTH_SHORT).show();
                    });
                }
        );
        recyclerView.setAdapter(adapter);

        if(isFirstRun){
            startTutorial();
            prefs.edit().putBoolean("isFirstRun", false).apply();
        }
    }

    private void startTutorial() {
        String[] tutorialTexts = {
                getString(R.string.tutorial_step1),
                getString(R.string.tutorial_step2),
                getString(R.string.tutorial_step3),
                getString(R.string.tutorial_step4),
                getString(R.string.tutorial_step5)
        };

        final int totalSteps = tutorialTexts.length;
        final int[] currentStep = {0};

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_tutorial, null);

        TextView tutorialText = dialogView.findViewById(R.id.tutorialText);
        Button nextButton = dialogView.findViewById(R.id.tutorialNextButton);

        tutorialText.setText(tutorialTexts[currentStep[0]]);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        nextButton.setOnClickListener(v -> {
            currentStep[0]++;

            if(currentStep[0] < totalSteps){
                tutorialText.setText(tutorialTexts[currentStep[0]]);

                if(currentStep[0] == totalSteps - 1){
                    nextButton.setText(getString(R.string.close));
                }
            }
            else{
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null && result.getContents() != null) {
            try {
                JSONObject json = new JSONObject(result.getContents());
                String ip = json.getString("Ip");
                int port = json.getInt("Port");
                String token = json.getString("Token");

                new Thread(() -> performPairing(ip, port, token)).start();
            }
            catch (Exception e) {
                Log.e(TAG, "Invalid QR code: " + e.getMessage());
            }
        } 
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void performPairing(String ip, int port, String token) {
        new Thread(() -> {
            String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            String deviceName = Settings.Global.getString(getContentResolver(), "device_name");
            if (deviceName == null) {
                deviceName = android.os.Build.MODEL;
            }
            if (deviceName == null) {
                deviceName = "Android Device";
            }

            try (Socket socket = new Socket(ip, port);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println("PAIR");

                // Build request JSON
                PairingRequest request = new PairingRequest(token, androidId, deviceName);
                String jsonRequest = gson.toJson(request);

                Log.i(TAG, "Sending pairing request: " + jsonRequest);
                out.println(jsonRequest);

                String jsonResponse = in.readLine();
                if (jsonResponse == null) {
                    throw new Exception("No response from server");
                }
                Log.i(TAG, "Received pairing response: " + jsonResponse);

                PairingResponse response;
                try {
                    response = gson.fromJson(jsonResponse, PairingResponse.class);
                } catch (JsonSyntaxException e) {
                    throw new Exception("Malformed response from server");
                }

                if ("success".equalsIgnoreCase(response.status)) {
                    Log.i(TAG, "Pairing successful! Server name: " + response.serverName);
                    PairedDevice newDevice = new PairedDevice(ip, port, response.serverName, token);
                    deviceManager.addDevice(newDevice);
                    runOnUiThread(() -> {
                        adapter.notifyItemInserted(deviceManager.getPairedDevices().size() - 1);
                        Toast.makeText(this, getString(R.string.pairing_successful, response.serverName), Toast.LENGTH_SHORT).show();
                    });
                } else {
                    String reason = response.reason != null ? response.reason : "Unknown error";
                    Log.e(TAG, "Pairing failed: " + reason);
                    runOnUiThread(() -> {
                        Toast.makeText(this, getString(R.string.pairing_failed)+ ": " + reason, Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Pairing failed: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.pairing_failed)+ ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}
