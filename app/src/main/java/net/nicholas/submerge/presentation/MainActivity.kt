package net.nicholas.submerge.presentation

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionsToRequest = arrayOf(
            android.Manifest.permission.BODY_SENSORS,
            android.Manifest.permission.BODY_SENSORS_BACKGROUND,
            android.Manifest.permission.WAKE_LOCK,
            android.Manifest.permission.FOREGROUND_SERVICE,
            android.Manifest.permission.VIBRATE,
            android.Manifest.permission.POST_NOTIFICATIONS
        )

        val permissionsToRequestList = permissionsToRequest.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequestList.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequestList.toTypedArray(),
                5565
            )
        }

        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        val isServiceRunning = sharedPreferences.getBoolean("isMonitoringEnabled", false)

        setContent {
            SubmergeApp(isServiceRunning, ::startMonitoring, ::stopMonitoring)
        }
    }

    private fun startMonitoring() {
        val serviceIntent = Intent(this, HeartRateService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        sharedPreferences.edit().putBoolean("isMonitoringEnabled", true).apply()
        Log.d("Submerge", "startMonitoring() called")
        // TODO: Consider sending update to MainScreen for button text "Enable" -> "Disable"
    }

    private fun stopMonitoring() {
        val serviceIntent = Intent(this, HeartRateService::class.java)
        stopService(serviceIntent)

        sharedPreferences.edit().putBoolean("isMonitoringEnabled", false).apply()
        Log.d("Submerge", "stopMonitoring() called")
        // TODO: Consider sending update to MainScreen for button text "Disable" -> "Enable"
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun SubmergeApp(isServiceRunning: Boolean, startMonitoring: () -> Unit, stopMonitoring: () -> Unit) {
    val navController = rememberNavController()
    val pagerState = rememberPagerState(initialPage = 1, pageCount = {
        2
        // History <-> DiveScreen
    })

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { pageIndex ->
        when (pageIndex) {
            0 -> HistoryScreen()
            1 -> DiveScreen()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    SubmergeApp(false, startMonitoring = {}, stopMonitoring = {})
}