# Keep reflection-based WifiManager methods for API 21-25 hotspot toggle
-keepclassmembers class android.net.wifi.WifiManager {
    public boolean setWifiApEnabled(android.net.wifi.WifiConfiguration, boolean);
    public int getWifiApState();
    public boolean isWifiApEnabled();
    public android.net.wifi.WifiConfiguration getWifiApConfiguration();
    public boolean setWifiApConfiguration(android.net.wifi.WifiConfiguration);
}

# Hilt
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
