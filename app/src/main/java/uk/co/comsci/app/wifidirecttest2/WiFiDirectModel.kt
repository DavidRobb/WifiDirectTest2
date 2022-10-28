package uk.co.comsci.app.wifidirecttest2

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.*
import android.os.Parcelable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber

class WiFiDirectModel(
    private var channel: WifiP2pManager.Channel,
    private var manager: WifiP2pManager
) : ViewModel() {

    var isWifiP2pEnabled: Boolean = false
    private val intentFilter = IntentFilter()
    var receiver = WiFiDirectBroadcastReceiver(this)
    private var peers = mutableListOf<WifiP2pDevice>()

    val deviceInfoFLow = MutableStateFlow(WifiP2pDeviceList())
    val connectedDeviceFlow = MutableStateFlow<WifiP2pGroup?>(null)

    init {
        // Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)

        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    @SuppressLint("MissingPermission")
    fun onGroupInfoAvailable(goIpAddress: String, p2pGroupInfo: WifiP2pGroup?) {
        val devName = p2pGroupInfo?.owner?.deviceName ?: ""
        Timber.w("Connected to device at $devName on IP $goIpAddress and DA ${p2pGroupInfo?.owner?.deviceAddress}")
        connectedDeviceFlow.value = p2pGroupInfo
    }

    fun discoverPeers() {
        Timber.i("Doing P2P Discover peers")
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                Timber.i("P2P start Discover peers success!")
                // Code for when the discovery initiation is successful goes here.
                // No services have actually been discovered yet, so this method
                // can often be left blank. Code for peer discovery goes in the
                // onReceive method, detailed below.
            }

            override fun onFailure(reasonCode: Int) {
                Timber.w("P2P Discover peers failed with reason $reasonCode")
                // Code for when the discovery initiation fails goes here.
                // Alert the user that something went wrong.
            }
        })
    }

    fun connect(device: WifiP2pDevice) {

        Timber.i("Attempting connect to device ${device.deviceName} at ${device.deviceAddress}")

        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }

        try {

            manager.connect(channel, config, object : WifiP2pManager.ActionListener {

                override fun onSuccess() {
                    Timber.i("P2P start connect to ${device.deviceAddress} success")
                    // WiFiDirectBroadcastReceiver notifies us. Ignore for now.
                }

                override fun onFailure(reason: Int) {
                    Timber.w("P2P connect to ${device.deviceAddress} failed with reason $reason")
                }
            })
        } catch (e: Exception) {
            Timber.e(e, "Caught connect throwable! ${e.cause}")
        }
    }

    fun peersChanged() {
        manager.requestPeers(channel) {
            val refreshedPeers = it.deviceList
            if (refreshedPeers != peers) {
                peers.clear()
                peers.addAll(refreshedPeers)

                for (p in peers) {
                    Timber.i("Discovered peer ${p.deviceName} at ${p.deviceAddress}")
                }
            }

            deviceInfoFLow.value = it
            if (peers.isEmpty()) {
                Timber.i("No devices found")
                return@requestPeers
            }
        }
    }

    fun connectionInfoChanged(intent: Intent) {
        val networkInfo =
            intent.getParcelableExtra<Parcelable>(WifiP2pManager.EXTRA_NETWORK_INFO) as NetworkInfo?
        Timber.i("P2P connection network Info Rxd ${networkInfo.toString()}")
        if (networkInfo?.isConnected == true) {
            Timber.d("Requesting extra P2P connection info")
            // we are connected with the other device, request connection
            // info to find group owner IP
            manager.requestConnectionInfo(channel) { connInfo ->
                val goIpAddress = connInfo.groupOwnerAddress?.hostAddress.toString()
                Timber.d("P2P device requesting Group info")
                manager.requestGroupInfo(channel) { groupInfo ->
                    onGroupInfoAvailable(goIpAddress, groupInfo)
                }
            }
        } else {
            // It's a disconnect
            Timber.i("Connection disconnect")
            connectedDeviceFlow.value = null
        }
    }

    fun updateThisDevice(p2pDevice: WifiP2pDevice?) {
        Timber.i("Got P2P this device  ${p2pDevice.toString()}")
    }


    fun disconnect(it: WifiP2pGroup) {
        manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onFailure(reasonCode: Int) {
                Timber.e("Disconnect failed. Reason :$reasonCode")
            }

            override fun onSuccess() {
                Timber.i("Disconnect success")
                connectedDeviceFlow.value = null
            }
        })
    }
}
