package com.voice.studio;

import android.content.*;
import android.media.*;
import android.os.Build;

import java.io.*;
import java.util.concurrent.*;

/** Optional Bluetooth communication route, restored when user recording finishes. */
final class RecordRoute implements AutoCloseable {
    final AudioManager manager;
    final boolean enabled;
    int mode;
    AudioDeviceInfo previous;
    boolean priorSco, startedSco;

    RecordRoute(Context context, boolean bluetooth) throws Exception {
        manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        enabled = bluetooth;
        if (!enabled) return;
        mode = manager.getMode();
        priorSco = manager.isBluetoothScoOn();
        try {
            manager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            if (Build.VERSION.SDK_INT >= 31) {
                previous = manager.getCommunicationDevice();
                AudioDeviceInfo chosen = null;
                for (AudioDeviceInfo device : manager.getAvailableCommunicationDevices())
                    if (device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                            || device.getType() == AudioDeviceInfo.TYPE_BLE_HEADSET) {
                        chosen = device;
                        break;
                    }
                if (chosen == null || !manager.setCommunicationDevice(chosen))
                    throw new IOException("未找到可用的蓝牙通话耳机，请先在手机设置连接");
            } else if (!priorSco) {
                if (!manager.isBluetoothScoAvailableOffCall()) throw new IOException("手机不支持蓝牙通话录音");
                CountDownLatch connected = new CountDownLatch(1);
                BroadcastReceiver receiver =
                        new BroadcastReceiver() {
                            public void onReceive(Context c, Intent i) {
                                if (i.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
                                        == AudioManager.SCO_AUDIO_STATE_CONNECTED)
                                    connected.countDown();
                            }
                        };
                context.registerReceiver(
                        receiver, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
                try {
                    manager.startBluetoothSco();
                    startedSco = true;
                    manager.setBluetoothScoOn(true);
                    if (!connected.await(10, TimeUnit.SECONDS))
                        throw new IOException("蓝牙录音连接超时，请重新连接耳机");
                } finally {
                    context.unregisterReceiver(receiver);
                }
            }
        } catch (Exception e) {
            close();
            throw e;
        }
    }

    public void close() {
        if (!enabled) return;
        if (Build.VERSION.SDK_INT >= 31) {
            if (previous != null) manager.setCommunicationDevice(previous);
            else manager.clearCommunicationDevice();
        } else {
            if (startedSco) manager.stopBluetoothSco();
            manager.setBluetoothScoOn(priorSco);
        }
        manager.setMode(mode);
    }
}
