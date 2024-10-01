package com.example.wifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    lateinit var wifiManager: WifiManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the WifiManager
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // Request location permission if not granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        // Register a BroadcastReceiver to listen for scan results
        val wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    Log.d("WiFiScan", "WiFi scan successful")
                    logWiFiScanResults()  // If scan successful, log results
                } else {
                    Log.e("WiFiScan", "WiFi scan failed")
                    Toast.makeText(context, "WiFi Scan Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Register the receiver for WiFi scan results
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, intentFilter)

        // Start a WiFi scan
        Log.d("WiFiScan", "Starting WiFi scan")
        wifiManager.startScan()
    }

    // Handle the result of permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // If permission granted, start WiFi scan
            wifiManager.startScan()
        } else {
            Toast.makeText(this, "Location permission is required for WiFi scanning", Toast.LENGTH_SHORT).show()
        }
    }

    // Method to log WiFi scan results
    private fun logWiFiScanResults() {
        // Check if the app has location permission before accessing WiFi scan results
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission is required to get WiFi scan results", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val scanResults = wifiManager.scanResults
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            // Use internal storage to store the file (no permissions required)
            val file = File(filesDir, "wifi_scan_logs.txt")

            if (!file.exists()) {
                Log.d("WiFiScan", "Creating file at: ${file.absolutePath}")
                val created = file.createNewFile()
                if (created) {
                    Log.d("WiFiScan", "File successfully created")
                } else {
                    Log.e("WiFiScan", "File creation failed")
                }
            }

            val fileWriter = FileWriter(file, true)  // Append mode
            for (result: ScanResult in scanResults) {
                // Format: <timestamp, SSID, MAC Address, RSSI Level>
                val logEntry = "$timestamp, ${result.SSID}, ${result.BSSID}, ${result.level}\n"
                fileWriter.append(logEntry)
                Log.d("WiFiScan", "Log entry: $logEntry")
            }

            fileWriter.close()
            Log.d("WiFiScan", "WiFi Scan Logs saved to: ${file.absolutePath}")
            Toast.makeText(this, "WiFi Scan Logs Saved", Toast.LENGTH_SHORT).show()

        } catch (e: SecurityException) {
            Log.e("WiFiScan", "Location permission required to access scan results", e)
            Toast.makeText(this, "Location permission required to access scan results", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Log.e("WiFiScan", "Error writing WiFi scan logs to file", e)
        }
    }
}
