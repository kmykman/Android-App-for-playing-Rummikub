package com.makerlab.bt;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import androidx.core.app.ActivityCompat;
import android.util.Log;

import com.makerlab.ui.BuildConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BluetoothScan {
    static public final int REQUEST_BT_PERMISSION = 39;
    static public final int REQUEST_BT_ENABLE = 22;
    static private String LOG_TAG =BluetoothScan.class.getSimpleName();
    static private ArrayList<ScanFilter> mServiceUUIDfilters;
    static public final boolean D = BuildConfig.DEBUG;
    //
    private BluetoothAdapter mBluetoothAdapter;
    private BleScanCallback mBleScanCallback;
    private ClassicBtDiscoveryCallback mDiscoveryBroadcastReceiver;
    private ClassicBtDiscoveryResultCallback mClassBtDiscveryResultCallback;
    private boolean mPermssionGranted = false;
    private Activity activity;
    private Map<String, BluetoothDevice> mBluetoothDeviceMap;
    private ResultHandler handler;
    private boolean registeredReceiver=false;

    static{
        mServiceUUIDfilters = new ArrayList<>();
        ParcelUuid uuid1 = new ParcelUuid(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"));
        ParcelUuid uuid2 = new ParcelUuid(UUID.fromString("0000dfb0-0000-1000-8000-00805f9b34fb"));
        ScanFilter filter1 = new ScanFilter.Builder().setServiceUuid(uuid1).build();
        ScanFilter filter2 = new ScanFilter.Builder().setServiceUuid(uuid2).build();
        mServiceUUIDfilters.add(filter1);
        mServiceUUIDfilters.add(filter2);
    }

    public BluetoothScan(Activity activity) {
        this.activity = activity;
        mBluetoothDeviceMap = new HashMap<>();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBleScanCallback = new BleScanCallback();
        mDiscoveryBroadcastReceiver = new ClassicBtDiscoveryCallback();
        mClassBtDiscveryResultCallback = new ClassicBtDiscoveryResultCallback();
    }

    public void setResultHandler(ResultHandler handler) {
        this.handler = handler;
    }

    public BluetoothDevice getBluetoothDevice(String address) {
        BluetoothDevice bluetoothDevice = null;
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(intent, REQUEST_BT_ENABLE);
        } else {

            bluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
        }
        return bluetoothDevice;
    }

    public boolean start() {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(intent, REQUEST_BT_ENABLE);
            return false;
        }
        //
        ActivityCompat.requestPermissions(activity,
                new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                0);
        // for paired devices
        addClassicBtPairedDevices();
        // for classic SPP devices
        if (!mBluetoothAdapter.isDiscovering()) {
            activity.registerReceiver(mDiscoveryBroadcastReceiver,
                    new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
            activity.registerReceiver(mDiscoveryBroadcastReceiver,
                    new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
            activity.registerReceiver(mClassBtDiscveryResultCallback,
                    new IntentFilter(BluetoothDevice.ACTION_FOUND));
            mBluetoothAdapter.startDiscovery();
            registeredReceiver=true;
        }
        // for BLE devices
        BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner != null) {
//           bluetoothLeScanner.startScan(mBleScanCallback);
            bluetoothLeScanner.startScan(mServiceUUIDfilters,
                    new ScanSettings.Builder().setScanMode(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build(),
                    mBleScanCallback);
            Log.e(LOG_TAG, "start()- Discovery Started...");
        }

        return true;
    }

    public void stop() {
        Log.e(LOG_TAG, "stop()- Discovery stopped...");
        if (!mBluetoothAdapter.isEnabled()) return;
        // cancel BLE scanning
        mBluetoothAdapter.getBluetoothLeScanner().stopScan(mBleScanCallback);
        // cancel Bluetooth Class Scanning
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        if (registeredReceiver) {
            registeredReceiver=false;
            activity.unregisterReceiver(mDiscoveryBroadcastReceiver);
            activity.unregisterReceiver(mClassBtDiscveryResultCallback);
        }
    }

    public Map<String, BluetoothDevice> getmBluetoothDevices() {
        return mBluetoothDeviceMap;
    }

    private void addClassicBtPairedDevices() {
        Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
        Iterator<BluetoothDevice> loop = bondedDevices.iterator();
        while (loop.hasNext()) {
            BluetoothDevice remoteDevice = loop.next();
            BluetoothClass btClass = remoteDevice.getBluetoothClass();
            //Log.e(LOG_TAG, "addClassicBtPairedDevices()"+remoteDevice.getName()+", "+btClass.getMajorDeviceClass());
            if (btClass.getMajorDeviceClass() == BluetoothClass.Device.Major.UNCATEGORIZED ||
                    btClass.getMajorDeviceClass() == BluetoothClass.Device.Major.COMPUTER) {
                mBluetoothDeviceMap.put(remoteDevice.getAddress(), remoteDevice);
                if (handler != null) {
                    handler.setResult(remoteDevice);
                }
            }
        }
    }


    //
    public class ClassicBtDiscoveryCallback extends BroadcastReceiver {
        private String LOG_TAG =ClassicBtDiscoveryCallback.class.getSimpleName();
        @Override
        public void onReceive(Context context, Intent intent) {
            String intentActon = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intentActon)) {
                if (D) {
                    Log.e(LOG_TAG, "onReceive() - Discovery Started...");
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intentActon)) {
                mBluetoothAdapter.getBluetoothLeScanner().stopScan(mBleScanCallback);
                if (D) {
                    Log.e(LOG_TAG, "onReceive() - Discovery Complete.");
                }
                if (handler != null) {
                    handler.onPostResult();
                }
            }
        }
    }

    //
    public class ClassicBtDiscoveryResultCallback extends BroadcastReceiver {
        private String LOG_TAG =ClassicBtDiscoveryResultCallback.class.getSimpleName();
        @Override
        public void onReceive(Context context, Intent intent) {
//            String remoteDeviceName =
//                    intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
            BluetoothDevice remoteDevice =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (remoteDevice != null) {
                mBluetoothDeviceMap.put(remoteDevice.getAddress(), remoteDevice);
                BluetoothClass btClass = remoteDevice.getBluetoothClass();
                if (btClass.getMajorDeviceClass() == BluetoothClass.Device.Major.UNCATEGORIZED ||
                        btClass.getMajorDeviceClass() == BluetoothClass.Device.Major.COMPUTER) {
                    if (handler != null) {
                        handler.setResult(remoteDevice);
                    }
                }
            }
        }
    }

    //
    public class BleScanCallback extends ScanCallback {
        private String LOG_TAG =BleScanCallback.class.getSimpleName();
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice remoteDevice = result.getDevice();
            String name = remoteDevice.getName();
            if (name != null) {
                if (D) {
                    Log.e(LOG_TAG, "onScanResult() called - found " + remoteDevice.getName());
                }
                mBluetoothDeviceMap.put(remoteDevice.getAddress(), remoteDevice);
                if (handler != null) {
                    handler.setResult(remoteDevice);
                }
            } else {
                if (D) {
                    Log.e(LOG_TAG, "onScanResult() called");
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(LOG_TAG,"onScanFailed() - error code "+String.valueOf(errorCode));
            if (handler != null) {
                handler.onPostResult();
            }
        }
    }

    public interface ResultHandler {
        public void setResult(BluetoothDevice bluetoothDevice);

        public void onPostResult();
    }
}
