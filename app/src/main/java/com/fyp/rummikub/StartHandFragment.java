package com.fyp.rummikub;

import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY_INV;
import static org.opencv.imgproc.Imgproc.THRESH_OTSU;

import static java.lang.Math.abs;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.makerlab.bt.BluetoothConnect;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class StartHandFragment extends Fragment implements CameraBridgeViewBase.CvCameraViewListener2 {
    static private String LOG_TAG = StartHandFragment.class.getSimpleName();
    View view;
    Button next;
    ProgressBar progressBar;
    TextView hint, prep;
    ImageView tick;
    GameActivity activity;
    static public final boolean D = BuildConfig.DEBUG;
    private Timer mDataSendTimer = null, mReadTimer = null;
    private Thread thread = null;
    private boolean ack = true;
    private Queue<String> command = new LinkedList<>();
    private byte[] current_command = null;
    long startTime = 0, currentTime = 0;
    private String pck_no = "";
    private Queue<byte[]> mQueue = new LinkedList<>();
    private BluetoothConnect mBluetoothConnect;
    private String message_list = "";
    private int cmd_no = 1;
    boolean view_prepared = true, grab_prepare = true, getPosition = true;
    private String DATA_PATH;
    Bitmap hsv_input;
    List<Point[]> points_list = new ArrayList<>();
    private int image_row = 0, image_col = 0;
    private int maxNumTile = 0;
    private int tmpNumTile = 0;
    CameraBridgeViewBase cameraBridgeViewBase;
    Mat gray, hsv, hsvIMG, hsv_mask, hsv_mask_clone1, hsv_mask_clone2, hsv_mask_clone3, red_clone1, red_clone2, red_clone3, blue_clone1, blue_clone2, blue_clone3, black_clone1, black_clone2, black_clone3, blue1, blue, red, red1, red2, red3, red4, black1, black2, blur_gray, roi, blur, mask, hierarchy, hierarchy_hsv, hierarchy_blue, hierarchy_red, hierarchy_black;
    MatOfPoint2f approxCurve;
    TessBaseAPI tess = new TessBaseAPI();
    private List<MatOfPoint> contours = new ArrayList<>();
    private List<MatOfPoint> contours_hsv = new ArrayList<>();
    private List<MatOfPoint> contours_blue = new ArrayList<>();
    private List<MatOfPoint> contours_red = new ArrayList<>();
    private List<MatOfPoint> contours_black = new ArrayList<>();

    public StartHandFragment() {
    }

    public static StartHandFragment newInstance() {
        StartHandFragment fragment = new StartHandFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.e(LOG_TAG, "onCreateView()");
        view = inflater.inflate(R.layout.fragment_start_hand, container, false);
        progressBar = view.findViewById(R.id.progressBar);
        hint = view.findViewById(R.id.hint);
        prep = view.findViewById(R.id.prep);
        next = view.findViewById(R.id.next);
        tick = view.findViewById(R.id.ok_tick);
        cameraBridgeViewBase = view.findViewById(R.id.cameraViewer);

        next.setOnClickListener(this::goStartPage);

        Log.e(LOG_TAG, String.valueOf(view_prepared));

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (D)
            Log.e(LOG_TAG, "onStart()");

        activity = (GameActivity) getActivity();

        for (HashMap.Entry<Integer, HashMap<String, Object>> tile : activity.getRobotRack().entrySet()) {
            if (tile.getKey() == 10){
                break;
            }
            if (!(Boolean) tile.getValue().get("placed")) {
                grab_prepare = false;
            }
            if (tile.getValue().get("tile") == "E") {
                view_prepared = false;
            }
            if (!grab_prepare && !view_prepared){
                break;
            }
        }


        thread = new Thread(() -> {
            while (!grab_prepare) {
                activity.runOnUiThread(() -> {
                    next.setEnabled(false);
                    next.setBackgroundResource(R.drawable.rounded_button_grey);
                    progressBar.setVisibility(View.VISIBLE);
                    hint.setVisibility(View.VISIBLE);
                    hint.setText("Wait for robotic arm to get starting hand...");
                    prep.setVisibility(View.INVISIBLE);
                    tick.setVisibility(View.INVISIBLE);
                });

                mBluetoothConnect = activity.getBluetoothConnect();

                mDataSendTimer = new Timer();
                mDataSendTimer.scheduleAtFixedRate(new DataSendTimerTask(), 1000, 250);
                mReadTimer = new Timer();
                mReadTimer.scheduleAtFixedRate(new ReadTimerTask(), 0, 250);
                grabTile();

                while (command.size() > 0) {}

                grab_prepare = true;
                for (HashMap.Entry<Integer, HashMap<String, Object>> tile : activity.getRobotRack().entrySet()) {
                    if (tile.getKey() == 10){
                        break;
                    }
                    if (!(Boolean) tile.getValue().get("placed")) {
                        grab_prepare = false;
                    }
                }
            }

            activity.runOnUiThread(() -> {
                prep.setVisibility(View.INVISIBLE);
                tick.setVisibility(View.INVISIBLE);
                hint.setText("Wait for robotic arm to view tiles...");
            });

            if (mDataSendTimer != null) {
                mDataSendTimer.cancel();
            }
            if (mReadTimer != null) {
                mReadTimer.cancel();
            }

            startTime = System.currentTimeMillis();
        });

        if (!grab_prepare || !view_prepared) {
            thread.start();
        }


        if (!view_prepared) {
            next.setEnabled(false);
            next.setBackgroundResource(R.drawable.rounded_button_grey);
            progressBar.setVisibility(View.VISIBLE);
            prep.setVisibility(View.INVISIBLE);
            tick.setVisibility(View.INVISIBLE);
            if (OpenCVLoader.initDebug()) {
                cameraBridgeViewBase.enableView();
                Log.e(LOG_TAG, "Load OpenCV success");
            } else {
                Log.e(LOG_TAG, "Load OpenCV error");
            }
            cameraBridgeViewBase.setCameraIndex(0);
            cameraBridgeViewBase.setAlpha(0);
            cameraBridgeViewBase.setCameraPermissionGranted();
            cameraBridgeViewBase.setCvCameraViewListener(this);
        }
    }

    private void grabTile(){
        mQueue.add(activity.getPayload("M3 P" + cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        Log.i(LOG_TAG, String.valueOf(cmd_no));
        cmd_no++;
        mQueue.add(activity.getPayload("M202 P"+ cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        Log.i(LOG_TAG, String.valueOf(cmd_no));
        cmd_no++;
        mQueue.add(activity.getPayload("G28 P"+ cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        Log.i(LOG_TAG, String.valueOf(cmd_no));
        cmd_no++;
        mQueue.add(activity.getPayload("G1 X0 Y105 Z160 P" + cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        Log.i(LOG_TAG, String.valueOf(cmd_no));
        cmd_no++;
        mQueue.add(activity.getPayload("G1 X-50 Y105 Z160 P" + cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        Log.i(LOG_TAG, String.valueOf(cmd_no));
        cmd_no++;
        mQueue.add(activity.getPayload("G1 X-100 Y190 Z180 P" + cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        Log.i(LOG_TAG, String.valueOf(cmd_no));
        cmd_no++;
        mQueue.add(activity.getPayload("M5 T20 P" + cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        Log.i(LOG_TAG, String.valueOf(cmd_no));
        cmd_no++;


        for (HashMap.Entry<Integer, HashMap<String, Object>> tile : activity.getRobotRack().entrySet()) {
            if (tile.getKey() == 10){
                break;
            }
            if (!(Boolean) tile.getValue().get("placed")) {
                mQueue.add(activity.getPayload("M202 P"+ cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;
                mQueue.add(activity.getPayload("M1 X-139 Y283 Z20 T-60 P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;
                mQueue.add(activity.getPayload("G1 X-139 Y283 Z-23 P" + cmd_no + "\r\n"));

                command.add(String.valueOf(cmd_no));
                cmd_no++;
                mQueue.add(activity.getPayload("M3 P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;
                mQueue.add(activity.getPayload("G1 X-139 Y283 Z20 P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;


                mQueue.add(activity.getPayload("G1 X-100 Y190 Z180 P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;

                int x = (int) tile.getValue().get("x");
                int y = (int) tile.getValue().get("y");
                int z = (int) tile.getValue().get("z");

                int up_z = z + 15;

                mQueue.add(activity.getPayload("M1 X" + x + " Y" + y + " Z" + up_z + " P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;
                Log.i(LOG_TAG, "G1 X" + x + " Y" + y + " Z" + up_z + " P" + cmd_no);

                mQueue.add(activity.getPayload("G1 X" + x + " Y" + y + " Z" + z + " P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;
                mQueue.add(activity.getPayload("M5 T20 P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;
                Log.i(LOG_TAG, "G1 X" + x + " Y" + y + " Z" + z + " P" + cmd_no);

                mQueue.add(activity.getPayload("G1 X" + x + " Y" + y + " Z" + up_z + " P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;

                mQueue.add(activity.getPayload("G1 X-100 Y190 Z180 P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;
            }
            activity.setRobotTile(tile.getKey(), "placed", true);
        }
        mQueue.add(activity.getPayload("M200 T0 P" + cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        cmd_no++;
        mQueue.add(activity.getPayload("G1 X-50 Y90 Z160 P" + cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        cmd_no++;
        mQueue.add(activity.getPayload("G1 X0 Y90 Z160 P" + cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        cmd_no++;
        mQueue.add(activity.getPayload("G1 X0 Y215 Z180 P" + cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        cmd_no++;
        mQueue.add(activity.getPayload("M200 T0 P" + cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        cmd_no++;

        activity.setCmd(cmd_no);
    }

    class ReadTimerTask extends TimerTask {
        @Override
        public void run() {
            String message = null;
            try {
                message = mBluetoothConnect.read();
            } catch (IOException e) {
                Log.e(LOG_TAG, "error in ReadTimerTask");
                Log.e(LOG_TAG, String.valueOf(e));
            }
            message_list += message;
        }
    }

    class DataSendTimerTask extends TimerTask {
        @Override
        public void run() {
            if (!mBluetoothConnect.isConnected()) {
                activity.runOnUiThread(() -> Toast.makeText(getActivity(), "Reconnecting...", Toast.LENGTH_SHORT).show());
                activity.connectBT();
                return;
            }

            if (!ack){
                Log.e(LOG_TAG, "message   " + message_list);
                Log.e(LOG_TAG, "pck_no   " + pck_no);
                if (message_list.contains("A") && message_list.contains(pck_no)){
                    ack = true;
                    command.remove();
                    message_list = "";
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
                    }
                }
            }
        }
    }

    private void prepDone(){
        activity.runOnUiThread(() -> {
            next.setEnabled(true);
            next.setBackgroundResource(R.drawable.rounded_button);
            progressBar.setVisibility(View.INVISIBLE);
            hint.setVisibility(View.INVISIBLE);
            prep.setVisibility(View.VISIBLE);
            tick.setVisibility(View.VISIBLE);
        });
        view_prepared = true;
        if (mDataSendTimer != null) {
            mDataSendTimer.cancel();
        }
        if (mReadTimer != null) {
            mReadTimer.cancel();
        }
        if (thread.isAlive() || !thread.isInterrupted()) {
            thread.interrupt();
        }

    }

    private void goStartPage(View view) {
        grab_prepare = true;
        view_prepared = true;

        StartPageFragment StartPageFragment = new StartPageFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in, R.anim.slide_out, R.anim.enter_top, R.anim.exit_bottom);
        transaction.addToBackStack(null);
        transaction.replace(R.id.frag_con, StartPageFragment).commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mDataSendTimer != null) {
            mDataSendTimer.cancel();
        }
        if (mReadTimer != null) {
            mReadTimer.cancel();
        }
        if (thread.isAlive() || !thread.isInterrupted()) {
            thread.interrupt();
        }
        if (D)
            Log.e(LOG_TAG, "onDestroy()");
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
        if (thread.isAlive() || !thread.isInterrupted()) {
            thread.interrupt();
        }
        if (D)
            Log.e(LOG_TAG, "onStop()");
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        gray = new Mat();
        hsv = new Mat();
        hsvIMG = new Mat();
        hsv_mask = new Mat();
        hsv_mask_clone1 = new Mat();
        hsv_mask_clone2 = new Mat();
        hsv_mask_clone3 = new Mat();
        red_clone1 = new Mat();
        red_clone2 = new Mat();
        red_clone3 = new Mat();
        blue_clone1 = new Mat();
        blue_clone2 = new Mat();
        blue_clone3 = new Mat();
        black_clone1 = new Mat();
        black_clone2 = new Mat();
        black_clone3 = new Mat();
        blue1 = new Mat();
        blue = new Mat();
        red = new Mat();
        red1 = new Mat();
        red2 = new Mat();
        red3 = new Mat();
        red4 = new Mat();
        black1 = new Mat();
        black2 = new Mat();
        blur_gray = new Mat();
        roi = new Mat();
        blur = new Mat();
        approxCurve = new MatOfPoint2f();
        hierarchy = new Mat();
        hierarchy_hsv = new Mat();
        hierarchy_blue = new Mat();
        hierarchy_red = new Mat();
        hierarchy_black = new Mat();

        startTime = System.currentTimeMillis();
    }

    @Override
    public void onCameraViewStopped() {
        gray.release();
        hsv.release();
        hsvIMG.release();
        hsv_mask.release();
        blue1.release();
        blue.release();
        red.release();
        red1.release();
        red2.release();
        red3.release();
        red4.release();
        black1.release();
        black2.release();
        blur_gray.release();
        roi.release();
        approxCurve.release();
        blur.release();
        hierarchy.release();
        hierarchy_hsv.release();
        hierarchy_blue.release();
        hierarchy_red.release();
        hierarchy_black.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat image = inputFrame.rgba();

        if (grab_prepare && !view_prepared && getPosition) {
            currentTime = System.currentTimeMillis();
            if (currentTime - startTime > 500 && currentTime - startTime < 1300) {
                hsv_mask_clone1 = hsvIMG.clone();
                red_clone1 = hsvIMG.clone();
                blue_clone1 = hsvIMG.clone();
                black_clone1 = hsvIMG.clone();

            }
            if (currentTime - startTime > 1300 && currentTime - startTime < 2000) {
                hsv_mask_clone2 = hsvIMG.clone();
                red_clone2 = hsvIMG.clone();
                blue_clone2 = hsvIMG.clone();
                black_clone2 = hsvIMG.clone();
            }
            if (currentTime - startTime > 2000 && tmpNumTile >= maxNumTile) {
                Size size = image.size();
                hsv_mask_clone3 = hsvIMG.clone();
                red_clone3 = hsvIMG.clone();
                blue_clone3 = hsvIMG.clone();
                black_clone3 = hsvIMG.clone();
                hsv_input = Bitmap.createBitmap((int) size.width, (int) size.height, Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(hsvIMG, hsv_input);
                getPosition = false;

                getContent();

            }


            image_row = image.rows();
            image_col = image.cols();

            Imgproc.cvtColor(image, gray, Imgproc.COLOR_RGBA2GRAY);
            Imgproc.adaptiveThreshold(gray, hsvIMG, 255, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY_INV, 13, 1);

            Imgproc.GaussianBlur(image, blur, new Size(7, 7), 7);
            Imgproc.cvtColor(blur, blur_gray, Imgproc.COLOR_RGBA2GRAY);

            Imgproc.threshold(blur_gray,blur_gray, 0, 255, THRESH_BINARY + THRESH_OTSU);

            List<MatOfPoint> list = new ArrayList<>();

            list.add(new MatOfPoint (new Point(800,150), new Point(620,150), new Point(710,570), new Point(900,550)));

            mask = new Mat(image.rows(), image.cols(), CV_8UC3, new Scalar(255, 255, 255));
            Imgproc.fillPoly(mask, list, new Scalar(0, 0, 0), Imgproc.LINE_8);
            Core.inRange(mask, new Scalar(0, 0, 0),new Scalar(180, 255, 130), roi);


            Core.bitwise_and(roi, blur_gray, blur_gray);


            Imgproc.cvtColor(image, hsv, Imgproc.COLOR_RGB2HSV);


            Core.inRange(hsv, new Scalar(0, 100, 100),
                    new Scalar(10, 255, 255), red1);  // red
            Core.inRange(hsv, new Scalar(0, 50, 100),

                    new Scalar(10, 200, 255), red2);  // red
            Core.inRange(hsv, new Scalar(90, 10, 100),
                    new Scalar(150, 200, 255), blue);  // blue

            Core.inRange(hsv, new Scalar(0, 0, 0),
                    new Scalar(180, 255, 180), black1);  // black, word

            Core.inRange(hsv, new Scalar(0, 0, 0),
                    new Scalar(180, 255, 140), black2);  // black, area


            Core.bitwise_or(red1, red2, red);

            Core.bitwise_not(black1, black1);
            Core.bitwise_and(black1,blur_gray,blur_gray);


            List<MatOfPoint> points = new ArrayList<>();

            contours.clear();
            contours_hsv.clear();
            contours_blue.clear();
            contours_red.clear();
            contours_black.clear();
            Imgproc.findContours(blur_gray, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
            Imgproc.findContours(blue, contours_blue, hierarchy_blue, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
            Imgproc.findContours(red, contours_red, hierarchy_red, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
            Imgproc.findContours(black2, contours_black, hierarchy_black, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);


            points_list.clear();

            for (MatOfPoint cnt : contours) {
                MatOfPoint2f curve = new MatOfPoint2f(cnt.toArray());
                Imgproc.approxPolyDP(curve, approxCurve, 0.02 * Imgproc.arcLength(curve, true), true);
                int numberVertices = (int) approxCurve.total();
                if (numberVertices > 3 && Imgproc.contourArea(cnt) > 1000) {
                    RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(cnt.toArray()));
                    Point[] vertices = new Point[4];
                    Point[] individual_vertices = new Point[4];
                    rect.points(vertices);


                    Point[] tmp_near_vertices = {new Point(0, 0), new Point(0, 0)};   // find near 2 point relative to y-axis
                    Point[] tmp_far_vertices = {new Point(0, 0), new Point(0, 0)};   // find far 2 point relative to y-axis


                    for (Point v : vertices) {
                        if (Objects.equals(tmp_near_vertices[0], new Point(0, 0))) {
                            tmp_near_vertices[0] = v;
                        } else if (!Objects.equals(tmp_near_vertices[0], new Point(0, 0)) && Objects.equals(tmp_near_vertices[1], new Point(0, 0))) {
                            tmp_near_vertices[1] = v;
                        } else {
                            for (int i = 0; i < 2; i++) {
                                if (v.x < tmp_near_vertices[i].x) {
                                    tmp_near_vertices[i] = v;
                                    break;
                                }
                            }
                        }

                        if (Objects.equals(tmp_far_vertices[0], new Point(0, 0))) {
                            tmp_far_vertices[0] = v;
                        } else if (!Objects.equals(tmp_far_vertices[0], new Point(0, 0)) && Objects.equals(tmp_far_vertices[1], new Point(0, 0))) {
                            tmp_far_vertices[1] = v;
                        } else {
                            for (int i = 0; i < 2; i++) {
                                if (v.x > tmp_far_vertices[i].x) {
                                    tmp_far_vertices[i] = v;
                                    break;
                                }
                            }
                        }
                    }

                    if (tmp_near_vertices[0].y < tmp_near_vertices[1].y) {
                        individual_vertices[0] = tmp_near_vertices[0];
                        individual_vertices[3] = tmp_near_vertices[1];
                    } else {
                        individual_vertices[3] = tmp_near_vertices[0];
                        individual_vertices[0] = tmp_near_vertices[1];
                    }

                    if (tmp_far_vertices[0].y < tmp_far_vertices[1].y) {
                        individual_vertices[1] = tmp_far_vertices[0];
                        individual_vertices[2] = tmp_far_vertices[1];
                    } else {
                        individual_vertices[2] = tmp_far_vertices[0];
                        individual_vertices[1] = tmp_far_vertices[1];
                    }

                    Imgproc.circle(image, individual_vertices[0], 10, new Scalar(0, 255, 0), 8);   // green
                    Imgproc.circle(image, individual_vertices[1], 10, new Scalar(0, 0, 255), 8);   // blue
                    Imgproc.circle(image, individual_vertices[2], 10, new Scalar(0, 0, 0), 8);   //black
                    Imgproc.circle(image, individual_vertices[3], 10, new Scalar(255, 255, 255), 8);   // white


                    double dst_right = 0, dst_down = 0;

                    // horizontal length
                    dst_right = Math.sqrt(Math.pow(individual_vertices[0].x - individual_vertices[1].x, 2) + Math.pow(individual_vertices[0].y - individual_vertices[1].y, 2));
                    // vertical length
                    dst_down = Math.sqrt(Math.pow(individual_vertices[0].x - individual_vertices[3].x, 2) + Math.pow(individual_vertices[0].y - individual_vertices[3].y, 2));

                    double upper_hor_slope = (individual_vertices[1].y - individual_vertices[0].y) / (individual_vertices[1].x - individual_vertices[0].x);
                    double lower_hor_slope = (individual_vertices[2].y - individual_vertices[3].y) / (individual_vertices[2].x - individual_vertices[3].x);
                    double left_ver_slope = (individual_vertices[3].y - individual_vertices[0].y) / (individual_vertices[3].x - individual_vertices[0].x);
                    double right_ver_slope = (individual_vertices[1].y - individual_vertices[2].y) / (individual_vertices[1].x - individual_vertices[2].x);

                    double ratio = 0, mod = 0;

                    if (upper_hor_slope > -0.2 && upper_hor_slope < 0.2 && lower_hor_slope > -0.2 && lower_hor_slope < 0.2 && (left_ver_slope < -5 || left_ver_slope > 5) && (right_ver_slope < -5 || right_ver_slope > 5)) {
                        if (dst_right < dst_down && Imgproc.contourArea(cnt) < 10000) {   // only 1 card
                            ratio = dst_down / dst_right;
                            if (ratio > 1 && ratio < 2) {
                                points.add(new MatOfPoint(individual_vertices));
                                points_list.add(individual_vertices);
                            }
                        } else {   // card series
                            ratio = (2.3 / 1.8) * dst_right / dst_down;   // card size = 2.6cm x 1.8cm  2.3/1.9 morning
                            mod = ratio - Math.floor(ratio);
                            if (ratio > 1) {
                                int no_of_card = (int) Math.round(ratio);


                                if (Imgproc.contourArea(cnt) / no_of_card > 1000 && Imgproc.contourArea(cnt) / no_of_card < 10000) {
                                    for (int i = 0; i < no_of_card; i++) {
                                        Point[] tmp_individual_vertices = new Point[4];
                                        tmp_individual_vertices[0] = new Point(individual_vertices[0].x + (individual_vertices[1].x - individual_vertices[0].x) * i / no_of_card, individual_vertices[0].y + (individual_vertices[1].y - individual_vertices[0].y) * i / no_of_card);
                                        tmp_individual_vertices[1] = new Point(individual_vertices[0].x + (individual_vertices[1].x - individual_vertices[0].x) * (i + 1) / no_of_card, individual_vertices[0].y + (individual_vertices[1].y - individual_vertices[0].y) * (i + 1) / no_of_card);
                                        tmp_individual_vertices[2] = new Point(individual_vertices[3].x + (individual_vertices[2].x - individual_vertices[3].x) * (i + 1) / no_of_card, individual_vertices[3].y + (individual_vertices[2].y - individual_vertices[3].y) * (i + 1) / no_of_card);
                                        tmp_individual_vertices[3] = new Point(individual_vertices[3].x + (individual_vertices[2].x - individual_vertices[3].x) * i / no_of_card, individual_vertices[3].y + (individual_vertices[2].y - individual_vertices[3].y) * i / no_of_card);
                                        points.add(new MatOfPoint(tmp_individual_vertices));
                                        points_list.add(tmp_individual_vertices);
                                    }
                                }
                            }
                        }
                    }

                }
            }

            tmpNumTile = points_list.size();
            if (tmpNumTile > maxNumTile) {
                maxNumTile = tmpNumTile;
            }
            Imgproc.drawContours(image, points, -1, new Scalar(255, 0, 255), 8);   // card series
        }

        return image;
    }

    private void getContent(){
        DATA_PATH = activity.initTessTwo();
        if (!tess.init(DATA_PATH, "eng+digits+digits1+digits_comma")){
            tess.recycle();
        }
        tess.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "12345678");

        List<Point>left_list = new ArrayList<>();
        left_list.add(new Point(665,157));
        left_list.add(new Point(673,213));
        left_list.add(new Point(680,272));
        left_list.add(new Point(690,340));
        left_list.add(new Point(705,415));
        left_list.add(new Point(730,490));

        List<Point>right_list = new ArrayList<>();
        right_list.add(new Point(790,155));
        right_list.add(new Point(803,212));
        right_list.add(new Point(823,270));
        right_list.add(new Point(843,335));
        right_list.add(new Point(868,410));
        right_list.add(new Point(887,493));

        List<Double>tier_length = new ArrayList<>();
        for (Point left: left_list){
            tier_length.add(Math.sqrt(Math.pow(left.x - right_list.get(left_list.indexOf(left)).x, 2) + Math.pow(left.y - right_list.get(left_list.indexOf(left)).y, 2)));
        }

        ArrayList<ArrayList<Double>> tier_number_list = new ArrayList<>(6);
        ArrayList<ArrayList<String>> tier_content_list = new ArrayList<>(6);
        for (int i = 0; i < 6; i++){
            ArrayList<Double> tmp_int = new ArrayList<>();
            ArrayList<String> tmp = new ArrayList<>();
            tier_number_list.add(tmp_int);
            tier_content_list.add(tmp);
        }

        List<String> color_list = Arrays.asList("K", "R", "B");

        List<MatOfPoint> tmp_cont = new ArrayList<>();
        for (Point[] corner: points_list) {
            boolean inv = false;
            double black_area = 0, red_area = 0, blue_area = 0;
            Map<String, Double> tmp_color = new HashMap();
            String tile_color = null, tile_number = null;
            List<String> tmp_number = new ArrayList<>();
            for(String color : color_list) {
                if (color == "B") {
                    tmp_cont = contours_blue;
                } else if (color == "R") {
                    tmp_cont = contours_red;
                } else if (color == "K") {
                    tmp_cont = contours_black;
                }
                if (tmp_cont != null) {
                    for (MatOfPoint cnt : tmp_cont) {
                        RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(cnt.toArray()));
                        Point[] vertices = new Point[4];
                        rect.points(vertices);

                        if ((vertices[0].x > corner[0].x) && (vertices[1].x > corner[0].x) && (vertices[2].x > corner[0].x) && (vertices[3].x > corner[0].x)) {
                            if ((vertices[0].y > corner[0].y) && (vertices[1].y > corner[0].y) && (vertices[2].y > corner[0].y) && (vertices[3].y > corner[0].y)) {
                                if ((vertices[0].x < corner[1].x) && (vertices[1].x < corner[1].x) && (vertices[2].x < corner[1].x) && (vertices[3].x < corner[1].x)) {
                                    if ((vertices[0].y > corner[1].y) && (vertices[1].y > corner[1].y) && (vertices[2].y > corner[1].y) && (vertices[3].y > corner[1].y)) {
                                        if ((vertices[0].x < corner[2].x) && (vertices[1].x < corner[2].x) && (vertices[2].x < corner[2].x) && (vertices[3].x < corner[2].x)) {
                                            if ((vertices[0].y < corner[2].y) && (vertices[1].y < corner[2].y) && (vertices[2].y < corner[2].y) && (vertices[3].y < corner[2].y)) {
                                                if ((vertices[0].x > corner[3].x) && (vertices[1].x > corner[3].x) && (vertices[2].x > corner[3].x) && (vertices[3].x > corner[3].x)) {
                                                    if ((vertices[0].y < corner[3].y) && (vertices[1].y < corner[3].y) && (vertices[2].y < corner[3].y) && (vertices[3].y < corner[3].y)) {

                                                        double y_cor[] = {vertices[0].y, vertices[1].y, vertices[2].y, vertices[3].y};
                                                        Arrays.sort(y_cor);

                                                        Matrix matrix = new Matrix();


                                                        if (abs(y_cor[0] - corner[0].y) > abs(y_cor[3] - corner[3].y)) {
                                                            inv=true;
                                                            matrix.preScale(-1.0f, -1.0f);
                                                        }

                                                        int init_width = (int) ((corner[1].x - corner[0].x) / 4 + corner[0].x);
                                                        int width = (int) ((corner[1].x - corner[0].x) / 5 * 3);
                                                        if (init_width + width > hsv_input.getWidth()){
                                                            width = hsv_input.getWidth() - init_width;
                                                        }
                                                        if (init_width < 0){
                                                            init_width = 0;
                                                        }
                                                        if (width < 0){
                                                            width = 0;
                                                        }

                                                        int init_height = (int) ((corner[0].y + y_cor[0]) / 2);
                                                        int height = (int) ((corner[3].y + y_cor[3]) / 2 - corner[0].y);
                                                        if (init_height + height > hsv_input.getHeight()){
                                                            height = hsv_input.getHeight() - init_height;
                                                        }
                                                        if (init_height < 0){
                                                            init_height = 0;
                                                        }
                                                        if (height < 0){
                                                            height = 0;
                                                        }

                                                        Rect roi = new Rect(init_width, init_height, width, height);

                                                        Mat input = new Mat();


                                                        if (color == "B") {
                                                            if (Imgproc.contourArea(cnt) < 300) {
                                                                blue_area += Imgproc.contourArea(cnt);
                                                            }
                                                            input = new Mat(hsv_mask_clone1, roi);
                                                        } else if (color == "R") {
                                                            if (Imgproc.contourArea(cnt) < 300) {
                                                                red_area += Imgproc.contourArea(cnt);
                                                            }
                                                            input = new Mat(hsv_mask_clone2, roi);
                                                        } else if (color == "K") {
                                                            if (Imgproc.contourArea(cnt) < 300) {
                                                                black_area += Imgproc.contourArea(cnt);
                                                            }
                                                            input = new Mat(hsv_mask_clone3, roi);
                                                        }

                                                        Bitmap inp = Bitmap.createBitmap((int) input.width(), (int) input.height(), Bitmap.Config.ARGB_8888);

                                                        Utils.matToBitmap(input, inp);
                                                        Bitmap bitmap_input = Bitmap.createBitmap(inp, 0, 0, inp.getWidth(), inp.getHeight(), matrix, false);

                                                        tess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_CHAR);
                                                        tess.setImage(bitmap_input);

                                                        String text = "";
                                                        if (tess.getUTF8Text() != null) {
                                                            text = tess.getUTF8Text();
                                                        }
                                                        int conf = tess.meanConfidence();
                                                        if (conf > 70 && !text.contains("0") && text.length() == 1) {
                                                            tmp_number.add(text);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (color == "B") {
                        tmp_color.put(color, blue_area);
                    } else if (color == "R") {
                        tmp_color.put(color, red_area);
                    } else if (color == "K") {
                        tmp_color.put(color, black_area);
                    }
                }
            }
            double max_color_area = Collections.max(tmp_color.entrySet(), Map.Entry.comparingByValue()).getValue();
            if (max_color_area > 0) {
                tile_color = Collections.max(tmp_color.entrySet(), Map.Entry.comparingByValue()).getKey();
            }
            else{
                tile_color = "X";
            }


            if (tmp_number.size() > 0) {
                Collections.sort(tmp_number);
                int most_freq = 0;
                int index = 0;
                tile_number = tmp_number.get(0);


                for (int i = 0; i < tmp_number.size(); i++){
                    int frequency = 1;
                    if (Objects.equals(tile_number, tmp_number.get(i)) && index != i){
                        continue;
                    }
                    for (int j = 0; j < tmp_number.size(); j++){
                        if (i != j) {
                            if (Objects.equals(tmp_number.get(i), tmp_number.get(j))) {
                                frequency++;
                            }
                        }
                    }

                    if (frequency > most_freq){
                        most_freq = frequency;
                        tile_number = tmp_number.get(i);
                        index = i;
                    }
                }
            }
            else {
                tile_number = "X";
            }
            String result = tile_color + tile_number;

            double shortest_distance = abs(left_list.get(0).y - corner[0].y);
            int tier = 0;
            for (Point left: left_list){
                double distance = abs(left.y - corner[0].y);
                if (shortest_distance > distance){
                    shortest_distance = distance;
                    tier = left_list.indexOf(left);
                }
            }

            double length = tier_length.get(tier) / 3;
            double left_distance = Math.sqrt(Math.pow(left_list.get(tier).x - ((corner[0].x + (corner[1].x)) / 2), 2) + Math.pow(left_list.get(tier).y - ((corner[0].y + (corner[1].y)) / 2), 2));

            tier_number_list.get(tier).add(left_distance / length);
            tier_content_list.get(tier).add(result);
        }

        int turn = 0;

        for (ArrayList<Double> number: tier_number_list){
            if (number.size() == 3) {
                ArrayList<Integer> in = new ArrayList<>();
                in.add(0);
                in.add(1);
                in.add(2);
                activity.setRobotTile(turn * 3,"tile", tier_content_list.get(turn).get(number.indexOf(Collections.min(number))));
                in.remove(in.indexOf(number.indexOf(Collections.min(number))));
                activity.setRobotTile(turn * 3 + 2,"tile", tier_content_list.get(turn).get(number.indexOf(Collections.max(number))));
                in.remove(in.indexOf(number.indexOf(Collections.max(number))));
                activity.setRobotTile(turn * 3 + 1, "tile", tier_content_list.get(turn).get(in.get(0)));
            }
            else{
                for (Double distance: number) {
                    int no = (int) Math.floor(distance);
                    if (no >= 3) {
                        no = 2;
                    }
                    activity.setRobotTile(turn * 3 + no, "tile", tier_content_list.get(turn).get(number.indexOf(distance)));
                }
            }
            turn++;
        }
        activity.printArray();
        prepDone();
    }
}