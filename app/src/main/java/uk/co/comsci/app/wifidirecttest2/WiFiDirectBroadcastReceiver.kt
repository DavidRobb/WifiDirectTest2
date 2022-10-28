package uk.co.comsci.app.wifidirecttest2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager
import timber.log.Timber

class WiFiDirectBroadcastReceiver(
    private val wiFiDirectModel: WiFiDirectModel
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // Determine if Wi-Fi Direct mode is enabled or not.
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                Timber.w("P2P State changed to $state")
                wiFiDirectModel.isWifiP2pEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {

                wiFiDirectModel.peersChanged()

            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {

                Timber.i("P2P connection info changed")
                wiFiDirectModel.connectionInfoChanged(intent)

            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                Timber.w("P2P This device changed")
                wiFiDirectModel.updateThisDevice(
                    intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                )
            }
        }
    }

}