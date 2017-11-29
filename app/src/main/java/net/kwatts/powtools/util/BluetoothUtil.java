package net.kwatts.powtools.util;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import net.kwatts.powtools.MainActivity;
import net.kwatts.powtools.model.OWDevice;

public interface BluetoothUtil {
    void init(MainActivity mainActivity, OWDevice mOWDevice);
    void reconnect(Activity activity);
    void stopScanning();
    void disconnect();
    boolean isConnected();
    boolean isScanning();
    void startScanning();
    BluetoothGattCharacteristic getCharacteristic(String onewheelCharacteristicLightingMode);
    void writeCharacteristic(BluetoothGattCharacteristic lc);
}
