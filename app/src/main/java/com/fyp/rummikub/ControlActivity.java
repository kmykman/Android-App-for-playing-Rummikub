package com.fyp.rummikub;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.makerlab.bt.BluetoothConnect;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import com.makerlab.bt.BluetoothScan;
import com.makerlab.ui.BluetoothDevListActivity;

public class ControlActivity extends AppCompatActivity implements BluetoothConnect.ConnectionHandler {
    static public final boolean D = BuildConfig.DEBUG;
    static public final int REQUEST_BT_GET_DEVICE = 1112;
    static public final String BLUETOOT_REMOTE_DEVICE = "bt_remote_device";
    private Timer mDataSendTimer = null, mReadTimer = null;
    private Thread readThread = null;
    private boolean ack = true;
    static private String LOG_TAG = ControlActivity.class.getSimpleName();
    private Queue<String> command = new LinkedList<>();
    private byte[] current_command = null;
    long startTime = 0, currentTime = 0;
    private int cmd_no = 0;
    private String pck_no = "";
    private Queue<byte[]> mQueue = new LinkedList<>();
    private BluetoothDevice mBluetoothDevice;
    private BluetoothConnect mBluetoothConnect;
    private BluetoothScan mBluetoothScan;
    private SharedPreferences mSharedPref;
    private String mSharedPrefFile = "com.fyp.rummikub.sharedprefs";
    private String message_list = "";
    private int x = 0, y = 215, z = 180;
    private int rotator_angle = 0;
    ImageView bt;
    TextView BTdevice;
    Button up_button, down_button, left_button, right_button, home_button, front_button, back_button, gripper_on_button, gripper_off_button, rotateCW_button, rotateCCW_button, getCard_button;
    String [] control_button = {"upButton", "downButton", "leftButton", "rightButton", "homeButton", "frontButton", "backButton", "gripperOpenButton", "gripperCloseButton", "rotateCWButton", "rotateCCWButton", "getCardButton"};

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);
        mBluetoothConnect = new BluetoothConnect(this);
        mBluetoothConnect.setConnectionHandler(this);
        bt = findViewById(R.id.bt);
        bt.setTag("disconnect");
        BTdevice = findViewById(R.id.BTdevice);

        mSharedPref = getSharedPreferences(mSharedPrefFile, MODE_PRIVATE);
        String bluetothDeviceAddr = mSharedPref.getString(BLUETOOT_REMOTE_DEVICE, null);
        if (bluetothDeviceAddr != null) {
            Log.e(LOG_TAG, "onCreate(): found share perference");
            mBluetoothScan = new BluetoothScan(this);
            mBluetoothDevice = mBluetoothScan.getBluetoothDevice(bluetothDeviceAddr);
            mBluetoothConnect.connectBluetooth(mBluetoothDevice);
            if (D)
                Log.e(LOG_TAG, "onCreate() - connecting bluetooth device");
        } else {
            if (D)
                Log.e(LOG_TAG, "onCreate()");
        }

        up_button = findViewById(R.id.upButton);
        down_button = findViewById(R.id.downButton);
        left_button = findViewById(R.id.leftButton);
        right_button = findViewById(R.id.rightButton);
        home_button = findViewById(R.id.homeButton);
        front_button =  findViewById(R.id.frontButton);
        back_button =  findViewById(R.id.backButton);
        gripper_on_button = findViewById(R.id.gripperOpenButton);
        gripper_off_button = findViewById(R.id.gripperCloseButton);
        rotateCW_button = findViewById(R.id.rotateCWButton);
        rotateCCW_button = findViewById(R.id.rotateCCWButton);
        getCard_button = findViewById(R.id.getCardButton);

        up_button.setOnClickListener(actionListener);
        down_button.setOnClickListener(actionListener);
        left_button.setOnClickListener(actionListener);
        right_button.setOnClickListener(actionListener);
        home_button.setOnClickListener(actionListener);
        front_button.setOnClickListener(actionListener);
        back_button.setOnClickListener(actionListener);
        gripper_on_button.setOnClickListener(actionListener);
        gripper_off_button.setOnClickListener(actionListener);
        rotateCW_button.setOnClickListener(actionListener);
        rotateCCW_button.setOnClickListener(actionListener);
        getCard_button.setOnClickListener(actionListener);

        readThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                String message = null;
                try {
                    Log.e(LOG_TAG, "before");
                    message = mBluetoothConnect.read();
                    Log.e(LOG_TAG, "after");
                } catch (IOException e) {
                    Log.e(LOG_TAG, "error");
                    Log.e(LOG_TAG, String.valueOf(e));
                }
                if (message != null) {
                    Log.e(LOG_TAG, message);
                }
                message_list += message;
            }
        });
    }

    private void SetButton(boolean flag) {
        runOnUiThread(new Thread() {
            public void run() {
                runOnUiThread(new Thread() {
                    public void run() {
                        if (!flag) {
                            bt.setImageResource(R.drawable.bluetooth_disabled);

                            for (String name: control_button) {
                                int id = getResources().getIdentifier(name, "id", getPackageName());
                                Button button = findViewById(id);
                                button.setEnabled(true);
                                button.setBackgroundResource(R.drawable.rounded_button_control);
                            }

                            if (mBluetoothConnect.getDeviceName() != null) {
                                BTdevice.setText("Connected to " + mBluetoothConnect.getDeviceName());
                            }
                            else if (mBluetoothConnect.getDeviceName() == null && mBluetoothConnect.getDeviceAddress() != null) {
                                BTdevice.setText("Connected to " + mBluetoothConnect.getDeviceAddress());
                            }
                            else {
                                BTdevice.setText("Connected");
                            }
                            BTdevice.setTextColor(Color.parseColor("#000000"));
                            bt.setTag("connect");
                        }
                        else{
                            bt.setImageResource(R.drawable.bluetooth);

                            for (String name: control_button) {
                                int id = getResources().getIdentifier(name, "id", getPackageName());
                                Button button = findViewById(id);
                                button.setEnabled(false);
                                button.setBackgroundResource(R.drawable.rounded_button_grey_control);
                            }

                            BTdevice.setText("Not connected");
                            BTdevice.setTextColor(Color.parseColor("#FA0000"));
                            bt.setTag("disconnect");
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        SetButton(!mBluetoothConnect.isConnected());
        mDataSendTimer = new Timer();
        mDataSendTimer.scheduleAtFixedRate(new DataSendTimerTask(), 1000, 250);
        mReadTimer = new Timer();
        mReadTimer.scheduleAtFixedRate(new ReadTimerTask(), 0, 250);
        if (D)
            Log.e(LOG_TAG, "onStart()");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mDataSendTimer != null) {
            mDataSendTimer.cancel();
        }
        if (mReadTimer != null) {
            mReadTimer.cancel();
        }
        if (readThread.isAlive()){
            readThread.interrupt();
        }
        if (D)
            Log.e(LOG_TAG, "onStop()");
    }

    public void runbt(View view) {
        if (mBluetoothConnect.isConnected()) {
            mBluetoothConnect.disconnectBluetooth();
        }
        if (bt.getTag() == "disconnect") {
            Log.e(LOG_TAG, "Press bt connect...");
            Intent intent = new Intent(this, BluetoothDevListActivity.class);
            startActivityForResult(intent, REQUEST_BT_GET_DEVICE);
        }
        else if (bt.getTag() == "connect") {
            Log.e(LOG_TAG, "Press bt disconnect...");
            SetButton(true);
        }
    }

    public void goHomePage(View view) {
        finish();
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
        if (mDataSendTimer != null) {
            mDataSendTimer.cancel();
        }
        if (mReadTimer != null) {
            mReadTimer.cancel();
        }
        if (readThread.isAlive()){
            readThread.interrupt();
        }
        mBluetoothConnect.disconnectBluetooth();
        if (D)
            Log.e(LOG_TAG, "onDestroy()");
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
        SharedPreferences.Editor preferencesEditor = mSharedPref.edit();
        runOnUiThread(new Thread(() -> {
            Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_SHORT).show();
        }));
        preferencesEditor.putString(BLUETOOT_REMOTE_DEVICE, mBluetoothConnect.getDeviceAddress());
        preferencesEditor.apply();
        SetButton(false);
        if (D)
            Log.e(LOG_TAG, "onConnectionSuccess() - connected");

        if (readThread.isInterrupted() || !readThread.isAlive()) {
            readThread = new Thread();
            readThread.start();
        }
    }

    @Override
    public void onConnectionFail(BluetoothConnect instant) {
        removeSharePerf();
        if (D)
            Log.e(LOG_TAG, "onConnectionFail()");
        SetButton(true);
    }

    @Override
    public void onDisconnected(BluetoothConnect instant) {
        SetButton(true);
        if (readThread.isAlive() || !readThread.isInterrupted()) {
            readThread.interrupt();
        }
        if (D)
            Log.e(LOG_TAG, "onDisconnected()");
    }

    private void removeSharePerf() {
        SharedPreferences.Editor preferencesEditor = mSharedPref.edit();
        preferencesEditor.remove(BLUETOOT_REMOTE_DEVICE);
        preferencesEditor.apply();
    }

    private View.OnClickListener actionListener = v -> {
        synchronized (mQueue) {
            if (v.getId() == R.id.homeButton) {
                x = 0;
                y = 215;
                z = 180;
                rotator_angle = 0;
                mQueue.add(getPayload("G28" + " P"+ cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no--;
            }

            else if (v.getId() == R.id.upButton) {
                    z += 10;
                    mQueue.add(getPayload("G1 X" + x + " Y" + y + " Z" + z + " P"+ cmd_no + "\r\n"));
                    command.add(String.valueOf(cmd_no));
                    cmd_no--;
            }

            else if (v.getId() == R.id.downButton) {
                    z -= 10;
                    mQueue.add(getPayload("G1 X" + x + " Y" + y + " Z" + z + " P"+ cmd_no + "\r\n"));
                    command.add(String.valueOf(cmd_no));
                    cmd_no--;
            }

            else if (v.getId() == R.id.leftButton) {
                    y -= 10;
                    mQueue.add(getPayload("G1 X" + x + " Y" + y + " Z" + z + " P"+ cmd_no + "\r\n"));
                    command.add(String.valueOf(cmd_no));
                    cmd_no--;
            }

            else if (v.getId() == R.id.rightButton) {
                    y += 10;
                    mQueue.add(getPayload("G1 X" + x + " Y" + y + " Z" + z + " P"+ cmd_no + "\r\n"));
                    command.add(String.valueOf(cmd_no));
                    cmd_no--;
            }

            else if (v.getId() == R.id.frontButton) {
                    x -= 10;
                    mQueue.add(getPayload("G1 X" + x + " Y" + y + " Z" + z + " P"+ cmd_no + "\r\n"));
                    command.add(String.valueOf(cmd_no));
                    cmd_no--;
            }

            else if (v.getId() == R.id.backButton) {
                    x += 10;
                    mQueue.add(getPayload("G1 X" + x + " Y" + y + " Z" + z + " P"+ cmd_no + "\r\n"));
                    command.add(String.valueOf(cmd_no));
                    cmd_no--;
            }

            else if (v.getId() == R.id.gripperOpenButton) {
                mQueue.add(getPayload("M5 T20"+ " P"+ cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no--;
            }

            else if (v.getId() == R.id.gripperCloseButton) {
                mQueue.add(getPayload("M3"+ " P"+ cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no--;
            }

            else if (v.getId() == R.id.rotateCWButton) {
                Log.e(LOG_TAG, String.valueOf(rotator_angle));
                if (rotator_angle + 10 <= 90) {
                    rotator_angle += 10;
                    mQueue.add(getPayload("M200 T" + rotator_angle + " P"+ cmd_no + "\r\n"));
                    command.add(String.valueOf(cmd_no));
                    cmd_no--;
                }
            }

            else if (v.getId() == R.id.rotateCCWButton) {
                Log.e(LOG_TAG, String.valueOf(rotator_angle));
                if (rotator_angle - 10 >= -90) {
                    rotator_angle -= 10;
                    Log.e(LOG_TAG, "sent" + "M200 T" + rotator_angle);
                    mQueue.add(getPayload("M200 T" + rotator_angle + " P"+ cmd_no + "\r\n"));
                    command.add(String.valueOf(cmd_no));
                    cmd_no--;
                }
            }

            else if (v.getId() == R.id.getCardButton) {
                mQueue.add(getPayload("M202"+ " P"+ cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no--;
            }
        }
    };

    private byte[] getPayload(String gcode) {
        try {
            return gcode.getBytes("iso8859-1");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    class ReadTimerTask extends TimerTask {
        @Override
        public void run() {
            String message = null;
            try {
                Log.e(LOG_TAG, "before");
                message = mBluetoothConnect.read();
                Log.e(LOG_TAG, "after");
            } catch (IOException e) {
                Log.e(LOG_TAG, "error");
                Log.e(LOG_TAG, String.valueOf(e));
            }
            if (message != null) {
                Log.e(LOG_TAG, message);
            }
            message_list += message;
        }
    }

    class DataSendTimerTask extends TimerTask {
        @Override
        public void run() {
            if (!mQueue.isEmpty()) {
                Log.e(LOG_TAG, "sent" + mQueue);
                Log.e(LOG_TAG, String.valueOf(ack));
            }

            if (!mBluetoothConnect.isConnected()) {
                return;
            }

            if (!ack){

                if (message_list.contains("A") && message_list.contains(pck_no)){
                    ack = true;
                    command.remove();
                    message_list = "";
                    Log.i("testing", "message recevied: A" + pck_no);
                    pck_no = "";
                    Arrays.fill(current_command, (byte)0);
                }
                if (message_list.contains("F")){
                    message_list = "";
                    mBluetoothConnect.send(current_command);
                    startTime = System.currentTimeMillis();
                }
                currentTime = System.currentTimeMillis();
                if (currentTime - startTime > 700){
                    message_list = "";
                    mBluetoothConnect.send(current_command);
                    startTime = System.currentTimeMillis();
                    Log.e("testing", "resend packet number: " + pck_no);
                }
            }

            synchronized (mQueue) {
                if (ack) {
                    if (!mQueue.isEmpty()) {
                        current_command = mQueue.peek();
                        mBluetoothConnect.send(mQueue.remove());
                        ack = false;
                        pck_no = command.peek();
                        startTime = System.currentTimeMillis();
                        Log.e(LOG_TAG, String.valueOf(command));
                        Log.e(LOG_TAG, "DataSendTimerTask.run() - send");
                        Log.e("testing", "send packet number: " + pck_no);
                    }
                }
            }
        }
    }
}