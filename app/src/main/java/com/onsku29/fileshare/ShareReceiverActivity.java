package com.onsku29.fileshare;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.onsku29.fileshare.networking.FileSender;
import com.onsku29.fileshare.storing.DeviceManager;
import com.onsku29.fileshare.storing.PairedDevice;
import com.onsku29.fileshare.util.FileUtils;
import com.onsku29.fileshare.util.NotificationHelper;

import java.io.File;
import java.util.List;

public class ShareReceiverActivity extends AppCompatActivity {
    private static final String TAG = "ShareReceiver";
    private DeviceManager deviceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        deviceManager = new DeviceManager(getApplicationContext());
        NotificationHelper.createChannel(this);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            Uri fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (fileUri != null) {
                showDevicePickerDialog(fileUri);
            } else {
                finishWithError("No file received.");
            }
        } else {
            finishWithError("Invalid sharing intent.");
        }
    }

    private void showDevicePickerDialog(Uri fileUri) {
        List<PairedDevice> devices = deviceManager.getPairedDevices();
        if (devices.isEmpty()) {
            finishWithError("No paired devices available.");
            return;
        }

        String[] deviceNames = devices.stream().map(PairedDevice::getName).toArray(String[]::new);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_device))
                .setItems(deviceNames, (dialog, which) -> {
                    PairedDevice selectedDevice = devices.get(which);
                    new Thread(() -> {
                        File tempFile = FileUtils.copyUriToCacheFile(this, fileUri);
                        if (tempFile == null) {
                            runOnUiThread(() -> finishWithError("Failed to prepare file for sending."));
                            return;
                        }

                        try {
                            FileSender.sendFile(this, selectedDevice, tempFile);
                        } finally {
                            if (tempFile.exists() && !tempFile.delete()) {
                                Log.w(TAG, "Failed to delete temp file: " + tempFile.getAbsolutePath());
                            }
                        }
                    }).start();
                    finish();
                })
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    private void finishWithError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, message);
        finish();
    }
}
