package com.makerlab.bt;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;

public class BluetoothConnect implements Serializable {
    static private String LOG_TAG = BluetoothConnect.class.getSimpleName();
    //
    private BluetoothDevice mBluetoothDevice;
    // Bluetooth LE
    private BluetoothGatt mBluetoothGatt = null;
    private BluetoothGattCharacteristic mGattCharacteristic = null;
    // classic Bluetooth SPP
    private BluetoothSocket mBluetoothSocket = null;
    private OutputStream mOutputStream;
    private InputStream mInputStream;

    private Activity mActivity;
    //
    private int mPrevChecksum = -1;
    private DisonnectedState mDisonnectedState;
    private ConnectionHandler mConnectionHandler;
    private boolean mIsConnected = false;
    private BluetoothConnect self;


    public BluetoothConnect(Activity activity) {
        mActivity = activity;
        self = this;
    }

    public void setConnectionHandler(ConnectionHandler mConnectionHandler) {
        this.mConnectionHandler = mConnectionHandler;
    }

    public void connectBluetooth(BluetoothDevice bluetoothDevice) {
        if (isConnected()) return;
        this.mBluetoothDevice = bluetoothDevice;
        connectBluetooth();
    }

    public void connectBluetooth() {
        BtSocketConnectAsyncTask btSocketConnectAsyncTask = new BtSocketConnectAsyncTask(mActivity, mBluetoothDevice);
        btSocketConnectAsyncTask.execute();
    }

    public void disconnectBluetooth() {
        if (mDisonnectedState != null) {
            mActivity.unregisterReceiver(mDisonnectedState);
            mDisonnectedState = null;
        }

        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
        }
        mBluetoothGatt = null;
        mGattCharacteristic = null;
        //
        try {
            if (mBluetoothSocket != null) {
                if (mOutputStream != null) {
                    mOutputStream.close();
                }
                if (mInputStream != null) {
                    mInputStream.close();
                }
                mBluetoothSocket.close();
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "disconnectBluetooth(): " + e.toString());
        } finally {
            mBluetoothSocket = null;
            mOutputStream = null;
            mInputStream = null;
        }
        //
        mPrevChecksum = -1;
        mIsConnected = false;
    }

    public String getDeviceAddress() {
        String addr = null;
        if (mBluetoothDevice != null) {
            addr = mBluetoothDevice.getAddress();
        }
        return addr;
    }

    public String getDeviceName() {
        String name = null;
        if (mBluetoothDevice != null) {
            name = mBluetoothDevice.getName();
        }
        return name;
    }


    public boolean isConnected() {
        return mIsConnected;
    }

    public int available() {
        return 0;
    }

    public String read() throws IOException {
        byte[] buffer = new byte[256];
        String message = null;
        if (mOutputStream != null && mBluetoothSocket.isConnected()) {
            int length = mInputStream.read(buffer);
            message = new String(buffer, 0, length);
            return message;
        }
        return null;
    }

    public boolean write(String buffer) {
        return send(buffer.getBytes());
    }

    public boolean send(byte[] payload) {
        if (!isConnected()) {
            Log.d(LOG_TAG, "send(): connection not set!");
            return false;
        }
        if (payload == null || payload.length == 0) {
            //Log.d(LOG_TAG, "send(): invalid payload");
            return false;
        }
        boolean isSuccess = false;
        if (mGattCharacteristic != null) {
            final int maxLength = 20;
            byte[] buffer;
            int loop = payload.length / maxLength;
            int from = 0;
            int to = 0;
            for (int i = 0; i < loop; i++) {
                to += (maxLength);
//                Log.e(LOG_TAG, "send(): A from "+String.valueOf(from));
//                Log.e(LOG_TAG, "send(): A to "+String.valueOf(to));
                buffer = Arrays.copyOfRange(payload, from, to);
//                Log.e(LOG_TAG, "send(): A buffer length "+buffer.length);
                mGattCharacteristic.setValue(buffer);
                mBluetoothGatt.writeCharacteristic(mGattCharacteristic);
                from = to;
                try {
                    Thread.sleep(20);
                } catch (Exception e) {

                }
            }
            int remain = payload.length % maxLength;
            if (remain > 0) {
                to += remain;
//                Log.e(LOG_TAG, "send(): B from "+String.valueOf(from));
//                Log.e(LOG_TAG, "send(): B to "+String.valueOf(to));
                buffer = Arrays.copyOfRange(payload, from, to);
//                Log.e(LOG_TAG, "send(): B buffer length "+buffer.length);
                mGattCharacteristic.setValue(buffer);
                mBluetoothGatt.writeCharacteristic(mGattCharacteristic);
            }
            isSuccess = true;
        } else if (mOutputStream != null) {
            try {
                mOutputStream.write(payload);
                mOutputStream.flush();
                isSuccess = true;
            } catch (IOException e) {
                Log.e(LOG_TAG, "send(): " + e.toString());
                disconnectBluetooth();
            }
        } else {
            Log.e(LOG_TAG, "send(): failed to send, GattCharacteristic or OutputStream is null!");
        }
//        if (isSuccess) {
//            Log.e(LOG_TAG, "send(): sent data " + new String(payload));
//        }

        return isSuccess;
    }


    //  AsyncTask
    class BtSocketConnectAsyncTask extends AsyncTask<Void, Void, String> {
        private Activity activity;
        private BluetoothDevice bluetoothDevice;
        private BluetoothGattCallback mGattCallback = new BluetoothGattCallback();

        public BtSocketConnectAsyncTask(Activity activity, BluetoothDevice bluetoothDevice) {
            super();
            this.activity = activity;
            this.bluetoothDevice = bluetoothDevice;
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mConnectionHandler != null) {
                mConnectionHandler.onConnect(self);
            }
        }

        @Override
        protected String doInBackground(Void... voids) {
            int btDeviceType = BluetoothDevice.DEVICE_TYPE_UNKNOWN;

            btDeviceType = bluetoothDevice.getType();
            if (btDeviceType == BluetoothDevice.DEVICE_TYPE_LE) {
                bluetoothDevice.connectGatt(activity, false, mGattCallback);
                return null;
            } else if (btDeviceType == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
                if (mConnectionHandler != null) {
                    mConnectionHandler.onConnect(self);
                }
                mIsConnected = connectClassicBlueTooth(bluetoothDevice);
                if (mConnectionHandler != null) {
                    if (mIsConnected) {
                        mConnectionHandler.onConnectionSuccess(self);
                    } else {
                        mConnectionHandler.onConnectionFail(self);
                    }
                }
            }
            return null;
        }

