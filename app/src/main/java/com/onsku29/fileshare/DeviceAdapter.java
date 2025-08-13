package com.onsku29.fileshare;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.onsku29.fileshare.storing.PairedDevice;

import java.util.ArrayList;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {
    public interface OnDeviceLongClickListener {
        void onDeviceLongClick(PairedDevice device);
    }

    private final ArrayList<PairedDevice> devices;
    private final OnDeviceLongClickListener listener;

    public DeviceAdapter(ArrayList<PairedDevice> devices, OnDeviceLongClickListener listener) {
        this.devices = devices;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView deviceName;

        public ViewHolder(View view) {
            super(view);
            deviceName = view.findViewById(R.id.deviceName);
        }
    }

    @NonNull
    @Override
    public DeviceAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.device_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PairedDevice device = devices.get(position);
        holder.deviceName.setText(device.getName());
        holder.itemView.setOnLongClickListener(v -> {
            AlertDialog dialog = new AlertDialog.Builder(v.getContext())
                    .setTitle(v.getContext().getString(R.string.remove_device))
                    .setMessage(v.getContext().getString(R.string.remove_confirm) + " \"" + device.getName() + "\"?")
                    .setPositiveButton(v.getContext().getString(R.string.remove), (dialogint, which) -> {
                        listener.onDeviceLongClick(device);
                    })
                    .setNegativeButton(v.getContext().getString(R.string.cancel), null)
                    .create();

            dialog.setOnShowListener(dlg -> {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#0099E0"));
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.GRAY);
            });

            dialog.show();

            return true;
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }
}
