/*
 * Remote ID Scanner and ADS-B Scanner Android App
 * This app scans for Remote ID broadcasts via Bluetooth and Wi-Fi (upper half)
 * and retrieves ADS-B data using the user's location (lower half).
 */

package com.example.remoteidscanner;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "ADS-B Scanner";
    private BluetoothLeScanner bluetoothLeScanner;
    private WifiManager wifiManager;
    private LocationManager locationManager;
    private ListView remoteIdListView, adsbListView;
    private TextView locationTextView, apiCallTextView, apiResponseTextView;
    private ArrayAdapter<String> remoteIdAdapter, adsbAdapter;
    private List<String> detectedRemoteDevices, detectedAdsbFlights;
    private Handler wifiScanHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize components
        remoteIdListView = findViewById(R.id.remote_id_list);
        adsbListView = findViewById(R.id.adsb_list);
        locationTextView = findViewById(R.id.location_text);
        apiCallTextView = findViewById(R.id.api_call_text);
        apiResponseTextView = findViewById(R.id.api_response_text);

        detectedRemoteDevices = new ArrayList<>();
        detectedAdsbFlights = new ArrayList<>();

        remoteIdAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, detectedRemoteDevices);
        adsbAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, detectedAdsbFlights);

        remoteIdListView.setAdapter(remoteIdAdapter);
        adsbListView.setAdapter(adsbAdapter);

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            }, PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        }
        //ActivityCompat.requestPermissions(this,
        //        new String[]{
        //                Manifest.permission.BLUETOOTH_SCAN,
        //                Manifest.permission.ACCESS_FINE_LOCATION
        //        }, PERMISSION_REQUEST_CODE);
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

    /*private void initializeScanning() {
        // Initialize Bluetooth scanner
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            startBluetoothScan();
        }

        // Initialize Wi-Fi manager
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        startContinuousWifiScan();

        // Initialize location manager for ADS-B
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        startAdsbScan();
    }*/
    private void initializeScanning() {
        // Check and request permissions before scanning
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12 and above
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                }, PERMISSION_REQUEST_CODE);
                return; // Exit the method until permissions are granted
            }
        } else { // For devices below Android 12
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, PERMISSION_REQUEST_CODE);
                return; // Exit the method until permissions are granted
            }
        }

        // Initialize Bluetooth scanner
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            startBluetoothScan(); // Start scanning
        }

        // Initialize Wi-Fi manager
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        startContinuousWifiScan();

        // Initialize location manager for ADS-B
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        startAdsbScan();
    }


    /*private void startBluetoothScan() {
        ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                String deviceInfo = "Bluetooth: " + result.getDevice().getName() + " (" + result.getDevice().getAddress() + ")";
                if (!detectedRemoteDevices.contains(deviceInfo)) {
                    detectedRemoteDevices.add(deviceInfo);
                    remoteIdAdapter.notifyDataSetChanged();
                }
            }
        };
        bluetoothLeScanner.startScan(scanCallback);
    }*/
    private void startBluetoothScan() {
        ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                String deviceInfo = "Bluetooth: " + result.getDevice().getName() + " (" + result.getDevice().getAddress() + ")";
                if (!detectedRemoteDevices.contains(deviceInfo)) {
                    detectedRemoteDevices.add(deviceInfo);
                    remoteIdAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                for (ScanResult result : results) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    Log.d(TAG, "Batch Device Found: " + result.getDevice().getName() + " (" + result.getDevice().getAddress() + ")");
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e(TAG, "Bluetooth Scan Failed with Error Code: " + errorCode);
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
                        return;
                    }
                    List<android.net.wifi.ScanResult> wifiResults = wifiManager.getScanResults();
                    for (android.net.wifi.ScanResult result : wifiResults) {
                        String deviceInfo = "Wi-Fi: " + result.SSID + " (" + result.BSSID + ")";
                        if (!detectedRemoteDevices.contains(deviceInfo)) {
                            detectedRemoteDevices.add(deviceInfo);
                            remoteIdAdapter.notifyDataSetChanged();
                        }
                    }
                }
                wifiScanHandler.postDelayed(this, 10000); // Re-scan every 10 seconds
            }
        };
        wifiScanHandler.post(wifiScanRunnable);
    }

    private void startAdsbScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                Log.d(TAG, "Fetching ADS-B data for location: " + latitude + ", " + longitude);
                locationTextView.setText("Latitude: " + latitude + "\nLongitude: " + longitude);
                fetchAdsbData(latitude, longitude);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(@NonNull String provider) {}

            @Override
            public void onProviderDisabled(@NonNull String provider) {}
        });
    }

    private void fetchAdsbData(double latitude, double longitude) {
        String url = "http://api.airplanes.live/v2/point/" + latitude + "/" + longitude + "/100";
        Log.d(TAG, "ADS-B API URL: " + url);
        apiCallTextView.setText("API Call: " + url);

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "ADS-B API Response: " + response);
                        apiResponseTextView.setText("API Response: " + response);
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            JSONArray flights = jsonResponse.getJSONArray("flights");
                            detectedAdsbFlights.clear();
                            for (int i = 0; i < flights.length(); i++) {
                                JSONObject flight = flights.getJSONObject(i);
                                String flightNumber = flight.optString("flight_number", "Unknown");
                                if (!flightNumber.isEmpty()) {
                                    detectedAdsbFlights.add("Flight: " + flightNumber);
                                }
                            }
                            adsbAdapter.notifyDataSetChanged();
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing ADS-B data", e);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching ADS-B data: " + error.getMessage(), error);
                        apiResponseTextView.setText("Error: " + error.getMessage());
                    }
                });

        queue.add(stringRequest);
    }
}
