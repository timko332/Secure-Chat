package com.example.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private List<BluetoothDevice> devices = new ArrayList<>();
    private final OnDeviceClickListener listener;
    private boolean showAll = false; // Дали да ги покаже сите или само 4

    public interface OnDeviceClickListener {
        void onDeviceClick(BluetoothDevice device);
    }

    public DeviceAdapter(OnDeviceClickListener listener) {
        this.listener = listener;
    }

    public void addDevice(BluetoothDevice device) {
        // Проверка да не додаваме дупликати
        for (BluetoothDevice d : devices) {
            if (d.getAddress().equals(device.getAddress())) return;
        }
        devices.add(device);
        notifyDataSetChanged();
    }

    public void clear() {
        devices.clear();
        notifyDataSetChanged();
    }

    public void setShowAll(boolean showAll) {
        this.showAll = showAll;
        notifyDataSetChanged();
    }

    public int getRealSize() {
        return devices.size();
    }

    @Override
    public int getItemCount() {
        if (showAll) {
            return devices.size();
        } else {
            // Врати максимум 4, или помалку ако има помалку
            return Math.min(devices.size(), 4);
        }
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        BluetoothDevice device = devices.get(position);

        try {
            // Проверка за permission (иако main activity го има, Android бара try/catch тука)
            String name = device.getName();
            holder.tvName.setText(name != null ? name : "Unknown Device");
            holder.tvAddress.setText(device.getAddress());
        } catch (SecurityException e) {
            holder.tvName.setText("Permission Error");
        }

        holder.itemView.setOnClickListener(v -> listener.onDeviceClick(device));
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress;
        DeviceViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvDeviceName);
            tvAddress = itemView.findViewById(R.id.tvDeviceAddress);
        }
    }
}