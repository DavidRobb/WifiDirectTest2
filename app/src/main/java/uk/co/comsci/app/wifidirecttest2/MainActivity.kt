package uk.co.comsci.app.wifidirecttest2

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import timber.log.Timber
import uk.co.comsci.app.wifidirecttest2.ui.theme.WifiDirectTest2Theme

class MainActivity : ComponentActivity() {

    lateinit var wiFiDirectModel: WiFiDirectModel
    private val intentFilter = IntentFilter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.plant(Timber.DebugTree())

        // Indicates a change in the Wi-Fi Direct status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)

        // Indicates the state of Wi-Fi Direct connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)

        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

        val manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        val channel = manager.initialize(this, mainLooper, null)
        wiFiDirectModel = WiFiDirectModel(channel, manager)

        setContent {
            WifiDirectTest2Theme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    StartScanWithPermissions(activity = this)
                }
            }
        }
    }

    /** register the BroadcastReceiver with the intent values to be matched  */
    override fun onResume() {
        super.onResume()
        val receiver = WiFiDirectBroadcastReceiver(wiFiDirectModel)
        registerReceiver(receiver, intentFilter)
        wiFiDirectModel.receiver = receiver

    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(wiFiDirectModel.receiver)
    }

    fun startWifiScan() {
        Timber.i("start wifi scan")

        wiFiDirectModel.discoverPeers()

    }

    companion object {
        val REQUIRED_PERMISSIONS =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listOf(
                    Manifest.permission.NEARBY_WIFI_DEVICES,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                listOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun StartScanWithPermissions(activity: MainActivity) {
    val locationPermissionsState =
        rememberMultiplePermissionsState(MainActivity.REQUIRED_PERMISSIONS)

    if (locationPermissionsState.allPermissionsGranted) {
        WiFiTest(activity = activity)
    } else {
        Column {
            val allPermissionsRevoked =
                locationPermissionsState.permissions.size ==
                        locationPermissionsState.revokedPermissions.size

            val textToShow = if (!allPermissionsRevoked) {
                // If not all the permissions are revoked, it's because the user accepted the COARSE
                // location permission, but not the FINE one.
                "Yay! Thanks for letting me access your approximate location. " +
                        "But you know what would be great? If you allow me to know where you " +
                        "exactly are. Thank you!"
            } else if (locationPermissionsState.shouldShowRationale) {
                // Both location permissions have been denied
                "Getting your exact location is important for this app. " +
                        "Please grant us fine location. Thank you :D"
            } else {
                // First time the user sees this feature or the user doesn't want to be asked again
                "This feature requires location permission"
            }

            val buttonText = if (!allPermissionsRevoked) {
                "Allow precise location"
            } else {
                "Request permissions"
            }

            Text(text = textToShow)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { locationPermissionsState.launchMultiplePermissionRequest() }) {
                Text(buttonText)
            }
        }
    }
}

@Composable
fun WiFiTest(activity: MainActivity) {
    val peers = activity.wiFiDirectModel.deviceInfoFLow.collectAsStateWithLifecycle()
    val connectedDevice = activity.wiFiDirectModel.connectedDeviceFlow.collectAsStateWithLifecycle()
    Column(Modifier.padding(40.dp)) {
        Button(onClick = { activity.startWifiScan() }) {
            Text(text = "Scan")
        }

        for (p in peers.value.deviceList) {
            Card(
                backgroundColor = Color.LightGray,
                modifier = Modifier
                    .height(100.dp)
                    .padding(20.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Button(onClick = { activity.wiFiDirectModel.connect(p) }) {
                        Text("Connect to ${p.deviceName}")

                    }
                }
            }
        }
        connectedDevice.value?.let {
            Button(onClick = { activity.wiFiDirectModel.disconnect(it) }) {
                Text("DisConnect from ${it.networkName}")

            }
        }
    }

}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    WifiDirectTest2Theme {
    }
}