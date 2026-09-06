Smart Steganography File Hider Mobile App
A secure, offline Android application designed to hide sensitive files (documents, text, images) inside ordinary cover images and WAV audio files using a 4-bit Least Significant Bit (LSB) steganography technique combined with strong AES encryption and GZIP compression.
## 📌 Project Overview
**Smart Steganography File Hider Mobile App** allows users to conceal private data within media files on their mobile devices without changing the visual or auditory appearance of the cover media. Unlike standard encryption tools that make locked files obvious, this application provides **two-layer protection**:
1. **Cryptography**: Encrypts payload data with AES before hiding.
2. **Steganography**: Conceals the encrypted data within image pixels or audio sample bytes.
In addition, the application features a **Dual-PIN Authentication Mechanism** (Real Vault + Decoy Gallery) for protection against forced disclosure, operating completely offline using local device storage.
## ✨ Key Features
* **Higher-Capacity 4-Bit LSB Embedding**: Uses a 4-bit per channel LSB scheme across RGB channels, offering up to 4x higher capacity than conventional 1-bit LSB methods while keeping media changes imperceptible.
* **Dual-Layer Security**: Payload data is first compressed via **GZIP** and encrypted using **AES (256-bit key)** derived via **SHA-256** from the user-provided password before embedding.
* **Dual-PIN Duress Protection**:
  * **Original PIN**: Grants access to the real vault and hidden files.
  * **Fake (Decoy) PIN**: Opens a harmless decoy gallery with identical UI behavior to preserve plausible deniability under pressure.
* **Audio & Image Steganography**: Supports PNG/JPEG images and uncompressed WAV audio files (with automatic MP3-to-WAV transcoding support via `AudioConverter`).
* **100% Offline & Local Storage**: No Firebase, cloud servers, or remote databases are used. Credentials and history are securely saved locally using encrypted `SharedPreferences`, and hidden files are saved directly in local internal storage/device gallery.
* **Account Recovery**: Secure local account restoration via security questions and a unique one-time recovery key paired with an encrypted backup file (`.enc`).
* **History Tracking**: Keeps a simple local log of all hide and extract operations for user management.
## 🛠️ Technology Stack
* **Platform / OS**: Android 7.0+ (Min API 24, Target API 35)
* **Programming Language**: Java
* **IDE & Tools**: Android Studio, Gradle (Kotlin DSL), AndroidX
* **Cryptography & Compression**: `javax.crypto` (AES), `java.security.MessageDigest` (SHA-256), `java.util.zip` (GZIP)
* **Data Storage**: Local internal storage, Android Media/Gallery, and Encrypted `SharedPreferences` (No external DBMS / No Firebase)
## 🏗️ System Architecture & Data Flow
-----------------------------------------------------------------------+
|                          PRESENTATION LAYER                           |
|  (SetupActivity, LoginActivity, MainActivity, ImageHide, AudioHide,   |
|            ExtractActivity, HistoryActivity, FakeActivity)            |
+-----------------------------------------------------------------------+
|
v
+-----------------------------------------------------------------------+
|                             LOGIC LAYER                               |
|        CryptoStego Engine | HistoryManager | RecoveryManager          |
+-----------------------------------------------------------------------+
|                                                                       |
|                                                                       |
+-----------------------------------------------------------------------+
|                     SECURITY & STORAGE LAYER                          |
|   AES Encryption / SHA-256 | Local SharedPreferences | Device Storage |
+-----------------------------------------------------------------------+
## 🚀 How to Run & Install
### Prerequisites
* **Android Studio** (Ladybug / Jellyfish or newer recommended)
* **JDK 11+**
* Android Device or Emulator running **Android 7.0 (API 24)** or higher
### Setup Steps
1. **Download Code**: Download or clone the project source repository to your local computer.
2. **Open Project**: Open **Android Studio** and select **Open an Existing Project**.
3. **Sync Gradle**: Navigate to the project folder and let Android Studio sync the required dependencies.
4. **Run Application**: Connect a physical Android device via USB debugging (or start an emulator) and click **Run** (`Shift + F10`).
## 📄 License & Project Info
* **Project Type**: BS (IT) Final Year Project[span_0](start_span)[span_0](end_span)
* **Affiliation**: Department of Information Technology, Govt. Graduate College Burewala (Affiliated with Bahauddin Zakariya University, Multan)[span_1](start_span)[span_1](end_span)