/*
        protected void onPostExecute(String result) {

        }
*/

        private boolean connectClassicBlueTooth(BluetoothDevice bluetoothDevice) {
            boolean rc = true;
            try {
                ParcelUuid[] uuids = bluetoothDevice.getUuids();
                UUID SPPuuid;
                if (uuids == null || uuids.length == 0) {
                    SPPuuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                } else {
                    SPPuuid = uuids[0].getUuid();
                }
                mBluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(SPPuuid);
                mBluetoothSocket.connect();
                mOutputStream = mBluetoothSocket.getOutputStream();
                mInputStream = mBluetoothSocket.getInputStream();
                IntentFilter f2 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                IntentFilter f1 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
                mDisonnectedState = new DisonnectedState();
                mActivity.registerReceiver(mDisonnectedState, f1);
                mActivity.registerReceiver(mDisonnectedState, f2);
            } catch (IOException e) {
                Log.e(LOG_TAG, "connectClassicBlueTooth(): " + e.toString());
                //e.printStackTrace();
                mOutputStream = null;
                mInputStream = null;
                mBluetoothSocket = null;
                rc = false;
            }
            return rc;
        }

    }//  AsyncTask

    //
    class BluetoothGattCallback extends android.bluetooth.BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // query the device for available GATT services,
                // onServicesDiscovered() will be called if there are services found,
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                //disconnectBluetooth();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                bindServiceAndCharacteristics(gatt);
                Log.e(LOG_TAG, "onServicesDiscovered()");
            }
        }

        // called if receiving data
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] buff = characteristic.getValue();
            //Log.e("onCharacteristicChanged", "recevied: " + buff.length);
            // An updated value has been received for a characteristic.
        }

        private void bindServiceAndCharacteristics(BluetoothGatt gatt) {
            String serviceUuid[] = {
                    "0000ffe0-0000-1000-8000-00805f9b34fb",
                    "0000dfb0-0000-1000-8000-00805f9b34fb"
            };
            String charUuid[] = {
                    "0000ffe1-0000-1000-8000-00805f9b34fb",
                    "0000dfb1-0000-1000-8000-00805f9b34fb"
            };
            mIsConnected = false;
            BluetoothGattService gattService = null;
            int i = 0;
            for (i = 0; i < serviceUuid.length; i++) {
                gattService = gatt.getService(UUID.fromString(serviceUuid[i]));
                if (gattService != null) break;
            }
            if (gattService == null) {
                gatt.close();
                if (mConnectionHandler != null) {
                    mConnectionHandler.onConnectionFail(self);
                }
                return;
            }

            mGattCharacteristic = gattService.getCharacteristic(UUID.fromString(charUuid[i]));
            if (mGattCharacteristic == null) {
                gatt.close();
                if (mConnectionHandler != null) {
                    mConnectionHandler.onConnectionFail(self);
                }
                return;
            }

            // turn on data receiving event for the characteristic
            gatt.setCharacteristicNotification(mGattCharacteristic, true);
            //
            mBluetoothGatt = gatt;
            mIsConnected = true;
            //
            IntentFilter f2 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            IntentFilter f1 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            mDisonnectedState = new DisonnectedState();
            mActivity.registerReceiver(mDisonnectedState, f1);
            mActivity.registerReceiver(mDisonnectedState, f2);
            //
            if (mConnectionHandler != null) {
                mConnectionHandler.onConnectionSuccess(self);
            }
        }

    }

    // broadcast receiver for bluetooth disconnect
    private class DisonnectedState extends BroadcastReceiver {
        private final String LOG_TAG = DisonnectedState.class.getSimpleName();

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action == null) return;
            if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                //Log.e(LOG_TAG, "onReceive() : device disconnected");
                if (mConnectionHandler != null) {
                    mConnectionHandler.onDisconnected(self);
                    disconnectBluetooth();
                }
            } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                //Log.e(LOG_TAG, "onReceive() : adaptor state changed");
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        if (mConnectionHandler != null) {
                            mConnectionHandler.onDisconnected(self);
                            disconnectBluetooth();
                        }
                        break;
                    case BluetoothAdapter.STATE_ON:
                        break;
                }
            }
        }
    }

    public interface ConnectionHandler {
        void onConnect(BluetoothConnect self);

        void onConnectionSuccess(BluetoothConnect self);

        void onConnectionFail(BluetoothConnect self);

        void onDisconnected(BluetoothConnect self);
    }
}
