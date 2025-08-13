package com.onsku29.fileshare.storing;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class DeviceManager {
    private ArrayList<PairedDevice> pairedDevices;
    private final Gson gson = new Gson();
    private static final String FILE_NAME = "paired_devices.json";
    private final Context context;

    public DeviceManager(Context context){
        this.context = context;
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (file.isFile()) {
            pairedDevices = loadDevices();
        } else {
            pairedDevices = new ArrayList<>();
        }
    }

    public void addDevice(PairedDevice device){
        pairedDevices.add(device);
        saveDevices();
    }

    public void removeDevice(PairedDevice device){
        pairedDevices.remove(device);
        saveDevices();
    }

    private void saveDevices(){
        String json = gson.toJson(pairedDevices);
        try (OutputStreamWriter outputStreamWriter =
                     new OutputStreamWriter(context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE))) {
            outputStreamWriter.write(json);
            Log.i("DeviceManager", "Saved devices " + context.getFilesDir() + "/" + FILE_NAME);
        } catch (IOException e) {
            Log.e("DeviceManager", "Saving devices failed: " + e);
        }
        pairedDevices = loadDevices();
    }

    private ArrayList<PairedDevice> loadDevices(){
        try (InputStream inputStream = context.openFileInput(FILE_NAME);
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }

            Type listType = new TypeToken<ArrayList<PairedDevice>>() {}.getType();
            return gson.fromJson(stringBuilder.toString(), listType);

        } catch (IOException e) {
            Log.e("DeviceManager", "Loading devices failed: " + e);
        }
        return new ArrayList<>();
    }

    public ArrayList<PairedDevice> getPairedDevices(){
        return pairedDevices;
    }

    public boolean isDevicePaired(PairedDevice device){
        boolean paired = pairedDevices.contains(device);
        Log.i("DeviceManager", "isDevicePaired = " + paired);
        return paired;
    }
}