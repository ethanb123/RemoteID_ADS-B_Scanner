/*
 * Remote ID Scanner Android App
 * This is a skeleton Android project that scans for Remote ID broadcasts via Bluetooth and Wi-Fi.
 */

// MainActivity.java

package com.example.remoteidscanner;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
//import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private BluetoothLeScanner bluetoothLeScanner;
    private WifiManager wifiManager;
    private ListView listView;
    private ArrayAdapter<String> listAdapter;
    private List<String> detectedDevices;
    private Handler wifiScanHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize components
        listView = findViewById(R.id.device_list);
        detectedDevices = new ArrayList<>();
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, detectedDevices);
        listView.setAdapter(listAdapter);

        // Request permissions
        if (!checkPermissions()) {
            requestPermissions();
        } else {
            initializeScanning();
        }
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeScanning();
            } else {
                Toast.makeText(this, "Permissions required for scanning", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initializeScanning() {
        // Initialize Bluetooth scanner
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            startBluetoothScan();
        }

        // Initialize Wi-Fi manager
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        startContinuousWifiScan();
    }

    private void startBluetoothScan() {
        ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                String deviceInfo = "Bluetooth: " + result.getDevice().getName() + " (" + result.getDevice().getAddress() + ")";
                if (!detectedDevices.contains(deviceInfo)) {
                    detectedDevices.add(deviceInfo);
                    listAdapter.notifyDataSetChanged();
                }
            }
        };
        bluetoothLeScanner.startScan(scanCallback);
    }

    private void startContinuousWifiScan() {
        wifiScanHandler = new Handler(Looper.getMainLooper());
        Runnable wifiScanRunnable = new Runnable() {
            @Override
            public void run() {
                if (wifiManager != null) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    List<android.net.wifi.ScanResult> wifiResults = wifiManager.getScanResults();
                    for (android.net.wifi.ScanResult result : wifiResults) {
                        String deviceInfo = "Wi-Fi: " + result.SSID + " (" + result.BSSID + ")";
                        if (!detectedDevices.contains(deviceInfo)) {
                            detectedDevices.add(deviceInfo);
                            listAdapter.notifyDataSetChanged();
                        }
                    }
                }
                wifiScanHandler.postDelayed(this, 10000); // Re-scan every 10 seconds
            }
        };
        wifiScanHandler.post(wifiScanRunnable);
    }
}
