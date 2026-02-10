package com.example.bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // --- КОНСТАНТИ ---
    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final String CHANNEL_ID = "secure_chat_channel";
    // Стандарден UUID за сериска комуникација (SPP)
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // --- UI ЕЛЕМЕНТИ ---
    private TextView tvMyDeviceName, tvStatus, tvSeeMore;
    private AppCompatButton btnConnect;
    private RecyclerView rvDevices;
    private DeviceAdapter deviceAdapter;

    // --- BLUETOOTH ПРОМЕНЛИВИ ---
    private BluetoothAdapter bluetoothAdapter;
    private ConnectionManager connectionManager;
    private BluetoothSocket activeSocket; // Сокетот што го користиме за проверка
    private boolean isServer = false; // Дали ние сме Серверот или Клиентот

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // Full Screen
        setContentView(R.layout.activity_main);

        // 1. ИНИЦИЈАЛИЗАЦИЈА НА UI
        initViews();

        // 2. BLUETOOTH SETUP
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        createNotificationChannel();

        // 3. SETUP НА ЛИСТАТА (RECYCLER VIEW)
        setupRecyclerView();

        // 4. ПРОВЕРКА НА ДОЗВОЛИ
        if (checkAndRequestPermissions()) {
            updateMyDeviceName();
            startServerMode(); // Почни да чекаш конекции веднаш
        }

        // 5. ЛОГИКА НА КОПЧИЊАТА
        btnConnect.setOnClickListener(v -> startScanning());

        tvSeeMore.setOnClickListener(v -> {
            deviceAdapter.setShowAll(true);
            tvSeeMore.setVisibility(View.GONE);
        });
    }

    private void initViews() {
        tvMyDeviceName = findViewById(R.id.tvMyDeviceName);
        tvStatus = findViewById(R.id.tvStatus);
        tvSeeMore = findViewById(R.id.tvSeeMore);
        btnConnect = findViewById(R.id.btnConnect);
        rvDevices = findViewById(R.id.rvDevices);
    }

    private void setupRecyclerView() {
        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        deviceAdapter = new DeviceAdapter(device -> {
            // ШТО СЕ СЛУЧУВА КОГА КЛИКАШ НА УРЕД:
            connectToDevice(device);
        });
        rvDevices.setAdapter(deviceAdapter);
    }

    // --- 1. СКЕНИРАЊЕ НА УРЕДИ ---

    @SuppressLint("MissingPermission")
    private void startScanning() {
        if (!hasPermissions()) return;

        btnConnect.setText("REFRESH DEVICES");
        tvStatus.setText("Scanning for nearby phones...");
        deviceAdapter.clear();
        deviceAdapter.setShowAll(false);
        tvSeeMore.setVisibility(View.GONE);

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(scanReceiver, filter);
    }

    private final BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && device.getBluetoothClass() != null) {
                    // ФИЛТЕР: САМО ТЕЛЕФОНИ
                    if (device.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.Major.PHONE) {
                        deviceAdapter.addDevice(device);
                        if (deviceAdapter.getRealSize() > 4) {
                            tvSeeMore.setVisibility(View.VISIBLE);
                            tvSeeMore.setText("See " + (deviceAdapter.getRealSize() - 4) + " more devices...");
                        }
                    }
                }
            }
        }
    };

    // --- 2. КОНЕКЦИЈА И СЕРВЕР ---

    private void startServerMode() {
        // Оваа класа ја креиравме претходно (ConnectionManager)
        connectionManager = new ConnectionManager(new ConnectionManager.ConnectionListener() {
            @Override
            public void onConnectionRequest(BluetoothSocket socket) {
                // НЕКОЈ НИ СЕ ЗАКАЧИ!
                activeSocket = socket;

                // Провери дали апликацијата е активна
                if (getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                    runOnUiThread(() -> showSecurityDialog(true)); // true = Ние сме Сервер
                } else {
                    sendNotification("Connection Request", "Someone wants to secure chat with you.");
                }
            }

            @Override
            public void onDataReceived(String data) { /* Не се користи тука, директно во дијалогот */ }
            @Override
            public void onConnectionSuccess() { /* Handled manually */ }
            @Override
            public void onConnectionFailed() {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Connection Failed", Toast.LENGTH_SHORT).show());
            }
        });
        connectionManager.startListening();
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        tvStatus.setText("Connecting to " + device.getName() + "...");

        // Обид за конекција во посебен Thread за да не кочи UI
        new Thread(() -> {
            try {
                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                socket.connect();

                // УСПЕХ!
                activeSocket = socket;
                runOnUiThread(() -> showSecurityDialog(false)); // false = Ние сме Клиент

            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Failed to connect", Toast.LENGTH_SHORT).show();
                    tvStatus.setText("Connection Failed");
                });
            }
        }).start();
    }

    // --- 3. БЕЗБЕДНОСТ И ДИЈАЛОГ (HANDSHAKE) ---

    private void showSecurityDialog(boolean amIServer) {
        isServer = amIServer;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Load the XML layout we created
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_auth, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Врзување на елементите од dialog_auth.xml
        TextView tvTitle = view.findViewById(R.id.tvAuthTitle);
        TextView tvCode = view.findViewById(R.id.tvSecurityCode);
        TextView tvDesc = view.findViewById(R.id.tvAuthDesc);
        EditText etCode = view.findViewById(R.id.etAuthCode);
        AppCompatButton btnAction = view.findViewById(R.id.btnAuthAction);

        if (isServer) {
            // --- СЕРВЕР ЛОГИКА ---
            String code = connectionManager.generateSecurityCode(); // Генерира random код
            tvCode.setText(code.substring(0, 3) + " " + code.substring(3)); // Формат: 123 456
            tvCode.setVisibility(View.VISIBLE);
            tvDesc.setText("Ask the other user to enter this code:");
            btnAction.setVisibility(View.GONE); // Серверот само чека

            // Серверот слуша дали кодот е точен
            listenForVerificationCode(dialog, code);

        } else {
            // --- КЛИЕНТ ЛОГИКА ---
            etCode.setVisibility(View.VISIBLE);
            tvDesc.setText("Enter the code displayed on the other device:");
            btnAction.setText("VERIFY & CONNECT");

            btnAction.setOnClickListener(v -> {
                String input = etCode.getText().toString().trim();
                if (input.length() == 6) {
                    sendCodeForVerification(input, dialog);
                } else {
                    etCode.setError("Enter 6 digits");
                }
            });
        }

        dialog.setCancelable(false); // Не може да се исклучи случајно
        dialog.show();
    }

    // Серверот чека Клиентот да го прати кодот
    private void listenForVerificationCode(AlertDialog dialog, String correctCode) {
        new Thread(() -> {
            try {
                InputStream inputStream = activeSocket.getInputStream();
                byte[] buffer = new byte[1024];
                int bytes = inputStream.read(buffer); // ЧЕКАЈ ПОДАТОЦИ...

                String receivedCode = new String(buffer, 0, bytes);

                runOnUiThread(() -> {
                    if (receivedCode.equals(correctCode)) {
                        sendSignal("ACK"); // Кажи му на клиентот дека е точен
                        dialog.dismiss();
                        openChatActivity();
                    } else {
                        sendSignal("NACK"); // Грешен код
                        Toast.makeText(this, "Wrong Code! Connection Rejected.", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                        try { activeSocket.close(); } catch (IOException e) {}
                    }
                });
            } catch (IOException e) {
                // Socket closed or error
            }
        }).start();
    }

    // Клиентот го праќа кодот и чека потврда
    private void sendCodeForVerification(String code, AlertDialog dialog) {
        new Thread(() -> {
            try {
                OutputStream outputStream = activeSocket.getOutputStream();
                outputStream.write(code.getBytes()); // ПРАТИ ГО КОДОТ

                // Сега чекај одговор (ACK или NACK)
                InputStream inputStream = activeSocket.getInputStream();
                byte[] buffer = new byte[1024];
                int bytes = inputStream.read(buffer);
                String response = new String(buffer, 0, bytes);

                runOnUiThread(() -> {
                    if (response.equals("ACK")) {
                        dialog.dismiss();
                        openChatActivity();
                    } else {
                        Toast.makeText(this, "Incorrect Code!", Toast.LENGTH_SHORT).show();
                        // Не затворај веднаш, дај му шанса пак да проба (опционално)
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Communication Error", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void sendSignal(String msg) {
        try {
            activeSocket.getOutputStream().write(msg.getBytes());
        } catch (IOException e) {}
    }

    private void openChatActivity() {
        // Кога сè е успешно, одиме на ChatActivity
        // Треба да го подадеме 'activeSocket' некако.
        // Бидејќи Sockets не се Serializable, обично се користи Singleton Manager.
        // Засега, ќе ставиме Toast.

        // TODO: Во следен чекор ќе го направиме ChatActivity да го користи овој socket.
        // За да работи, ќе го ставиме socket-от во статичка променлива во ConnectionManager.
        SocketHandler.setSocket(activeSocket);

        Intent intent = new Intent(MainActivity.this, CommunicationActivity.class);
        startActivity(intent);
        finish(); // Затвори ја MainActivity
    }

    // --- 4. ПОМОШНИ МЕТОДИ ---

    @SuppressLint("MissingPermission")
    private void updateMyDeviceName() {
        String name = bluetoothAdapter.getName();
        tvMyDeviceName.setText("ID: " + (name != null ? name : "Unknown"));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Connection Requests", NotificationManager.IMPORTANCE_HIGH);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void sendNotification(String title, String message) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        getSystemService(NotificationManager.class).notify(1, builder.build());
    }

    // --- 5. PERMISSIONS ---

    private boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private boolean checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_ADVERTISE
                }, PERMISSION_REQUEST_CODE);
                return false;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            updateMyDeviceName();
            startServerMode();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(scanReceiver);
        } catch (IllegalArgumentException e) {}
    }
}