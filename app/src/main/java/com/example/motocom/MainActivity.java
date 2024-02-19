package com.example.motocom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.motocom.service.ClientAudioService;
import com.example.motocom.service.ServerAudioService;

public class MainActivity extends AppCompatActivity {

    private EditText ipAddressEditText, portNumEditText, portServerNumEditText;
    private Button connectBtn, disconnectBtn, startBtn, stopBtn, clientModeBtn, serverModeBtn, exitBtn, helpBtn, aboutBtn;
    private TextView serverStatus, clientStatus;
    private LinearLayout clientLayout, serverLayout;

    private static final int ALL_PERMISSIONS_REQUEST_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init Permissions
        requestPermissions();

        // Init Layouts
        initFormElements();

        exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopApp();
            }
        });

        helpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, HelpActivity.class);
                startActivity(intent);
            }
        });

        aboutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(intent);
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
        ipAddressEditText = findViewById(R.id.ipAddress);
        portNumEditText = findViewById(R.id.portNum);
        portServerNumEditText = findViewById(R.id.portServerNum);
        connectBtn = findViewById(R.id.connect);
        disconnectBtn = findViewById(R.id.disconnect);
        startBtn = findViewById(R.id.start);
        stopBtn = findViewById(R.id.stop);
        exitBtn = findViewById(R.id.exit);
        helpBtn = findViewById(R.id.help);
        aboutBtn = findViewById(R.id.about);
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


    private void clientConnect() {

        String ipAddress = this.ipAddressEditText.getText().toString();
        String portStr = portNumEditText.getText().toString();
        if (ipAddress.isEmpty() || portStr.isEmpty()) {
            Toast.makeText(this, R.string.info_insert_ip_and_port, Toast.LENGTH_SHORT).show();
            return;
        }

        int port = 25000;
        try {
            port = Integer.parseInt(portStr);
            if (port > 65535 || port < 1) {
                port = 25000;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.error_invalid_port_number_format_defaulted_to_25000, Toast.LENGTH_SHORT).show();
        }

        // Initialize microphone
        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    serverStatus.setText(R.string.error_problem_with_permissions);
                }
            });
            return;
        } else {

            clientStatus.setText(R.string.info_connected);
            Intent serviceIntent = new Intent(this, ClientAudioService.class);
            serviceIntent.putExtra("IP_ADDRESS", ipAddress);
            serviceIntent.putExtra("PORT", port);
            startService(serviceIntent);
        }
    }

    private void serverStart() {
        String portStr = portServerNumEditText.getText().toString();
        String ipAddress = getIP();

        portServerNumEditText.setVisibility(View.INVISIBLE);

        int port = 25000;
        try {
            port = Integer.parseInt(portStr);
            if (port > 65535 || port < 1) {
                port = 25000;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.error_invalid_port_number_format_defaulted_to_25000, Toast.LENGTH_SHORT).show();
        }

        // Initialize microphone
        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    serverStatus.setText(R.string.error_problem_with_permissions);
                }
            });
            return;
        } else {
            serverStatus.setText(String.format(getString(R.string.info_server_started), ipAddress, port));
            Intent serviceIntent = new Intent(this, ServerAudioService.class);
            serviceIntent.putExtra("PORT", port);
            startService(serviceIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ALL_PERMISSIONS_REQUEST_CODE) {
            // Check if all permissions are granted
            boolean allGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.INTERNET
        };
        ActivityCompat.requestPermissions(this, permissions, ALL_PERMISSIONS_REQUEST_CODE);
    }


    private String getIP() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Check if wifi is enabled
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            // Get WifiInfo
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();

            // Convert IP address to string
            @SuppressLint("DefaultLocale") String ipAddressString = String.format("%d.%d.%d.%d",
                    (ipAddress & 0xff),
                    (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff),
                    (ipAddress >> 24 & 0xff));

            // Set the IP address to TextView
            return ipAddressString;
        }
        return "WiFi Disabled";
    }
}
