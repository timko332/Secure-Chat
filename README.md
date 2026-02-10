# 🔒 SecureChat - Bluetooth Messenger

**SecureChat** is a native Android application designed for secure, **offline communication** between devices using Bluetooth technology. It allows users to chat without an internet connection (Wi-Fi or Mobile Data), ensuring privacy and connectivity in remote areas.

---

## ✨ Key Features

* **📡 Offline & Private:** No internet required. Messages are sent directly device-to-device via Bluetooth RFCOMM sockets.
* **🛡️ Secure Handshake Protocol:** Includes a manual verification step. One user generates a **6-digit security code**, and the other must enter it correctly to establish a connection.
* **🔍 Smart Device Filtering:** Automatically filters scanned devices to show **only smartphones**, ignoring headphones, watches, and other peripherals.
* **🎨 Modern UI:** Built with a "Glassmorphism" aesthetic, Dark Mode support, and **Edge-to-Edge** design (transparent system bars).
* **⌨️ Responsive Layout:** Features a smart keyboard handling system that ensures the input field remains visible when typing.

---

## 🛠️ Tech Stack

* **Language:** Java
* **Minimum SDK:** Android 7.0 (API 24)
* **Target SDK:** Android 14 (API 34)
* **Architecture:** MVC (Model-View-Controller)
* **Core Components:**
    * `BluetoothAdapter` & `BluetoothServerSocket`
    * `BroadcastReceiver` (for discovery)
    * `RecyclerView` (custom adapters for devices and chat bubbles)
    * `ConstraintLayout` & `ViewCompat` (Edge-to-Edge insets)

---

## ⚠️ Installation Troubleshooting

If you are installing the app via USB (Debug) or sending the APK manually to a friend, you might encounter security restrictions on newer Android devices.

### 📱 Xiaomi / Redmi / Poco Devices
**Error:** `INSTALL_FAILED_USER_RESTRICTED`
**Fix:**
1.  Insert a SIM card and turn off Wi-Fi (use Mobile Data temporarily).
2.  Go to **Settings > Additional Settings > Developer Options**.
3.  Enable **"Install via USB"**.
4.  Enable **"USB Debugging (Security Settings)"**.

### 📱 Samsung Devices (One UI 6.0+)
**Error:** "App not installed" or "Package appears to be invalid"
**Fix:**
1.  Go to **Settings > Security and Privacy**.
2.  Find **Auto Blocker**.
3.  Set it to **OFF**.
4.  If sending via Telegram/Viber, save the file to "My Files" first, then install.

---

## 🚀 How to Use

1.  **Permissions:** Upon first launch, allow Bluetooth (Nearbys devices) and Location permissions.
2.  **Scan:** Tap "SCAN FOR DEVICES".
3.  **Connect:** Select the target phone from the list.
4.  **Verify:**
    * **Device A (Server):** Will show a code (e.g., `592 013`).
    * **Device B (Client):** Enter the code shown on Device A.
5.  **Chat:** Once verified, the secure chat room opens.

---

### License
This project is for educational purposes.
