package com.fyp.rummikub;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.util.Collections;
import java.util.List;

public class CameraFragment extends Fragment implements CameraBridgeViewBase.CvCameraViewListener2 {
    static private String LOG_TAG = CameraFragment.class.getSimpleName();
    CameraBridgeViewBase cameraBridgeViewBase;
    Button next;
    TextView hint;

    public CameraFragment() {
    }
    public static CameraFragment newInstance() {
        CameraFragment fragment = new CameraFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(LOG_TAG, "onCreate()");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.e(LOG_TAG, "onCreateView()");
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        next = view.findViewById(R.id.next);
        hint = view.findViewById(R.id.hint);
        hint.setText("Put the phone in the frame");
        next.setText("Next");
        cameraBridgeViewBase = view.findViewById(R.id.cameraViewer);


        next.setOnClickListener(this::goNext);

        if (OpenCVLoader.initDebug()) {
            cameraBridgeViewBase.enableView();
        }
        cameraBridgeViewBase.setCameraIndex(0);
        cameraBridgeViewBase.setCameraPermissionGranted();
        cameraBridgeViewBase.setCvCameraViewListener(this);
        return view;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.d(LOG_TAG, "onCameraViewStarted()");
    }
    @Override
    public void onCameraViewStopped() {
        Log.d(LOG_TAG, "onCameraViewStopped()");
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Log.d(LOG_TAG, "onCameraFrame()");
        return inputFrame.rgba();
    }

    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(cameraBridgeViewBase);
    }

    public void goNext(View view) {
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }
        Log.d(LOG_TAG, "Press goNext...");

        StartHandFragment StartHandFragment = new StartHandFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in, R.anim.slide_out, R.anim.enter_top, R.anim.exit_bottom);
        transaction.addToBackStack(null);
        transaction.replace(R.id.frag_con, StartHandFragment).commit();
    }
}