package com.example.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.UUID;

public class ConnectionManager {

    private static final String APP_NAME = "SecureChatApp";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;
    private AcceptThread acceptThread;
    private ConnectionListener listener;

    // Генериран код за проверка
    private String generatedCode;

    public interface ConnectionListener {
        void onConnectionRequest(BluetoothSocket socket); // Кога некој ќе се закачи
        void onDataReceived(String data); // Кога ќе стигне кодот
        void onConnectionSuccess(); // Кога се е во ред
        void onConnectionFailed();
    }

    public ConnectionManager(ConnectionListener listener) {
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.listener = listener;
    }

    // 1. СТАРТУВАЈ СЕРВЕР (Чекај конекции)
    @SuppressLint("MissingPermission")
    public void startListening() {
        if (acceptThread != null) { acceptThread.cancel(); }
        acceptThread = new AcceptThread();
        acceptThread.start();
    }

    // 2. КЛИЕНТ (Поврзи се со друг)
    @SuppressLint("MissingPermission")
    public void connectTo(BluetoothSocket socket) {
        // Веќе сме поврзани (socket е отворен), сега почнуваме проверка
        listener.onConnectionRequest(socket);
    }

    // Генерирај случаен 6-цифрен код
    public String generateSecurityCode() {
        Random r = new Random();
        int code = 100000 + r.nextInt(900000);
        this.generatedCode = String.valueOf(code);
        return this.generatedCode;
    }

    public boolean verifyCode(String inputCode) {
        return generatedCode != null && generatedCode.equals(inputCode);
    }

    // --- ВНАТРЕШНА КЛАСА ЗА СЕРВЕРОТ ---
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        @SuppressLint("MissingPermission")
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
            } catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            while (true) {
                try {
                    if (mmServerSocket != null)
                        socket = mmServerSocket.accept(); // ТУКА ЧЕКАМЕ!
                } catch (IOException e) {
                    break;
                }

                if (socket != null) {
                    // Конекцијата е прифатена!
                    // Одиме на главниот thread за да го известиме UI-то
                    BluetoothSocket finalSocket = socket;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        listener.onConnectionRequest(finalSocket);
                    });
                    try {
                        mmServerSocket.close(); // Затвораме слушање по едно поврзување
                    } catch (IOException e) { }
                    break;
                }
            }
        }

        public void cancel() {
            try {
                if (mmServerSocket != null) mmServerSocket.close();
            } catch (IOException e) { }
        }
    }
}