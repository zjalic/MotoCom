package com.example.motocom;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private EditText ipAddress, portNum, portServerNum;
    private Button connectBtn, disconnectBtn, startBtn, stopBtn, clientModeBtn, serverModeBtn, exitBtn;
    private TextView serverStatus, clientStatus;
    private LinearLayout clientLayout, serverLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initFormElements();

        exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopApp();
            }
        });

        serverModeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serverLayout.setVisibility(View.VISIBLE);
                clientLayout.setVisibility(View.INVISIBLE);
            }
        });

        clientModeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serverLayout.setVisibility(View.INVISIBLE);
                clientLayout.setVisibility(View.VISIBLE);
            }
        });

        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clientConnect();
            }
        });

        disconnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopApp();
            }
        });

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serverStart();
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopApp();
            }
        });
    }

    private void stopApp() {
        finish();
        System.exit(0);
    }

    private void initFormElements() {
        // Fields
        ipAddress = findViewById(R.id.ipAddress);
        portNum = findViewById(R.id.portNum);
        portServerNum = findViewById(R.id.portServerNum);
        connectBtn = findViewById(R.id.connect);
        disconnectBtn = findViewById(R.id.disconnect);
        startBtn = findViewById(R.id.start);
        stopBtn = findViewById(R.id.stop);
        exitBtn = findViewById(R.id.exit);
        clientModeBtn = findViewById(R.id.clientMode);
        serverModeBtn = findViewById(R.id.serverMode);
        serverStatus = findViewById(R.id.serverStatus);
        clientStatus = findViewById(R.id.clientStatus);
        clientLayout = findViewById(R.id.topLayout);
        serverLayout = findViewById(R.id.bottomLayout);

        // Visibility
        clientLayout.setVisibility(View.INVISIBLE);
        serverLayout.setVisibility(View.INVISIBLE);
    }

    private String getIP(){
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Check if wifi is enabled
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            // Get WifiInfo
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();

            // Convert IP address to string
            String ipAddressString = String.format("%d.%d.%d.%d",
                    (ipAddress & 0xff),
                    (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff),
                    (ipAddress >> 24 & 0xff));

            // Set the IP address to TextView
            return ipAddressString;
        }
        return "WiFi Disabled";
    }

    private void clientConnect() {

        String ipAddress = this.ipAddress.getText().toString();
        String portStr = portNum.getText().toString();
        if (ipAddress.isEmpty() || portStr.isEmpty()) {
            Toast.makeText(this, "Please enter IP Address and Port number", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "IP: " + ipAddress + " Port: " + portStr, Toast.LENGTH_SHORT).show();

        Thread clientThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int port = 25000;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    clientStatus.setText("Invalid Port Number Format. Setting to 25000");
                }

                try (Socket socket = new Socket(ipAddress, port)) {
                    clientStatus.setText("Connected");
                    // Audio setup
                    int sampleRate = 44100;
                    int channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
                    int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
                    int bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_STEREO, audioFormat);

                    // Initialize microphone
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                clientStatus.setText("Problem with permissions ...");
                            }
                        });
                        return;
                    }
                    AudioRecord microphone = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, sampleRate, AudioFormat.CHANNEL_IN_STEREO, audioFormat, bufferSize);
                    microphone.startRecording();

                    // Initialize speakers
                    AudioTrack speakers = new AudioTrack(AudioManager.STREAM_VOICE_CALL, sampleRate, channelConfig, audioFormat, bufferSize, AudioTrack.MODE_STREAM);
                    speakers.play();

                    byte[] buffer = new byte[bufferSize];

                    // Audio streaming
                    while (true) {
                        int bytesRead = microphone.read(buffer, 0, bufferSize);
                        socket.getOutputStream().write(buffer, 0, bytesRead);

                        int bytesReceived = socket.getInputStream().read(buffer, 0, bufferSize);
                        speakers.write(buffer, 0, bytesReceived);
                    }


                } catch (IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            clientStatus.setText("Problem connecting to server ...");
                        }
                    });
                    return;
                }

            }
        });
        clientThread.start();
    }

    private void serverStart() {
        String portStr = portServerNum.getText().toString();
        if (portStr.isEmpty()) {
            Toast.makeText(this, "Port number not specified", Toast.LENGTH_SHORT).show();
        }

        String ipAddress = getIP();
        Toast.makeText(this, "IP: " + ipAddress + " Port: " + portStr, Toast.LENGTH_SHORT).show();

        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int port = 25000;
                try {
                    port = Integer.parseInt(portStr);
                } catch (Exception ignored) {

                }
                serverStatus.setText("Server Started. IP: " + ipAddress + " Port:" + port);

                try (ServerSocket serverSocket = new ServerSocket(port)) {
                    Socket clientSocket = serverSocket.accept();

                    // Audio setup
                    int sampleRate = 44100;
                    int channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
                    int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
                    int bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_STEREO, audioFormat);

                    // Initialize microphone
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Problem with permissions ...", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }
                    AudioRecord microphone = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, sampleRate, AudioFormat.CHANNEL_IN_STEREO, audioFormat, bufferSize);
                    microphone.startRecording();

                    // Initialize speakers
                    AudioTrack speakers = new AudioTrack(AudioManager.STREAM_VOICE_CALL, sampleRate, channelConfig, audioFormat, bufferSize, AudioTrack.MODE_STREAM);
                    speakers.play();

                    byte[] buffer = new byte[bufferSize];

                    // Audio streaming
                    while (true) {
                        int bytesRead = microphone.read(buffer, 0, bufferSize);
                        clientSocket.getOutputStream().write(buffer, 0, bytesRead);

                        int bytesReceived = clientSocket.getInputStream().read(buffer, 0, bufferSize);
                        speakers.write(buffer, 0, bytesReceived);
                    }
                } catch (IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            serverStatus.setText("Server Stopped");
                        }
                    });
                    return;
                }
            }
        });
        serverThread.start();
    }

}
