package com.fyp.rummikub;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import java.util.Timer;
import java.util.TimerTask;

public class ConnectBluetoothFragment extends Fragment {
    static private String LOG_TAG = ConnectBluetoothFragment.class.getSimpleName();
    View view;
    Button next;
    ImageView bt, home_page;
    private Timer EnableTimer = null;

    public ConnectBluetoothFragment() {
    }

    public static ConnectBluetoothFragment newInstance() {
        ConnectBluetoothFragment fragment = new ConnectBluetoothFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_connect_bluetooth, container, false);
        next = view.findViewById(R.id.next);
        home_page = view.findViewById(R.id.home_page);
        bt = view.findViewById(R.id.bt);

        if (((GameActivity)getActivity()).connectedBluetooth()){
            SetButton(false);
        }
        else {
            SetButton(true);
        }
        SetButton(true);
        bt.setOnClickListener(this::runbt);
        home_page.setOnClickListener(this::goHome);
        next.setOnClickListener(this::goPrepActivity);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        EnableTimer = new Timer();
        EnableTimer.scheduleAtFixedRate(new EnableTimerTask(), 1000, 250);
    }

    class EnableTimerTask extends TimerTask {
        @Override
        public void run() {
            if (!((GameActivity)getActivity()).connectedBluetooth()){
                ((GameActivity)getActivity()).runOnUiThread(() -> SetButton(true));
            }
            else {
                ((GameActivity)getActivity()).runOnUiThread(() -> SetButton(false));
            }
        }
    }

    private void SetButton(boolean flag) {
        if (!flag) {
            bt.setImageResource(R.drawable.bluetooth_disabled);
            next.setEnabled(true);
            next.setBackgroundResource(R.drawable.rounded_button);
            bt.setTag("connect");
        }
        else{
            bt.setImageResource(R.drawable.bluetooth);
            next.setEnabled(false);
            next.setBackgroundResource(R.drawable.rounded_button_grey);
            bt.setTag("disconnect");
        }
    }

    public void runbt(View view) {
        ((GameActivity)getActivity()).connect_bt((String) bt.getTag());
        if (bt.getTag() == "connect") {
            SetButton(true);
        }
    }

    public void goHome(View view) {
        getActivity().finish();
    }

    public void goPrepActivity(View view) {
        PrepFragment PrepFragment = new PrepFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_left_in, R.anim.slide_left_out, R.anim.enter_left, R.anim.exit_right);
        transaction.addToBackStack(null);
        transaction.replace(R.id.frag_con, PrepFragment).commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i(LOG_TAG, "onDestroyView()");
        if (EnableTimer != null) {
            EnableTimer.cancel();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(LOG_TAG, "onStop()");
        if (EnableTimer != null) {
            EnableTimer.cancel();
        }
    }
}