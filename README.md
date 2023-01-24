# WifiDirectTest2
WiFi Direct Test

Project created to demonstrate WiFiP2PManager crashes on connect with Android 12. Works fine with Android versions 7 to 11

Requires another device (IoT etc) that provides a WiFi Direct access point with a persistent group to connect to.

Other device will require a p2p capable network card and wpa_supplicant.conf configured with something like:

```
ctrl_interface=/var/run/wpa_supplicant
ctrl_interface_group=0
device_name=mydevice-wd-0000
device_type=1-0050F204-1
#config_methods=virtual_push_button

p2p_go_intent=15
#p2p_go_ht40=1
#bss_max_count=300
#p2p_go_max_inactivity=10
ignore_old_scan_res=1

network={
        ssid="DIRECT-CE-0000"
        psk="whatever"
        proto=RSN
        key_mgmt=WPA-PSK
        pairwise=CCMP
        group=CCMP
        auth_alg=OPEN
        mode=3
        disabled=2
}
```

then execute:
```
wpa_cli p2p_group_add persistent=0
wpa_cli wps_pbc
```
you will need to configure some method of asigning a static IP address and DHCP service for the created interface p2p-wlan0-0 so that connecting clients can be assigned an IP address
```
ip a 
5: p2p-wlan0-0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue state UP group default qlen 1000
    link/ether 64:79:f0:39:1a:ef brd ff:ff:ff:ff:ff:ff
    inet 192.168.51.1/24 brd 192.168.51.255 scope global p2p-wlan0-0
       valid_lft forever preferred_lft forever
    inet6 fe80::6679:f0ff:fe39:1aef/64 scope link
       valid_lft forever preferred_lft forever
```

