package com.fyp.rummikub;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class PrepFragment extends Fragment {
    static private String LOG_TAG = PrepFragment.class.getSimpleName();
    View view;
    Button next;

    public PrepFragment() {}

    public static PrepFragment newInstance() {
        PrepFragment fragment = new PrepFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_prep, container, false);
        next = view.findViewById(R.id.next);
        next.setOnClickListener(this::goCameraActivity);
        return view;
    }

    public void goCameraActivity(View view) {
        CameraFragment CameraFragment = new CameraFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_left_in, R.anim.slide_left_out, R.anim.enter_left, R.anim.exit_right);
        transaction.addToBackStack(null);
        transaction.replace(R.id.frag_con, CameraFragment).commit();
    }
}