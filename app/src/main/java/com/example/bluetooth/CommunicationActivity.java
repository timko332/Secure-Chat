package com.example.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CommunicationActivity extends AppCompatActivity {

    private ChatAdapter adapter;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private AppCompatButton btnSend;
    private TextView tvStatusHeader;

    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // --- ПОПРАВКА ЗА ТАСТАТУРАТА И EDGE-TO-EDGE ---
        // Овој дел ја „турка“ апликацијата нагоре кога ќе се отвори тастатурата
        // Важно: Во activity_chat.xml најгорниот лејаут мора да има id: chat_root
        View mainView = findViewById(R.id.chat_root);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime()); // Тастатура

                // Пресметуваме колку да се крене (или колку е навигацијата или тастатурата)
                int bottomPadding = Math.max(systemBars.bottom, ime.bottom);

                v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding);
                return insets;
            });
        }

        // 1. Иницијализација на UI
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        tvStatusHeader = findViewById(R.id.tvChatName);

        // 2. Setup на Листата
        adapter = new ChatAdapter();
        // StackFromEnd значи дека новите пораки се појавуваат долу, како на Viber/Messenger
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);

        // 3. ПРЕЗЕМИ ЈА BLUETOOTH ВРСКАТА
        socket = SocketHandler.getSocket();

        if (socket != null && socket.isConnected()) {
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                isConnected = true;
                tvStatusHeader.setText("Connected Securely");

                // Започни да слушаш за пораки
                listenForMessages();
            } catch (IOException e) {
                showErrorAndExit("Stream Error");
            }
        } else {
            showErrorAndExit("Connection Lost");
        }

        // 4. Логика за праќање порака
        btnSend.setOnClickListener(v -> {
            String msg = etMessage.getText().toString().trim();
            if (!msg.isEmpty() && isConnected) {
                sendMessage(msg);
            }
        });
    }

    private void sendMessage(String message) {
        new Thread(() -> {
            try {
                if (outputStream != null) {
                    outputStream.write(message.getBytes());
                    runOnUiThread(() -> {
                        // true = Пораката е од МЕНЕ (Сина)
                        adapter.addMessage(new Message(message, true));
                        etMessage.setText("");
                        // Скролај најдолу за да се гледа новата порака
                        if (adapter.getItemCount() > 0) {
                            rvMessages.smoothScrollToPosition(adapter.getItemCount() - 1);
                        }
                    });
                }
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(CommunicationActivity.this, "Failed to send", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void listenForMessages() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;

            while (isConnected) {
                try {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        String receivedMsg = new String(buffer, 0, bytes);
                        runOnUiThread(() -> {
                            // false = Пораката е од ДРУГИОТ (Сива)
                            adapter.addMessage(new Message(receivedMsg, false));
                            if (adapter.getItemCount() > 0) {
                                rvMessages.smoothScrollToPosition(adapter.getItemCount() - 1);
                            }
                        });
                    }
                } catch (IOException e) {
                    isConnected = false;
                    runOnUiThread(() -> showErrorAndExit("Connection closed"));
                    break;
                }
            }
        }).start();
    }

    private void showErrorAndExit(String error) {
        // Проверка за да не крашне ако активноста е веќе затворена
        if (!isFinishing()) {
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {}
    }
}