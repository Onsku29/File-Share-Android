package com.onsku29.fileshare.networking;

import android.content.Context;
import android.util.Log;

import com.onsku29.fileshare.storing.PairedDevice;
import com.onsku29.fileshare.util.NotificationHelper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;

public class FileSender {
    private static final String TAG = "FileSender";

    public static void sendFile(Context context, PairedDevice device, File file) {
        NotificationHelper.createChannel(context);
        NotificationHelper.showProgress(context, file.getName(), 0, (int) file.length());

        try (Socket socket = new Socket(device.getIp(), device.getPort())) {
            socket.setSendBufferSize(1024 * 1024);

            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String fileName = file.getName();
            long fileSize = file.length();

            out.println("SHARE");
            String response = in.readLine();
            if (!Objects.equals(response, "READY_FOR_FILE")) {
                Log.w(TAG, "Unexpected server response: " + response);
                NotificationHelper.showFailure(context, file.getName());
                return;
            }

            out.println(fileName);
            out.println(fileSize);

            response = in.readLine();
            if (!Objects.equals(response, "READY_TO_RECEIVE_FILE")) {
                Log.w(TAG, "Unexpected server response: " + response);
                NotificationHelper.showFailure(context, file.getName());
                return;
            }

            try (
                    BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file), 65536);
                    BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream(), 65536)
            ) {
                byte[] buffer = new byte[65536]; // 64KB
                int bytesRead;
                long totalSent = 0;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalSent += bytesRead;
                    NotificationHelper.showProgress(context, file.getName(), (int) totalSent, (int) file.length());
                }

                outputStream.flush();
            }

            response = in.readLine();
            Log.i(TAG, response);
            NotificationHelper.cancel(context);
            if (Objects.equals(response, "FILE_RECEIVED")) {
                Log.i(TAG, "File sent successfully to " + device.getName());
                NotificationHelper.showSuccess(context, file.getName());
            } else {
                Log.e(TAG, "Unexpected server response after sending file: " + response);
                NotificationHelper.showFailure(context, file.getName());
            }

        } catch (Exception e) {
            NotificationHelper.cancel(context);
            if (e instanceof java.net.SocketException && Objects.requireNonNull(e.getMessage()).contains("Socket closed")) {
                Log.i(TAG, "Socket was closed after sending, assuming success.");
                NotificationHelper.showSuccess(context, file.getName());
            } else {
                Log.e(TAG, "File transfer failed: " + e.getMessage(), e);
                NotificationHelper.showFailure(context, file.getName());
            }
        }
    }
}
