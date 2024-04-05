package com.fyp.rummikub;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.makerlab.bt.BluetoothConnect;
import com.makerlab.bt.BluetoothScan;
import com.makerlab.ui.BluetoothDevListActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class GameActivity extends AppCompatActivity implements
        BluetoothConnect.ConnectionHandler {
    static public final boolean D = BuildConfig.DEBUG;
    static public final int REQUEST_BT_GET_DEVICE = 1112;
    static public final String BLUETOOT_REMOTE_DEVICE = "bt_remote_device";
    static private String LOG_TAG = GameActivity.class.getSimpleName();
    private String DATA_PATH;
    private AssetManager assetManager;
    private BluetoothConnect mBluetoothConnect;
    private BluetoothScan mBluetoothScan;
    private SharedPreferences mSharedPref;
    private String mSharedPrefFile = "com.fyp.rummikub.sharedprefs";
    public ConnectBluetoothFragment ConnectBluetoothFragment = new ConnectBluetoothFragment();
    private HashMap<Integer, HashMap<String,Object>> robot_rack = new HashMap<>();
    private HashMap<Integer, HashMap<String,Integer>> common_rack = new HashMap<>();
    private int total_rack = 0;
    private String winner = "";

    private int cmd = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        mBluetoothConnect = new BluetoothConnect(this);
        connectBT();

        getSupportFragmentManager().beginTransaction().add(R.id.frag_con, ConnectBluetoothFragment).show(ConnectBluetoothFragment).commit();

        for (int i = 0; i < 18; i++) {
            HashMap<String,Object> item = new HashMap<>();
            item.put("tile", "E");
            if (i == 0) {
                item.put("x", -74);
                item.put("y", 99);
                item.put("z", 124);
            }
            else if (i == 1) {
                item.put("x", -97);
                item.put("y", 99);
                item.put("z", 124);
            }
            else if (i == 2) {
                item.put("x", -119);
                item.put("y", 100);
                item.put("z", 124);
            }
            else if (i == 3) {
                item.put("x", -76);
                item.put("y", 134);
                item.put("z", 98);
            }
            else if (i == 4) {
                item.put("x", -97);
                item.put("y", 131);
                item.put("z", 97);
            }
            else if (i == 5) {
                item.put("x", -122);
                item.put("y", 130);
                item.put("z", 97);
            }
            else if (i == 6) {
                item.put("x", -78);
                item.put("y", 164);
                item.put("z", 70);
            }
            else if (i == 7) {
                item.put("x", -99);
                item.put("y", 163);
                item.put("z", 69);
            }
            else if (i == 8) {
                item.put("x", -120);
                item.put("y", 160);
                item.put("z", 69);
            }
            else if (i == 9) {
                item.put("x", -80);
                item.put("y", 191);
                item.put("z", 40);
            }
            else if (i == 10) {
                item.put("x", -101);
                item.put("y", 192);
                item.put("z", 40);
            }
            else if (i == 11) {
                item.put("x", -124);
                item.put("y", 188);
                item.put("z", 40);
            }
            else if (i == 12) {
                item.put("x", -82);
                item.put("y", 220);
                item.put("z", 13);
            }
            else if (i == 13) {
                item.put("x", -102);
                item.put("y", 218);
                item.put("z", 13);
            }
            else if (i == 14) {
                item.put("x", -125);
                item.put("y", 217);
                item.put("z", 13);
            }
            else if (i == 15) {
                item.put("x", -83);
                item.put("y", 250);
                item.put("z", -8);
            }
            else if (i == 16) {
                item.put("x", -105);
                item.put("y", 247);
                item.put("z", -8);
            }
            else if (i == 17) {
                item.put("x", -129);
                item.put("y", 246);
                item.put("z", -8);
            }
            item.put("placed", false);
            robot_rack.put(i, item);
        }

        for (int i = 0; i < 48; i++) {
            HashMap<String,Integer> item = new HashMap<>();
            if (i == 0) {
                item.put("x", 182);
                item.put("y", 140);
                item.put("z", 121);
            }
            else if (i == 1) {
                item.put("x", 163);
                item.put("y", 142);
                item.put("z", 123);
            }
            else if (i == 2) {
                item.put("x", 142);
                item.put("y", 143);
                item.put("z", 123);
            }
            else if (i == 3) {
                item.put("x", 119);
                item.put("y", 149);
                item.put("z", 123);
            }
            else if (i == 4) {
                item.put("x", 93);
                item.put("y", 149);
                item.put("z", 128);
            }
            else if (i == 5) {
                item.put("x", 73);
                item.put("y", 149);
                item.put("z", 130);
            }
            else if (i == 6) {
                item.put("x", 50);
                item.put("y", 152);
                item.put("z", 128);
            }
            else if (i == 7) {
                item.put("x", 24);
                item.put("y", 153);
                item.put("z", 129);
            }
            else if (i == 8) {
                item.put("x", 185);
                item.put("y", 173);
                item.put("z", 94);
            }
            else if (i == 9) {
                item.put("x", 163);
                item.put("y", 174);
                item.put("z", 96);
            }
            else if (i == 10) {
                item.put("x", 139);
                item.put("y", 175);
                item.put("z", 97);
            }
            else if (i == 11) {
                item.put("x", 119);
                item.put("y", 179);
                item.put("z", 97);
            }
            else if (i == 12) {
                item.put("x", 95);
                item.put("y", 181);
                item.put("z", 99);
            }
            else if (i == 13) {
                item.put("x", 70);
                item.put("y", 183);
                item.put("z", 103);
            }
            else if (i == 14) {
                item.put("x", 47);
                item.put("y", 184);
                item.put("z", 101);
            }
            else if (i == 15) {
                item.put("x", 26);
                item.put("y", 188);
                item.put("z", 101);
            }
            else if (i == 16) {
                item.put("x", 180);
                item.put("y", 204);
                item.put("z", 70);
            }
            else if (i == 17) {
                item.put("x", 159);
                item.put("y", 204);
                item.put("z", 70);
            }
            else if (i == 18) {
                item.put("x", 135);
                item.put("y", 207);
                item.put("z", 70);
            }
            else if (i == 19) {
                item.put("x", 116);
                item.put("y", 208);
                item.put("z", 70);
            }
            else if (i == 20) {
                item.put("x", 90);
                item.put("y", 210);
                item.put("z", 70);
            }
            else if (i == 21) {
                item.put("x", 69);
                item.put("y", 211);
                item.put("z", 71);
            }
            else if (i == 22) {
                item.put("x", 44);
                item.put("y", 212);
                item.put("z", 72);
            }
            else if (i == 23) {
                item.put("x", 25);
                item.put("y", 212);
                item.put("z", 73);
            }
            else if (i == 24) {
                item.put("x", 183);
                item.put("y", 231);
                item.put("z", 41);
            }
            else if (i == 25) {
                item.put("x", 160);
                item.put("y", 235);
                item.put("z", 44);
            }
            else if (i == 26) {
                item.put("x", 139);
                item.put("y", 236);
                item.put("z", 43);
            }
            else if (i == 27) {
                item.put("x", 117);
                item.put("y", 239);
                item.put("z", 46);
            }
            else if (i == 28) {
                item.put("x", 93);
                item.put("y", 241);
                item.put("z", 46);
            }
            else if (i == 29) {
                item.put("x", 72);
                item.put("y", 242);
                item.put("z", 46);
            }
            else if (i == 30) {
                item.put("x", 47);
                item.put("y", 244);
                item.put("z", 47);
            }
            else if (i == 31) {
                item.put("x", 26);
                item.put("y", 245);
                item.put("z", 47);
            }
            else if (i == 32) {
                item.put("x", 179);
                item.put("y", 264);
                item.put("z", 17);
            }
            else if (i == 33) {
                item.put("x", 158);
                item.put("y", 264);
                item.put("z", 17);
            }
            else if (i == 34) {
                item.put("x", 137);
                item.put("y", 265);
                item.put("z", 19);
            }
            else if (i == 35) {
                item.put("x", 115);
                item.put("y", 268);
                item.put("z", 18);
            }
            else if (i == 36) {
                item.put("x", 91);
                item.put("y", 269);
                item.put("z", 18);
            }
            else if (i == 37) {
                item.put("x", 70);
                item.put("y", 270);
                item.put("z", 19);
            }
            else if (i == 38) {
                item.put("x", 45);
                item.put("y", 270);
                item.put("z", 21);
            }
            else if (i == 39) {
                item.put("x", 24);
                item.put("y", 275);
                item.put("z", 23);
            }
            else if (i == 40) {
                item.put("x", 181);
                item.put("y", 287);
                item.put("z", -8);
            }
            else if (i == 41) {
                item.put("x", 160);
                item.put("y", 288);
                item.put("z", -8);
            }
            else if (i == 42) {
                item.put("x", 139);
                item.put("y", 292);
                item.put("z", -7);
            }
            else if (i == 43) {
                item.put("x", 115);
                item.put("y", 294);
                item.put("z", -8);
            }
            else if (i == 44) {
                item.put("x", 93);
                item.put("y", 296);
                item.put("z", -8);
            }
            else if (i == 45) {
                item.put("x", 69);
                item.put("y", 296);
                item.put("z", -7);
            }
            else if (i == 46) {
                item.put("x", 47);
                item.put("y", 298);
                item.put("z", -7);
            }
            else if (i == 47) {
                item.put("x", 27);
                item.put("y", 299);
                item.put("z", -6);
            }
            common_rack.put(i, item);
        }
        Log.e(LOG_TAG, String.valueOf(robot_rack));
        Log.e(LOG_TAG, String.valueOf(common_rack));
    }

    public void connectBT() {
        mBluetoothConnect.setConnectionHandler(this);

        mSharedPref = getSharedPreferences(mSharedPrefFile, MODE_PRIVATE);
        String bluetothDeviceAddr = mSharedPref.getString(BLUETOOT_REMOTE_DEVICE, null);
        if (bluetothDeviceAddr != null) {
            mBluetoothScan = new BluetoothScan(this);
            BluetoothDevice mBluetoothDevice = mBluetoothScan.getBluetoothDevice(bluetothDeviceAddr);
            mBluetoothConnect.connectBluetooth(mBluetoothDevice);
            if (D)
                Log.e(LOG_TAG, "onCreate() - connecting bluetooth device");
        } else {
            if (D)
                Log.e(LOG_TAG, "onCreate()");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);

        if (requestCode == REQUEST_BT_GET_DEVICE) {
            if (resultCode == RESULT_OK) {
                BluetoothDevice bluetoothDevice = resultIntent.getParcelableExtra(BluetoothDevListActivity.EXTRA_KEY_DEVICE);
                if (bluetoothDevice != null) {
                    mBluetoothConnect.connectBluetooth(bluetoothDevice);
                    if (D)
                        Log.e(LOG_TAG, "onActivityResult() - connecting");
                }
            } else if (resultCode == RESULT_CANCELED) {
                if (D)
                    Log.e(LOG_TAG, "onActivityResult() - canceled");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothConnect.disconnectBluetooth();
        if (D)
            Log.e(LOG_TAG, "onDestroy()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        mBluetoothConnect.disconnectBluetooth();
        if (D)
            Log.e(LOG_TAG, "onStop()");
    }

    public BluetoothConnect getBluetoothConnect() {
        return mBluetoothConnect;
    }

    public boolean connectedBluetooth(){
        return mBluetoothConnect.isConnected();
    }

    @Override
    public void onConnect(BluetoothConnect instant) {
        runOnUiThread(new Thread(() -> {
            Toast.makeText(getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT).show();
        }));
        if (D)
            Log.e(LOG_TAG, "onConnect() - Connecting");
    }

    @Override
    public void onConnectionSuccess(BluetoothConnect instant) {
        runOnUiThread(new Thread(() -> {
            Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_SHORT).show();
        }));
        SharedPreferences.Editor preferencesEditor = mSharedPref.edit();
        preferencesEditor.putString(BLUETOOT_REMOTE_DEVICE, mBluetoothConnect.getDeviceAddress());
        preferencesEditor.apply();
        if (D)
            Log.e(LOG_TAG, "onConnectionSuccess() - connected");
    }

    @Override
    public void onConnectionFail(BluetoothConnect instant) {
        removeSharePerf();
        if (D)
            Log.e(LOG_TAG, "onConnectionFail()");
    }

    @Override
    public void onDisconnected(BluetoothConnect instant) {

        mSharedPref = getSharedPreferences(mSharedPrefFile, MODE_PRIVATE);
        String bluetothDeviceAddr = mSharedPref.getString(BLUETOOT_REMOTE_DEVICE, null);
        if (bluetothDeviceAddr != null) {
            mBluetoothScan = new BluetoothScan(this);
            BluetoothDevice mBluetoothDevice = mBluetoothScan.getBluetoothDevice(bluetothDeviceAddr);
            mBluetoothConnect.connectBluetooth(mBluetoothDevice);
            if (D)
                Log.e(LOG_TAG, "onCreate() - connecting bluetooth device");
        } else {
            if (D)
                Log.e(LOG_TAG, "onCreate()");
        }
    }

    private void removeSharePerf() {
        SharedPreferences.Editor preferencesEditor = mSharedPref.edit();
        preferencesEditor.remove(BLUETOOT_REMOTE_DEVICE);
        preferencesEditor.apply();
    }

    public void connect_bt(String tag) {
        if (mBluetoothConnect.isConnected()) {
            mBluetoothConnect.disconnectBluetooth();
        }
        if (tag == "disconnect") {
            Log.e(LOG_TAG, "Press bt connect...");
            Intent intent = new Intent(this, BluetoothDevListActivity.class);
            startActivityForResult(intent, REQUEST_BT_GET_DEVICE);
        }
        else if (tag == "connect") {
            Log.e(LOG_TAG, "Press bt disconnect...");
            runOnUiThread(new Thread() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "Bluetooth disconnected", Toast.LENGTH_LONG).show();
                }
            });
        }
    }
    public String initTessTwo() {
        AssetManager assetManager = getAssets();

        Context context = this;
        String[] dataset = {"eng", "digits", "digits1", "digits_comma"};
        for (String data: dataset){
            File f = new File(getExternalFilesDir("/tessdata"), data + ".traineddata");

            try {
                f.createNewFile();
                InputStream in = assetManager.open(data + ".traineddata");
                OutputStream out = new FileOutputStream(new File(String.valueOf(f)));


                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();

                File DATA_FILE = context.getExternalFilesDir(null);
                DATA_PATH = String.valueOf(DATA_FILE);


            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return DATA_PATH;
    }

    public byte[] getPayload(String gcode) {
        try {
            return gcode.getBytes("iso8859-1");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public void setRobotRack(HashMap<Integer, HashMap<String, Object>> robot_rack) {
        this.robot_rack = robot_rack;
    }

    public void setRobotTile(int index, String key, Object value) {
        for (HashMap.Entry<Integer, HashMap<String, Object>> tile : this.robot_rack.entrySet()) {
            if (tile.getKey() == index){
                tile.getValue().put(key, value);
                break;
            }
        }
    }

    public HashMap<Integer, HashMap<String, Object>> getRobotRack() {
        return this.robot_rack;
    }
    public HashMap<Integer, HashMap<String, Integer>> getCommonRack() {
        return this.common_rack;
    }
    public void incTotalRack(){
        total_rack++;
    }

    public int getTotalRack(){
        return this.total_rack;
    }

    public void setWinner(String win){
        winner = win;
    }

    public String getWinner(){
        return this.winner;
    }

    public void setCmd(int c){
        cmd += c;
    }

    public void printArray(){
        Log.i("test", String.valueOf(robot_rack));
        Log.i("test", String.valueOf(common_rack));
    }
}