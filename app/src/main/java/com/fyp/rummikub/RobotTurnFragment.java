package com.fyp.rummikub;

import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY_INV;
import static org.opencv.imgproc.Imgproc.THRESH_OTSU;
import static java.lang.Math.abs;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class RobotTurnFragment extends Fragment implements CameraBridgeViewBase.CvCameraViewListener2 {
    static private String LOG_TAG = RobotTurnFragment.class.getSimpleName();
    GameActivity activity;
    View view;
    TextView yes, no, ok, hint;
    ImageView icon;
    static public final boolean D = BuildConfig.DEBUG;
    private Timer mDataSendTimer = null, mReadTimer = null;
    private boolean ack = true;
    private Queue<String> command = new LinkedList<>();
    private byte[] current_command = null;
    long startTime = 0, currentTime = 0;
    private String pck_no = "";
    private Queue<byte[]> mQueue = new LinkedList<>();
    private BluetoothConnect mBluetoothConnect;
    private String message_list = "";
    private int cmd_no = 1, rerun_time = 0;
    boolean view_prepared = true, grab_prepare = true, getPosition = true, closeCamera = true, draw = false, rerun = true;
    private String DATA_PATH;
    ArrayList<ArrayList<String>> new_common_rack = new ArrayList<>();
    ArrayList<ArrayList<String>> original_common_rack = new ArrayList<>();
    ArrayList<ArrayList<String>> rack = new ArrayList<>();
    ArrayList<ArrayList<String>> mid_rack = new ArrayList<>();
    ArrayList<ArrayList<String>> new_rack = new ArrayList<>();
    ArrayList<Integer> move_index = new ArrayList<>();
    ArrayList<Integer> move_index_robot = new ArrayList<>();
    ArrayList<String> robot_rack = new ArrayList<>();
    ArrayList<String> common_rack = new ArrayList<>();

    ArrayList<Integer> grp_index = new ArrayList<>();
    ArrayList<Integer> stay_index = new ArrayList<>();
    private String py_input = "";
    Handler handler = new Handler();
    Bitmap hsv_input;
    List<Point[]> points_list = new ArrayList<>();
    private int image_row = 0, image_col = 0;
    private int maxNumTile = 0;
    private int tmpNumTile = 0;
    private boolean unknown_tile = false;
    HashMap<Point[], String> common_tier = new HashMap<>();
    ArrayList<ArrayList<Point[]>> common_points_list = new ArrayList<>(6);
    Python python;
    CameraBridgeViewBase cameraBridgeViewBase;
    Mat gray, hsv, hsvIMG, hsv_mask, hsv_mask_clone1, hsv_mask_clone2, hsv_mask_clone3, red_clone1, red_clone2, red_clone3, blue_clone1, blue_clone2, blue_clone3, black_clone1, black_clone2, black_clone3, blue1, blue, red, red1, red2, red3, red4, black1, black2, blur_gray, roi, roi1, blur, mask, mask1, hierarchy, hierarchy_hsv, hierarchy_blue, hierarchy_red, hierarchy_black;
    MatOfPoint2f approxCurve;
    TessBaseAPI tess = new TessBaseAPI();
    private List<MatOfPoint> contours = new ArrayList<>();
    private List<MatOfPoint> contours_hsv = new ArrayList<>();
    private List<MatOfPoint> contours_blue = new ArrayList<>();
    private List<MatOfPoint> contours_red = new ArrayList<>();
    private List<MatOfPoint> contours_black = new ArrayList<>();
    public RobotTurnFragment() {}

    public static RobotTurnFragment newInstance() {
        RobotTurnFragment fragment = new RobotTurnFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_robot_turn, container, false);

        icon = view.findViewById(R.id.icon);
        hint = view.findViewById(R.id.hint);
        cameraBridgeViewBase = view.findViewById(R.id.cameraViewer);

        Dialog backpress_dialog = new Dialog(getActivity());
        Dialog leave_notice = new Dialog(getActivity());

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                backpress_dialog.setContentView(R.layout.backpress_dialog);
                backpress_dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                backpress_dialog.setCancelable(false);

                yes = backpress_dialog.findViewById(R.id.yes);
                no = backpress_dialog.findViewById(R.id.no);

                yes.setOnClickListener(v -> {
                    backpress_dialog.dismiss();
                    leave_notice.setContentView(R.layout.leave_notice);
                    leave_notice.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    leave_notice.setCancelable(false);

                    ok = leave_notice.findViewById(R.id.ok);
                    ok.setOnClickListener(v1 -> {
                        leave_notice.dismiss();
                        getActivity().finish();
                    });
                    leave_notice.show();
                });
                no.setOnClickListener(v -> backpress_dialog.dismiss());
                backpress_dialog.show();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.e(LOG_TAG, "onStart()");

        activity = (GameActivity) getActivity();
        Log.e(LOG_TAG, String.valueOf(activity.getRobotRack()));

        DATA_PATH = activity.initTessTwo();
        if (!tess.init(DATA_PATH, "eng+digits+digits1+digits_comma")){
            tess.recycle();
        }
        tess.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "12345678");

        for (int i = 0; i < 6; i++) {
            ArrayList<String> tier = new ArrayList<>();
            for (int j = 0; j < 8; j++) {
                tier.add("O");
            }
            rack.add(tier);
            ArrayList<Point[]> tmp_pt = new ArrayList<>();
            common_points_list.add(tmp_pt);
        }


        if(!Python.isStarted()){
            Python.start(new AndroidPlatform(getActivity()));
        }
        python = Python.getInstance();

        for (HashMap.Entry<Integer, HashMap<String, Object>> tile : activity.getRobotRack().entrySet()) {
            if (((Boolean) tile.getValue().get("placed") && ((((String) tile.getValue().get("tile")).contains("X")) || (((String) tile.getValue().get("tile")).contains("E"))))){
                unknown_tile = true;
            }
        }

        if (OpenCVLoader.initDebug()) {
            cameraBridgeViewBase.enableView();
        }
        cameraBridgeViewBase.setCameraIndex(0);
        cameraBridgeViewBase.setAlpha(0);
        cameraBridgeViewBase.setCameraPermissionGranted();
        cameraBridgeViewBase.setCvCameraViewListener(this);
    }

    private void process() {
        getContent();
        prepInput();
        activity.runOnUiThread(() -> {
            icon.setImageResource(R.drawable.calculator);
            hint.setText("Calculating move...");
        });

        mBluetoothConnect = activity.getBluetoothConnect();

        mDataSendTimer = new Timer();
        mDataSendTimer.scheduleAtFixedRate(new DataSendTimerTask(), 1000, 250);
        mReadTimer = new Timer();
        mReadTimer.scheduleAtFixedRate(new ReadTimerTask(), 0, 250);

        while (rerun) {
            runSolver();

            activity.runOnUiThread(() -> {
                icon.setImageResource(R.drawable.move);

                hint.setText("Moving");

            });

            if (draw) {
                rerun = false;
                if (activity.getTotalRack() >= 14){
                    activity.setWinner("tie");
                    goEndPage();
                }
                int p = 0;
                for (HashMap.Entry<Integer, HashMap<String, Object>> tile : activity.getRobotRack().entrySet()) {
                    if ((Boolean) tile.getValue().get("placed")){
                        p++;
                    }
                }
                if (p >= 18){
                    activity.setWinner("tie");
                    goEndPage();
                }
                drawTile();
                activity.incTotalRack();
            } else {
                getTilePosition();
            }
        }

        if (!draw){
            getTileMovingIndex();
            putTile();
            activity.setCmd(cmd_no);
        }

        while (command.size() > 0) {}

        activity.printArray();


        // check game end
        int empty_tile = 0;
        for (HashMap.Entry<Integer, HashMap<String, Object>> tile : activity.getRobotRack().entrySet()) {
            if (!(Boolean) tile.getValue().get("placed")) {
                empty_tile++;
            }
        }
        if (empty_tile == 18) {
            if (mDataSendTimer != null) {
                mDataSendTimer.cancel();
            }
            if (mReadTimer != null) {
                mReadTimer.cancel();
            }
            activity.setWinner("robot");
            goEndPage();
        }
        else {
            activity.runOnUiThread(() -> {
                icon.setImageResource(R.drawable.ok_tick);
                hint.setText("Done!");
            });

            handler.postDelayed(() -> {
                if (mDataSendTimer != null) {
                    mDataSendTimer.cancel();
                }
                if (mReadTimer != null) {
                    mReadTimer.cancel();
                }
                goHumanTurnPage();
            }, 5000);
        }
    }

    private void putTile() {
        mQueue.add(activity.getPayload("G1 X0 Y215 Z180 P" + cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        cmd_no++;

        mQueue.add(activity.getPayload("M3 P" + cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        Log.i(LOG_TAG, String.valueOf(cmd_no));
        cmd_no++;
        mQueue.add(activity.getPayload("M5 T20 P" + cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        Log.i(LOG_TAG, String.valueOf(cmd_no));
        cmd_no++;

        if (move_index.size() > 0) {
            mQueue.add(activity.getPayload("G1 X100 Y190 Z180 P" + cmd_no + "\r\n"));
            command.add(String.valueOf(cmd_no));
            cmd_no++;
        }


        boolean position = false;

        while (move_index.size() > 0){
            int x = (int) activity.getCommonRack().get(move_index.get(0)).get("x");
            int y = (int) activity.getCommonRack().get(move_index.get(0)).get("y");
            int z = (int) activity.getCommonRack().get(move_index.get(0)).get("z");

            int up_z = z + 15;

            if (move_index.get(0) >= 24){
                up_z += 15;
            }

            mQueue.add(activity.getPayload("M1 X" + x + " Y" + y + " Z" + up_z + " P" + cmd_no + "\r\n"));
            command.add(String.valueOf(cmd_no));
            cmd_no++;
            Log.i(LOG_TAG, "G1 X" + x + " Y" + y + " Z" + up_z + " P" + cmd_no);

            mQueue.add(activity.getPayload("G1 X" + x + " Y" + y + " Z" + z + " P" + cmd_no + "\r\n"));
            command.add(String.valueOf(cmd_no));
            cmd_no++;

            if (!position) {
                mQueue.add(activity.getPayload("M3 P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;
                position = true;
            }
            else if (position){
                mQueue.add(activity.getPayload("M5 T20 P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;
                position = false;
            }
            Log.i(LOG_TAG, "G1 X" + x + " Y" + y + " Z" + z + " P" + cmd_no);
            mQueue.add(activity.getPayload("G1 X" + x + " Y" + y + " Z" + up_z + " P" + cmd_no + "\r\n"));
            command.add(String.valueOf(cmd_no));
            cmd_no++;
            mQueue.add(activity.getPayload("G1 X100 Y190 Z180 P" + cmd_no + "\r\n"));
            command.add(String.valueOf(cmd_no));
            cmd_no++;

            move_index.remove(0);

        }

        if (move_index_robot.size() > 0) {
            mQueue.add(activity.getPayload("G1 X0 Y215 Z180 P" + cmd_no + "\r\n"));
            command.add(String.valueOf(cmd_no));
            cmd_no++;
            mQueue.add(activity.getPayload("M200 T0 P" + cmd_no + "\r\n"));
            command.add(String.valueOf(cmd_no));
            cmd_no++;
            position = false;
        }

        int x = 0, y = 0, z = 0;
        while (move_index_robot.size() > 0){
            if (!position) {
                x = (int) activity.getRobotRack().get(move_index_robot.get(0)).get("x");
                y = (int) activity.getRobotRack().get(move_index_robot.get(0)).get("y");
                z = (int) activity.getRobotRack().get(move_index_robot.get(0)).get("z");
            }
            else if (position){
                x = (int) activity.getCommonRack().get(move_index_robot.get(0)).get("x");
                y = (int) activity.getCommonRack().get(move_index_robot.get(0)).get("y");
                z = (int) activity.getCommonRack().get(move_index_robot.get(0)).get("z");
            }

            int up_z = z + 15;

            if (!position) {


                mQueue.add(activity.getPayload("M3 P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                Log.i(LOG_TAG, String.valueOf(cmd_no));
                cmd_no++;
                mQueue.add(activity.getPayload("G1 X0 Y95 Z160 P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                Log.i(LOG_TAG, String.valueOf(cmd_no));
                cmd_no++;
                mQueue.add(activity.getPayload("G1 X-50 Y95 Z160 P" + cmd_no + "\r\n"));

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
                mQueue.add(activity.getPayload("M1 X" + x + " Y" + y + " Z" + up_z + " P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;

                mQueue.add(activity.getPayload("G1 X" + x + " Y" + y + " Z" + z + " P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;

                mQueue.add(activity.getPayload("M3 P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;
                mQueue.add(activity.getPayload("G1 X" + x + " Y" + y + " Z" + up_z + " P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;
                mQueue.add(activity.getPayload("G1 X-100 Y190 Z180 P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;

                mQueue.add(activity.getPayload("M200 T0 P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;

                mQueue.add(activity.getPayload("G1 X-50 Y105 Z170 P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;
                mQueue.add(activity.getPayload("G1 X0 Y105 Z170 P" + cmd_no + "\r\n"));

                command.add(String.valueOf(cmd_no));
                cmd_no++;
                mQueue.add(activity.getPayload("G1 X100 Y190 Z180 P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;

                activity.setRobotTile(move_index_robot.get(0), "placed", false);
                activity.setRobotTile(move_index_robot.get(0), "tile", "E");

                position = true;
            }
            else if (position) {

                if (move_index_robot.get(0) >= 24){
                    up_z += 15;
                }

                mQueue.add(activity.getPayload("M1 X" + x + " Y" + y + " Z" + up_z + " P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;

                mQueue.add(activity.getPayload("G1 X" + x + " Y" + y + " Z" + z + " P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;
                mQueue.add(activity.getPayload("M5 T20 P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;
                mQueue.add(activity.getPayload("G1 X" + x + " Y" + y + " Z" + up_z + " P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;
                mQueue.add(activity.getPayload("G1 X100 Y190 Z180 P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;
                mQueue.add(activity.getPayload("M200 T0 P" + cmd_no + "\r\n"));
                command.add(String.valueOf(cmd_no));
                cmd_no++;

                position = false;
            }

            move_index_robot.remove(0);
        }

        mQueue.add(activity.getPayload("M200 T0 P" + cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        cmd_no++;
        mQueue.add(activity.getPayload("G1 X0 Y215 Z180 P" + cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        cmd_no++;
    }

    private void getTileMovingIndex() {
        while (!rack.equals(new_rack)){
            boolean empty_slot = true;
            while (empty_slot) {
                empty_slot = false;
                int ter = 0;
                for (ArrayList<String> tier : rack) {
                    int tle = 0;
                    for (String tile : tier) {
                        if (!Objects.equals(tile, new_rack.get(ter).get(tle))) {
                            if (Objects.equals(tile, "O") && !Objects.equals(new_rack.get(ter).get(tle), "O")) {
                                if ((new_rack.get(ter).get(tle)).matches("[0-9]+")) {
                                    move_index.add(Integer.valueOf(new_rack.get(ter).get(tle)));
                                    move_index.add(ter * 8 + tle);
                                    rack.get(ter).set(tle, new_rack.get(ter).get(tle));
                                    rack.get((int) (Math.floor(Integer.valueOf(new_rack.get(ter).get(tle)) / 8))).set(Math.floorMod(Integer.valueOf(new_rack.get(ter).get(tle)), 8), "O");
                                    empty_slot = true;
                                } else {
                                    if (common_rack.contains(new_rack.get(ter).get(tle))) {
                                        int ter1 = 0;
                                        boolean got = false;
                                        for (ArrayList<String> tier1 : rack) {
                                            int tle1 = 0;
                                            for (String tile1 : tier1) {
                                                if (Objects.equals(tile1, new_rack.get(ter).get(tle))) {
                                                    move_index.add(ter1 * 8 + tle1);
                                                    move_index.add(ter * 8 + tle);
                                                    new_rack.get(ter).set(tle, "X");
                                                    rack.get(ter).set(tle, "X");
                                                    rack.get(ter1).set(tle1, "O");
                                                    got = true;
                                                    common_rack.remove(new_rack.get(ter).get(tle));
                                                    empty_slot = true;
                                                    break;
                                                }
                                                tle1++;
                                            }
                                            if (got) {
                                                break;
                                            }
                                            ter1++;
                                        }
                                    }
                                }
                            }
                        }
                        else{
                            if (!(new_rack.get(ter).get(tle)).matches("[0-9]+") && !Objects.equals("X", new_rack.get(ter).get(tle)) && !Objects.equals("O", new_rack.get(ter).get(tle)) && common_rack.contains(new_rack.get(ter).get(tle))) {
                                common_rack.remove(new_rack.get(ter).get(tle));
                                rack.get(ter).set(tle, "X");
                                new_rack.get(ter).set(tle, "X");
                            }
                        }
                        tle++;
                    }
                    ter++;
                }
            }
            int ter2 = 0;
            for (ArrayList<String> tier2 : rack) {
                int tle2 = 0;
                for (String tile2 : tier2) {
                    if (!Objects.equals(tile2, new_rack.get(ter2).get(tle2))) {
                        if (!common_rack.contains(new_rack.get(ter2).get(tle2)) && robot_rack.contains(new_rack.get(ter2).get(tle2))) {
                            for (HashMap.Entry<Integer, HashMap<String, Object>> t : activity.getRobotRack().entrySet()) {
                                if (Objects.equals(t.getValue().get("tile"), new_rack.get(ter2).get(tle2))){
                                    move_index_robot.add(t.getKey());
                                    move_index_robot.add(ter2 * 8 + tle2);
                                    rack.get(ter2).set(tle2, new_rack.get(ter2).get(tle2));
                                    activity.setRobotTile(t.getKey(), "tile", "E");
                                    activity.setRobotTile(t.getKey(), "placed", false);
                                    robot_rack.remove(new_rack.get(ter2).get(tle2));
                                    break;
                                }
                            }
                        }
                    }
                    tle2++;
                }
                ter2++;
            }
        }
        activity.printArray();
    }

    private void getTilePosition() {
        Collections.sort(stay_index);
        for (ArrayList<String> tier: mid_rack){
            ArrayList<String> tmp = new ArrayList<>();
            for (String t: tier){
                if (!t.matches("[0-9]+")) {
                    tmp.add("O");
                }
                else {
                    tmp.add(t);
                }
            }
            new_rack.add(tmp);
        }
        new_common_rack.sort(Comparator.comparing(ArrayList<String>::size));

        for (ArrayList<String> grp: new_common_rack){
            if (grp.size() >= 5){
                boolean empty = false;

                int tq = 0;

                for (ArrayList<String> mid_grp: new_rack){
                    if (Collections.frequency(mid_grp, "O") == 8){
                        int i = 0;
                        for (String tile: grp) {
                            new_rack.get(tq).set(i, tile);
                            i++;
                        }
                        empty = true;
                        break;
                    }
                    tq++;
                }
                if (!empty){
                    int number = 0, ti = 0;
                    ArrayList<Integer> t_5 = new ArrayList<>();

                    for (ArrayList<String> mid_grp: new_rack){
                        if (Collections.frequency(mid_grp, "O") >= 4){
                            number++;
                            t_5.add(ti);
                        }
                        ti++;
                    }

                    if (number >= 1){
                        HashMap<Integer,ArrayList<Integer>> tmp = new HashMap<>();
                        for (Integer t: t_5){
                            ArrayList<Integer> tm = new ArrayList<>();
                            boolean b = false;
                            int ty = 0;
                            for (String tile: new_rack.get(t)){
                                if (!Objects.equals(tile, "O")){
                                    tm.add(ty);
                                }
                                if (b && (Objects.equals(tile, "O")) || ty == 7){
                                    tmp.put(t, tm);
                                }
                                ty++;
                            }
                        }

                        ArrayList<Integer> t_3 = new ArrayList<>();
                        ArrayList<Integer> t_4 = new ArrayList<>();
                        boolean three = false, four = false;
                        number = 0;







                        for (Map.Entry<Integer, ArrayList<Integer>> entry : tmp.entrySet()){
                            if (new_rack.get(entry.getKey()).get(entry.getValue().get(0)).matches("[0-9]+")){
                                if (entry.getValue().get(entry.getValue().size() - 1) - entry.getValue().get(0) == 2){
                                    t_3.add(t_3.size(), entry.getKey());
                                    three = true;
                                }
                                else if (entry.getValue().get(entry.getValue().size() - 1) - entry.getValue().get(0) == 3){
                                    t_4.add(t_4.size(), entry.getKey());
                                    four = true;
                                }
                            }
                            else{
                                if (entry.getValue().get(entry.getValue().size() - 1) - entry.getValue().get(0) == 2){
                                    t_3.add(0, entry.getKey());
                                }
                                else if (entry.getValue().get(entry.getValue().size() - 1) - entry.getValue().get(0) == 3){
                                    t_4.add(0, entry.getKey());
                                }
                            }
                        }

                        int min_in = 0, max_in = 0, min_tier = 0, max_tier = 0, max_tile = 0, min_tile = 0;
                        boolean b = true;
                        int t1 = 0, t2 = 0;
                        if (!three && t_3.size() > 0){
                            t1 = t_3.get(0);
                            if (four){
                                t2 = t_4.get(0);
                            }
                            else{
                                if (t_3.size() > 1) {
                                    t2 = t_3.get(1);
                                } else if (t_4.size() > 0) {
                                    t2 = t_4.get(1);
                                }
                            }
                        }
                        else{
                            if (four && t_4.size() > 0){
                                t1 = t_3.get(0);
                                if (t_3.size() > 1){
                                    t2 = t_3.get(1);
                                }
                                else{
                                    if (t_4.size() > 1) {
                                        t2 = t_4.get(1);
                                    }
                                }
                            }
                            else if (t_3.size() > 0 && t_4.size() > 0){
                                t1 = t_4.get(0);
                                t2 = t_3.get(0);
                            }
                        }


                        for (String to: new_rack.get(t1)){
                            if (!Objects.equals(to, "O")){
                                if (b) {
                                    min_in = new_rack.get(t1).indexOf(to);
                                    b = false;
                                }

                                min_tile++;
                            }
                            else if (!b && Objects.equals(to, "O")){
                                break;
                            }
                        }
                        min_tier = t1;
                        max_tier = t2;
                        boolean c = false, d = false;
                        for (String to: new_rack.get(t2)){
                            if (!Objects.equals(to, "O")){
                                if (min_in > new_rack.get(t2).indexOf(to)) {
                                    if (!c && !d) {
                                        max_in = min_in;
                                        min_in = new_rack.get(t2).indexOf(to);
                                        max_tier = min_tier;
                                        min_tier = t2;
                                        max_tile = min_tile;
                                        min_tile = 0;
                                        c = true;
                                    }
                                }
                                else {
                                    if (!c && !d) {
                                        max_in = new_rack.get(t2).indexOf(to);
                                        max_tier = t2;
                                        d = true;
                                    }
                                }
                                if (c){
                                    min_tile++;
                                }
                                if (d){
                                    max_tile++;
                                }
                            }
                        }

                        if ((8 - (min_in + min_tile)) > max_tile){
                            int i = 1;
                            for (int m = max_in; m < 8; m++){
                                if (!Objects.equals(new_rack.get(max_tier).get(m), "O")){
                                    new_rack.get(min_tier).set(min_in + min_tile + i, new_rack.get(max_tier).get(m));
                                    new_rack.get(max_tier).set(m, "O");
                                    i++;
                                }
                                else {
                                    break;
                                }
                            }
                        }

                        else if (max_in > min_tile){
                            int i = 0;
                            for (int m = min_in; m < 8; m++){
                                if (!Objects.equals(new_rack.get(min_tier).get(m), "O")){
                                    new_rack.get(max_tier).set(i, new_rack.get(min_tier).get(m));
                                    new_rack.get(min_tier).set(m, "O");
                                    i++;
                                }
                                else {
                                    break;
                                }
                            }
                            max_tier = min_tier;
                        }

                        else {
                            int i = 0;
                            for (int m = min_in; m < 8; m++) {
                                if (!Objects.equals(new_rack.get(min_tier).get(m), "O")) {
                                    new_rack.get(min_tier).set(i, new_rack.get(min_tier).get(m));
                                    new_rack.get(min_tier).set(m, "O");
                                    i++;
                                    if (m == 7) {
                                        i++;
                                    }
                                } else {
                                    i++;
                                    break;
                                }
                            }
                            for (int m = max_in; m < 8; m++) {
                                if (!Objects.equals(new_rack.get(max_tier).get(m), "O")) {
                                    new_rack.get(min_tier).set(i, new_rack.get(max_tier).get(m));
                                    new_rack.get(max_tier).set(m, "O");
                                    i++;
                                } else {
                                    break;
                                }
                            }
                        }
                        int p = 0;
                        for (String tile: grp) {
                            new_rack.get(max_tier).set(p, tile);
                            p++;
                        }
                    }
                    else{
                        reRun();
                        rerun = true;
                        return;
                    }
                }
            }
            else if (grp.size() == 4){
                int number = 0, ti = 0;
                ArrayList<Integer> t_5 = new ArrayList<>();

                for (ArrayList<String> mid_grp: new_rack){
                    if (Collections.frequency(mid_grp, "O") >= 5){
                        number++;
                        t_5.add(ti);
                    }
                    ti++;
                }

                if (number >= 1) {
                    HashMap<Integer, ArrayList<Integer>> tmp = new HashMap<>();

                    for (Integer t : t_5) {
                        ArrayList<Integer> tm = new ArrayList<>();
                        boolean b = false;
                        int ty = 0;
                        for (String tile : new_rack.get(t)) {
                            if (!Objects.equals(tile, "O")) {
                                tm.add(ty);
                                b = true;
                            }
                            if (b && (Objects.equals(tile, "O") || ty == 7)) {
                                tmp.put(t, tm);
                            }
                            ty++;
                        }
                    }
                    boolean pass = false;
                    for (Map.Entry<Integer, ArrayList<Integer>> entry : tmp.entrySet()) {
                        if (entry.getValue().get(0) >= 5) {
                            int p = 0;
                            for (String tile : grp) {
                                new_rack.get(entry.getKey()).set(p, tile);
                                p++;
                            }
                            pass = true;
                            break;
                        } else if (entry.getValue().get(entry.getValue().size() - 1) <= 2) {
                            int p = 4;
                            for (String tile : grp) {
                                new_rack.get(entry.getKey()).set(p, tile);
                                p++;
                            }
                            pass = true;
                            break;
                        }
                    }

                    if (!pass) {
                        boolean empty = false;
                        int tq = 0;

                        for (ArrayList<String> mid_grp : new_rack) {
                            if (Collections.frequency(mid_grp, "O") == 8) {
                                int i = 0;
                                for (String tile : grp) {
                                    new_rack.get(tq).set(i, tile);
                                    i++;
                                }
                                empty = true;
                                break;
                            }
                            tq++;
                        }
                        if (!empty) {

                            ArrayList<Integer> t_3 = new ArrayList<>();
                            ArrayList<Integer> t_4 = new ArrayList<>();
                            boolean three = false, four = false;
                            number = 0;

                            for (Map.Entry<Integer, ArrayList<Integer>> entry : tmp.entrySet()){
                                if (new_rack.get(entry.getKey()).get(entry.getValue().get(0)).matches("[0-9]+")){
                                    if (entry.getValue().get(entry.getValue().size() - 1) - entry.getValue().get(0) == 2){
                                        t_3.add(t_3.size(), entry.getKey());
                                        three = true;
                                    }
                                    else if (entry.getValue().get(entry.getValue().size() - 1) - entry.getValue().get(0) == 3){
                                        t_4.add(t_4.size(), entry.getKey());
                                        four = true;
                                    }
                                }
                                else{
                                    if (entry.getValue().get(entry.getValue().size() - 1) - entry.getValue().get(0) == 2){
                                        t_3.add(0, entry.getKey());
                                    }
                                    else if (entry.getValue().get(entry.getValue().size() - 1) - entry.getValue().get(0) == 3){
                                        t_4.add(0, entry.getKey());
                                    }
                                }
                            }

                            boolean ok = false;

                            int tier = 0;
                            if (!three && t_3.size() > 0) {
                                int i = 0, j = 0;
                                for (String tr : new_rack.get(t_3.get(0))) {
                                    if (!Objects.equals(tr, "O")) {
                                        new_rack.get(t_3.get(0)).set(j, tr);
                                        new_rack.get(t_3.get(0)).set(i, "O");
                                        j++;
                                    }
                                    if (i == tmp.get(t_3.get(0)).get(tmp.get(t_3.get(0)).size() - 1)) {
                                        break;
                                    }
                                    i++;
                                }
                                tier = t_3.get(0);
                                ok = true;
                            } else {
                                if (t_3.size() > 0) {
                                    int i = 0, j = 0;
                                    for (String tr : new_rack.get(t_3.get(0))) {
                                        if (!Objects.equals(tr, "O")) {
                                            new_rack.get(t_3.get(0)).set(j, tr);
                                            new_rack.get(t_3.get(0)).set(i, "O");
                                            j++;
                                        }
                                        if (i == tmp.get(t_3.get(0)).get(tmp.get(t_3.get(0)).size() - 1)) {
                                            break;
                                        }
                                        i++;
                                    }
                                    tier = t_3.get(0);
                                    ok = true;
                                }
                            }

                            if (!ok && (t_3.size() + t_4.size() >= 2)){
                                int min_in = 0, max_in = 0, min_tier = 0, max_tier = 0, max_tile = 0, min_tile = 0;
                                boolean b = true;
                                int t1 = 0, t2 = 0;
                                if (!three && t_3.size() > 0){
                                    t1 = t_3.get(0);
                                    if (four){
                                        t2 = t_4.get(0);
                                    }
                                    else{
                                        if (t_3.size() > 1) {
                                            t2 = t_3.get(1);
                                        } else if (t_4.size() > 0) {
                                            t2 = t_4.get(1);
                                        }
                                    }
                                }
                                else{
                                    if (four && t_4.size() > 0){
                                        t1 = t_3.get(0);
                                        if (t_3.size() > 1){
                                            t2 = t_3.get(1);
                                        }
                                        else{
                                            if (t_4.size() > 1) {
                                                t2 = t_4.get(1);
                                            }
                                        }
                                    }
                                    else if (t_3.size() > 0 && t_4.size() > 0){
                                        t1 = t_4.get(0);
                                        t2 = t_3.get(0);
                                    }
                                }


                                for (String to: new_rack.get(t1)){
                                    if (!Objects.equals(to, "O")){
                                        if (b) {
                                            min_in = new_rack.get(t1).indexOf(to);
                                            b = false;
                                        }

                                        min_tile++;
                                    }
                                    else if (!b && Objects.equals(to, "O")){
                                        break;
                                    }
                                }
                                min_tier = t1;
                                max_tier = t2;
                                boolean c = false, d = false;
                                for (String to: new_rack.get(t2)){
                                    if (!Objects.equals(to, "O")){
                                        if (min_in > new_rack.get(t2).indexOf(to)) {
                                            if (!c && !d) {
                                                max_in = min_in;
                                                min_in = new_rack.get(t2).indexOf(to);
                                                max_tier = min_tier;
                                                min_tier = t2;
                                                max_tile = min_tile;
                                                min_tile = 0;
                                                c = true;
                                            }
                                        }
                                        else {
                                            if (!c && !d) {
                                                max_in = new_rack.get(t2).indexOf(to);
                                                max_tier = t2;
                                                d = true;
                                            }
                                        }
                                        if (c){
                                            min_tile++;
                                        }
                                        if (d){
                                            max_tile++;
                                        }
                                    }
                                }

                                if ((8 - (min_in + min_tile)) > max_tile){
                                    int i = 1;
                                    for (int m = max_in; m < 8; m++){
                                        if (!Objects.equals(new_rack.get(max_tier).get(m), "O")){
                                            new_rack.get(min_tier).set(min_in + min_tile + i, new_rack.get(max_tier).get(m));
                                            new_rack.get(max_tier).set(m, "O");
                                            i++;
                                        }
                                        else {
                                            break;
                                        }
                                    }
                                }

                                else if (max_in > min_tile){
                                    int i = 0;
                                    for (int m = min_in; m < 8; m++){
                                        if (!Objects.equals(new_rack.get(min_tier).get(m), "O")){
                                            new_rack.get(max_tier).set(i, new_rack.get(min_tier).get(m));
                                            new_rack.get(min_tier).set(m, "O");
                                            i++;
                                        }
                                        else {
                                            break;
                                        }
                                    }
                                    max_tier = min_tier;
                                }

                                else {
                                    int i = 0;
                                    for (int m = min_in; m < 8; m++) {
                                        if (!Objects.equals(new_rack.get(min_tier).get(m), "O")) {
                                            new_rack.get(min_tier).set(i, new_rack.get(min_tier).get(m));
                                            new_rack.get(min_tier).set(m, "O");
                                            i++;
                                            if (m == 7) {
                                                i++;
                                            }
                                        } else {
                                            i++;
                                            break;
                                        }
                                    }
                                    for (int m = max_in; m < 8; m++) {
                                        if (!Objects.equals(new_rack.get(max_tier).get(m), "O")) {
                                            new_rack.get(min_tier).set(i, new_rack.get(max_tier).get(m));
                                            new_rack.get(max_tier).set(m, "O");
                                            i++;
                                        } else {
                                            break;
                                        }
                                    }
                                }
                                tier = max_tier;
                            }


                            int p = 4;
                            for (String tile : grp) {
                                new_rack.get(tier).set(p, tile);
                                p++;
                            }
                        }
                    }
                }
                else{
                    reRun();
                    rerun = true;
                    return;
                }
            }

            else if (grp.size() == 3){
                int number = 0, ti = 0;
                ArrayList<Integer> t_5 = new ArrayList<>();

                for (ArrayList<String> mid_grp: new_rack){
                    if (Collections.frequency(mid_grp, "O") >= 4){
                        number++;
                        t_5.add(ti);
                    }
                    ti++;
                }


                if (number >= 1) {
                    HashMap<Integer, ArrayList<Integer>> tmp = new HashMap<>();

                    for (Integer t : t_5) {
                        ArrayList<Integer> tm = new ArrayList<>();
                        boolean b = false;
                        int ty = 0;
                        for (String tile : new_rack.get(t)) {
                            if (!Objects.equals(tile, "O")) {
                                tm.add(ty);
                                b = true;
                            }
                            if (b && (Objects.equals(tile, "O") || ty == 7)) {
                                tmp.put(t, tm);
                            }
                            ty++;
                        }
                    }
                    boolean pass = false;
                    for (Map.Entry<Integer, ArrayList<Integer>> entry : tmp.entrySet()) {
                        if (entry.getValue().get(0) >= 4) {
                            int p = 0;
                            for (String tile : grp) {
                                new_rack.get(entry.getKey()).set(p, tile);
                                p++;
                            }
                            pass = true;
                            break;
                        } else if (entry.getValue().get(entry.getValue().size() - 1) <= 3) {
                            int p = 5;
                            for (String tile : grp) {
                                new_rack.get(entry.getKey()).set(p, tile);
                                p++;
                            }
                            pass = true;
                            break;
                        }
                    }

                    if (!pass) {
                        boolean empty = false;
                        int tq = 0;

                        for (ArrayList<String> mid_grp : new_rack) {
                            if (Collections.frequency(mid_grp, "O") == 8) {
                                int i = 0;
                                for (String tile : grp) {
                                    new_rack.get(tq).set(i, tile);
                                    i++;
                                }
                                empty = true;
                                break;
                            }
                            tq++;
                        }
                        if (!empty) {
                            ArrayList<Integer> t_3 = new ArrayList<>();
                            ArrayList<Integer> t_4 = new ArrayList<>();
                            boolean three = false, four = false;
                            number = 0;

                            for (Map.Entry<Integer, ArrayList<Integer>> entry : tmp.entrySet()) {
                                if (new_rack.get(entry.getKey()).get(entry.getValue().get(0)).matches("[0-9]+")) {
                                    if (entry.getValue().get(entry.getValue().size() - 1) - entry.getValue().get(0) == 2) {
                                        t_3.add(t_3.size(), entry.getKey());
                                        three = true;
                                    } else if (entry.getValue().get(entry.getValue().size() - 1) - entry.getValue().get(0) == 3) {
                                        t_4.add(t_4.size(), entry.getKey());
                                        four = true;
                                    }
                                } else {
                                    if (entry.getValue().get(entry.getValue().size() - 1) - entry.getValue().get(0) == 2) {
                                        t_3.add(0, entry.getKey());
                                    } else if (entry.getValue().get(entry.getValue().size() - 1) - entry.getValue().get(0) == 3) {
                                        t_4.add(0, entry.getKey());
                                    }
                                }
                            }

                            int tier = 0;
                            if (!three && t_3.size() > 0) {
                                int i = 0, j = 0;
                                for (String tr : new_rack.get(t_3.get(0))) {
                                    if (!Objects.equals(tr, "O")) {
                                        new_rack.get(t_3.get(0)).set(j, tr);
                                        new_rack.get(t_3.get(0)).set(i, "O");
                                        j++;
                                    }
                                    if (i == tmp.get(t_3.get(0)).get(tmp.get(t_3.get(0)).size() - 1)) {
                                        break;
                                    }
                                    i++;
                                }
                                tier = t_3.get(0);
                            } else if (!four && t_4.size() > 0) {
                                int i = 0, j = 0;
                                for (String tr : new_rack.get(t_4.get(0))) {
                                    if (!Objects.equals(tr, "O")) {
                                        new_rack.get(t_4.get(0)).set(j, tr);
                                        new_rack.get(t_4.get(0)).set(i, "O");
                                        j++;
                                    }
                                    if (i == tmp.get(t_4.get(0)).get(tmp.get(t_4.get(0)).size() - 1)) {
                                        break;
                                    }
                                    i++;
                                }
                                tier = t_4.get(0);
                            } else {
                                if (t_3.size() > 0) {
                                    int i = 0, j = 0;
                                    for (String tr : new_rack.get(t_3.get(0))) {
                                        if (!Objects.equals(tr, "O")) {
                                            new_rack.get(t_3.get(0)).set(j, tr);
                                            new_rack.get(t_3.get(0)).set(i, "O");
                                            j++;
                                        }
                                        if (i == tmp.get(t_3.get(0)).get(tmp.get(t_3.get(0)).size() - 1)) {
                                            break;
                                        }
                                        i++;
                                    }
                                    tier = t_3.get(0);
                                } else if (t_4.size() > 0) {
                                    int i = 0, j = 0;
                                    for (String tr : new_rack.get(t_4.get(0))) {
                                        if (!Objects.equals(tr, "O")) {
                                            new_rack.get(t_4.get(0)).set(j, tr);
                                            new_rack.get(t_4.get(0)).set(i, "O");
                                            j++;
                                        }
                                        if (i == tmp.get(t_4.get(0)).get(tmp.get(t_4.get(0)).size() - 1)) {
                                            break;
                                        }
                                        i++;
                                    }
                                    tier = t_4.get(0);
                                }
                            }


                            int p = 5;
                            for (String tile : grp) {
                                new_rack.get(tier).set(p, tile);
                                p++;
                            }
                        }
                    }
                }
                else{
                    reRun();
                    rerun = true;
                    return;
                }
            }
        }
        rerun = false;
    }

    private void reRun() {
        rerun_time++;
        mid_rack.clear();
        new_rack.clear();
        new_common_rack.clear();
        robot_rack.clear();
        common_rack.clear();
    }

    private void drawTile() {
        mQueue.add(activity.getPayload("G1 X0 Y215 Z180 P" + cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        cmd_no++;


        mQueue.add(activity.getPayload("M3 P" + cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        Log.i(LOG_TAG, String.valueOf(cmd_no));
        cmd_no++;
        mQueue.add(activity.getPayload("M202 P"+ cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        Log.i(LOG_TAG, String.valueOf(cmd_no));
        cmd_no++;
        mQueue.add(activity.getPayload("G1 X0 Y95 Z160 P" + cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        Log.i(LOG_TAG, String.valueOf(cmd_no));
        cmd_no++;
        mQueue.add(activity.getPayload("G1 X-50 Y95 Z160 P" + cmd_no + "\r\n"));

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



        mQueue.add(activity.getPayload("M1 X-139 Y283 Z20 T-60 P" + cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        cmd_no++;
        mQueue.add(activity.getPayload("G1 X-139 Y283 Z-20 P" + cmd_no + "\r\n"));
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

        int x = 184, y = 138, z = 125;
        int up_z = z + 15;
        for (HashMap.Entry<Integer, HashMap<String, Object>> tile : activity.getRobotRack().entrySet()) {
            if (!(Boolean) tile.getValue().get("placed")) {
                x = (int) tile.getValue().get("x");
                y = (int) tile.getValue().get("y");
                z = (int) tile.getValue().get("z");

                up_z = z + 15;
                activity.setRobotTile(tile.getKey(), "placed", true);
                activity.setRobotTile(tile.getKey(), "tile", "XX");
                break;
            }
        }

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

        mQueue.add(activity.getPayload("M200 T0 P" + cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        cmd_no++;

        mQueue.add(activity.getPayload("M3 P" + cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        Log.i(LOG_TAG, String.valueOf(cmd_no));
        cmd_no++;

        mQueue.add(activity.getPayload("G1 X-50 Y95 Z160 P" + cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        cmd_no++;
        mQueue.add(activity.getPayload("G1 X0 Y95 Z160 P" + cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        cmd_no++;
        mQueue.add(activity.getPayload("G1 X0 Y215 Z180 P" + cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        cmd_no++;
        mQueue.add(activity.getPayload("M200 T0 P" + cmd_no + "\r\n"));
        command.add(String.valueOf(cmd_no));
        cmd_no++;

    }

    private void prepInput() {
        int t = 0;
        for (ArrayList<String> tier: rack){
            int s = 0;
            ArrayList<String> group = new ArrayList<>();
            ArrayList<Integer> index = new ArrayList<>();
            boolean unknown = false;
            for (String set: tier){
                if (!Objects.equals(set, "O")) {
                    group.add(set);
                    index.add(s);
                    if (set.contains("X")) {
                        unknown = true;
                    }
                    if (s != (tier.size() - 1)){
                        s++;
                        continue;
                    }
                }
                if (group.size() > 0){
                    String tmp_input = "";

                    if (group.size() < 3 || unknown){
                        for (Integer in: index) {
                            rack.get(t).set(in, String.valueOf(t * 8 + in));
                        }
                        unknown = false;
                    }
                    else{
                        String type = "";
                        int prev_number = Integer.parseInt(group.get(0).substring(1, 2));
                        String color = group.get(0).substring(0, 1);
                        String app_color = group.get(0).substring(0, 1);
                        int same_color = 1, same_number = 1;
                        boolean consecutive = true;
                        int ti = 0;
                        for (String tile: group){
                            Log.i("stopp", String.valueOf(same_number));
                            tmp_input = tmp_input + tile + " ";
                            if (ti == 0){
                                ti++;
                                continue;
                            }
                            if (consecutive){
                                if (tile.substring(0, 1).equals(color)){
                                    if (abs(prev_number - Integer.parseInt(tile.substring(1, 2))) != 1){
                                        consecutive = false;
                                    }
                                    else {
                                        same_color++;
                                    }
                                    prev_number = Integer.parseInt(tile.substring(1, 2));
                                }
                            }
                            if (!app_color.contains(tile.substring(0, 1))){
                                app_color = app_color + tile.substring(0, 1);
                                if ((!tile.substring(1, 2).equals("X")) && (prev_number == Integer.parseInt(tile.substring(1, 2)))){
                                    same_number++;
                                }
                            }
                            ti++;
                        }
                        if (same_color == group.size()){
                            type = type + "R";
                        }
                        if (same_number == group.size()){
                            type = type + "G";
                        }
                        if (type.length() != 1){
                            for (Integer in: index) {
                                rack.get(t).set(in, String.valueOf(t * 8 + in));
                            }
                        }
                        else{
                            py_input = py_input + String.join(" ", group) + " | ";
                            ArrayList<String> tmp = new ArrayList<>(group);
                            original_common_rack.add(tmp);
                            grp_index.add(t * 8 + index.get(0));
                        }
                    }
                    group.clear();
                    index.clear();
                }
                s++;
            }
            t++;
        }

        if (!py_input.isEmpty()) {
            if (py_input.substring(py_input.length() - 3).equals(" | ")) {
                py_input = py_input.substring(0, py_input.length() - 3);
            }
        }
    }

    private void runSolver() {
        mid_rack = new ArrayList<>(rack);
        activity.printArray();

        PyObject pyObject = python.getModule("main");
        String[][] groupsToAdd = null;
        String[] newHand = null;
        for (HashMap.Entry<Integer, HashMap<String, Object>> tile : activity.getRobotRack().entrySet()){
            if (tile.getValue().get("tile") != "E" && !((String) tile.getValue().get("tile")).contains("X")){
                robot_rack.add((String) tile.getValue().get("tile"));
            }
        }
        for (int i = 0; i < robot_rack.size(); i++){
            if (i == rerun_time){
                break;
            }
            if (robot_rack.size() > 0) {
                robot_rack.remove(0);
            }
            else if (robot_rack.size() == 0){
                rerun = false;
                draw = true;
                return;
            }
        }
        String robot_input = String.join(" ", robot_rack);
        boolean run = true;
        while (run) {
            List<PyObject> result = pyObject.callAttr("solve", py_input, robot_input).asList();
            groupsToAdd = result.get(0).toJava(String[][].class);
            newHand = result.get(1).toJava(String[].class);
            if (py_input.length() == 0){
                run = false;
            }
            else if (py_input.length() > 0){
                if (groupsToAdd != null && groupsToAdd.length > 0){
                    run = false;
                }
            }
        }


        for (String[] group: groupsToAdd){
            ArrayList<String> tmp = new ArrayList<>();
            boolean skip = false;
            for (String t: group){
                String tile = t.substring(t.length() - 1) + t.charAt(0);
                common_rack.add(tile);
                tmp.add(tile);
            }
            int g = 0;
            for (ArrayList<String> grp: original_common_rack){
                if (grp.containsAll(tmp)){
                    if (grp.size() == tmp.size()) {
                        skip = true;
                        for (int i = 0; i < grp.size(); i++){
                            common_rack.remove(tmp.get(i));
                            mid_rack.get((int) (Math.floor(grp_index.get(g) / 8))).set(Math.floorMod(grp_index.get(g), 8) + i, String.valueOf(grp_index.get(g) + i));
                            stay_index.add(g + i);
                        }
                        break;
                    }
                }
                g++;
            }
            if (!skip) {
                new_common_rack.add(tmp);
            }
        }

        ArrayList<String> remain_hand = new ArrayList<>();
        if (py_input.length() == 0){
            ArrayList<String> tmp_hand = new ArrayList<>();
            for (String t : newHand) {
                String tile = t.substring(t.length() - 1) + t.charAt(0);
                tmp_hand.add(tile);
            }
            remain_hand = new ArrayList<>(robot_rack);
            for (String r: tmp_hand){
                remain_hand.remove(r);
            }
        }
        else {
            for (String t : newHand) {
                String tile = t.substring(t.length() - 1) + t.charAt(0);
                remain_hand.add(tile);
            }
        }

        if (remain_hand.size() == robot_rack.size()){
            draw = true;
            return;
        }

        for (String r: remain_hand){
            robot_rack.remove(r);
        }

        for (String r: robot_rack){
            common_rack.remove(r);
        }
    }

    void goHumanTurnPage(){
        HumanTurnFragment HumanTurnFragment = new HumanTurnFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in, R.anim.slide_out, R.anim.enter_top, R.anim.exit_bottom);
        transaction.addToBackStack(null);
        transaction.replace(R.id.frag_con, HumanTurnFragment).commit();
    }

    void goEndPage(){
        EndPageFragment EndPageFragment = new EndPageFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in, R.anim.slide_out, R.anim.enter_top, R.anim.exit_bottom);
        transaction.addToBackStack(null);
        transaction.replace(R.id.frag_con, EndPageFragment).commit();
    }

    class ReadTimerTask extends TimerTask {
        @Override
        public void run() {
            String message = null;
            try {
                message = mBluetoothConnect.read();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mDataSendTimer != null) {
            mDataSendTimer.cancel();
        }
        if (mReadTimer != null) {
            mReadTimer.cancel();
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
        mask = new Mat();
        mask1 = new Mat();
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
        roi1 = new Mat();
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
        mask.release();
        mask1.release();
        roi.release();
        roi1.release();
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

        if (getPosition) {
            currentTime = System.currentTimeMillis();
            if (currentTime - startTime > 500){
                closeCamera = false;
            }
            if (currentTime - startTime > 500 && currentTime - startTime < 1000) {
                hsv_mask_clone1 = hsvIMG.clone();
                red_clone1 = hsvIMG.clone();
                blue_clone1 = hsvIMG.clone();
                black_clone1 = hsvIMG.clone();
            }
            if (currentTime - startTime > 1000 && currentTime - startTime <1500) {
                hsv_mask_clone2 = hsvIMG.clone();
                red_clone2 = hsvIMG.clone();
                blue_clone2 = hsvIMG.clone();
                black_clone2 = hsvIMG.clone();
            }
            if (currentTime - startTime > 1500 && tmpNumTile >= maxNumTile) {
                Size size = image.size();
                hsv_mask_clone3 = hsvIMG.clone();
                red_clone3 = hsvIMG.clone();
                blue_clone3 = hsvIMG.clone();
                black_clone3 = hsvIMG.clone();
                hsv_input = Bitmap.createBitmap((int) size.width, (int) size.height, Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(hsvIMG, hsv_input);
                getPosition = false;
                process();
            }

            if (!closeCamera) {
                image_row = image.rows();
                image_col = image.cols();

                Imgproc.cvtColor(image, gray, Imgproc.COLOR_RGBA2GRAY);
                Imgproc.adaptiveThreshold(gray, hsvIMG, 255, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY_INV, 13, 1);

                Imgproc.GaussianBlur(image, blur, new Size(7, 7), 7);
                Imgproc.cvtColor(blur, blur_gray, Imgproc.COLOR_RGBA2GRAY);

                Imgproc.threshold(blur_gray,blur_gray, 0, 255, THRESH_BINARY + THRESH_OTSU);

                List<MatOfPoint> list1 = new ArrayList<>();

                list1.add(new MatOfPoint (new Point(23,590),new Point(23,515),new Point(60,515), new Point(60,420), new Point(160,150), new Point(590,150), new Point(530,630)));

                mask1 = new Mat(image.rows(), image.cols(), CV_8UC3, new Scalar(255, 255, 255));
                Imgproc.fillPoly(mask1, list1, new Scalar(0, 0, 0), Imgproc.LINE_8);
                Core.inRange(mask1, new Scalar(0, 0, 0), new Scalar(180, 255, 130), roi1);


                if (unknown_tile) {
                    List<MatOfPoint> list = new ArrayList<>();

                    list.add(new MatOfPoint (new Point(800,150), new Point(620,150), new Point(710,570), new Point(900,550)));

                    mask = new Mat(image.rows(), image.cols(), CV_8UC3, new Scalar(255, 255, 255));
                    Imgproc.fillPoly(mask, list, new Scalar(0, 0, 0), Imgproc.LINE_8);
                    Core.inRange(mask, new Scalar(0, 0, 0), new Scalar(180, 255, 130), roi);
                    Core.bitwise_or(roi, roi1, roi1);
                }

                Core.bitwise_and(roi1, blur_gray, blur_gray);


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
        }

        return image;
    }

    private void getContent() {
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


        List<Double> tier_length = new ArrayList<>();
        for (Point left : left_list) {
            tier_length.add(Math.sqrt(Math.pow(left.x - right_list.get(left_list.indexOf(left)).x, 2) + Math.pow(left.y - right_list.get(left_list.indexOf(left)).y, 2)));
        }

        List<Point> left_list_common = new ArrayList<>();

        left_list_common.add(new Point(170, 153));
        left_list_common.add(new Point(147, 213));
        left_list_common.add(new Point(120, 280));
        left_list_common.add(new Point(92, 350));
        left_list_common.add(new Point(55, 435));
        left_list_common.add(new Point(20, 530));

        List<Point> right_list_common = new ArrayList<>();
        right_list_common.add(new Point(535, 153));
        right_list_common.add(new Point(530, 215));
        right_list_common.add(new Point(523, 282));
        right_list_common.add(new Point(517, 366));
        right_list_common.add(new Point(510, 450));
        right_list_common.add(new Point(500, 550));


        List<Double> tier_length_common = new ArrayList<>();
        for (Point left : left_list_common) {
            tier_length_common.add(Math.sqrt(Math.pow(left.x - right_list_common.get(left_list_common.indexOf(left)).x, 2) + Math.pow(left.y - right_list_common.get(left_list_common.indexOf(left)).y, 2)));
        }

        ArrayList<ArrayList<Double>> tier_number_list = new ArrayList<>(6);
        ArrayList<ArrayList<String>> tier_content_list = new ArrayList<>(6);

        for (int i = 0; i < 6; i++) {
            ArrayList<Double> tmp_int = new ArrayList<>();
            ArrayList<String> tmp = new ArrayList<>();
            tier_number_list.add(tmp_int);
            tier_content_list.add(tmp);
        }

        List<String> color_list = Arrays.asList("K", "R", "B");

        List<MatOfPoint> tmp_cont = new ArrayList<>();

        for (Point[] corner : points_list) {
            boolean inv = false;
            double black_area = 0, red_area = 0, blue_area = 0;
            Map<String, Double> tmp_color = new HashMap();
            String tile_color = null, tile_number = null;
            List<String> tmp_number = new ArrayList<>();
            for (String color : color_list) {
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
                                                            inv = true;
                                                            matrix.preScale(-1.0f, -1.0f);
                                                        }


                                                        int init_width = (int) ((corner[1].x - corner[0].x) / 4 + corner[0].x);
                                                        int width = (int) ((corner[1].x - corner[0].x) / 5 * 3);
                                                        if (init_width + width > hsv_input.getWidth()) {
                                                            width = hsv_input.getWidth() - init_width;
                                                        }
                                                        if (init_width < 0) {
                                                            init_width = 0;
                                                        }
                                                        if (width < 0) {
                                                            width = 0;
                                                        }

                                                        int init_height = (int) ((corner[0].y + y_cor[0]) / 2);
                                                        int height = (int) ((corner[3].y + y_cor[3]) / 2 - corner[0].y);
                                                        if (init_height + height > hsv_input.getHeight()) {
                                                            height = hsv_input.getHeight() - init_height;
                                                        }
                                                        if (init_height < 0) {
                                                            init_height = 0;
                                                        }
                                                        if (height < 0) {
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
                                                        Bitmap finalBitmap_input = bitmap_input;

                                                        tess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_CHAR);
                                                        tess.setImage(bitmap_input);


                                                        String text = tess.getUTF8Text();
                                                        int conf = tess.meanConfidence();
                                                        if (conf > 70 && !text.contains("0") && text.length() == 1) {
                                                            tmp_number.add(text);
                                                        }


                                                        int i = 1;
                                                        while (i < 4) {
                                                            input = new Mat();

                                                            if (color == "B") {
                                                                if (i == 1) {
                                                                    input = new Mat(blue_clone1, roi);
                                                                } else if (i == 2) {
                                                                    input = new Mat(blue_clone2, roi);
                                                                } else if (i == 3) {
                                                                    input = new Mat(blue_clone3, roi);
                                                                }
                                                            } else if (color == "R") {
                                                                if (i == 1) {
                                                                    input = new Mat(red_clone1, roi);
                                                                } else if (i == 2) {
                                                                    input = new Mat(red_clone2, roi);
                                                                } else if (i == 3) {
                                                                    input = new Mat(red_clone3, roi);
                                                                }
                                                            } else if (color == "K") {
                                                                if (i == 1) {
                                                                    input = new Mat(black_clone1, roi);
                                                                } else if (i == 2) {
                                                                    input = new Mat(black_clone2, roi);
                                                                } else if (i == 3) {
                                                                    input = new Mat(black_clone3, roi);
                                                                }
                                                            }


                                                            inp = Bitmap.createBitmap((int) input.width(), (int) input.height(), Bitmap.Config.ARGB_8888);


                                                            Utils.matToBitmap(input, inp);
                                                            bitmap_input = Bitmap.createBitmap(inp, 0, 0, inp.getWidth(), inp.getHeight(), matrix, false);

                                                            tess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_CHAR);
                                                            tess.setImage(bitmap_input);


                                                            text = tess.getUTF8Text();
                                                            conf = tess.meanConfidence();
                                                            if (conf > 70 && !text.contains("0") && text.length() == 1) {
                                                                tmp_number.add(text);
                                                            }
                                                            i++;
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
            } else {
                tile_color = "X";
            }

            if (tmp_number.size() > 0) {
                Collections.sort(tmp_number);
                int most_freq = 0;
                int index = 0;
                tile_number = tmp_number.get(0);


                for (int i = 0; i < tmp_number.size(); i++) {
                    int frequency = 1;
                    if (Objects.equals(tile_number, tmp_number.get(i)) && index != i) {
                        continue;
                    }
                    for (int j = 0; j < tmp_number.size(); j++) {
                        if (i != j) {
                            if (Objects.equals(tmp_number.get(i), tmp_number.get(j))) {
                                frequency++;
                            }
                        }
                    }

                    if (frequency > most_freq) {
                        most_freq = frequency;
                        tile_number = tmp_number.get(i);
                        index = i;
                    }
                }
            } else {
                tile_number = "X";
            }
            String result = tile_color + tile_number;


            if (corner[0].x > 600) {
                double shortest_distance = abs(left_list.get(0).y - corner[0].y);
                int tier = 0;
                for (Point left : left_list) {
                    double distance = abs(left.y - corner[0].y);
                    if (shortest_distance > distance) {
                        shortest_distance = distance;
                        tier = left_list.indexOf(left);
                    }
                }


                double length = tier_length.get(tier) / 3;
                double left_distance = Math.sqrt(Math.pow(left_list.get(tier).x - ((corner[0].x + (corner[1].x)) / 2), 2) + Math.pow(left_list.get(tier).y - ((corner[0].y + (corner[1].y)) / 2), 2));
                tier_number_list.get(tier).add(left_distance / length);
                tier_content_list.get(tier).add(result);


            } else {
                double shortest_distance = abs(left_list_common.get(0).y - corner[0].y);
                int tier = 0;
                for (Point left : left_list_common) {
                    double distance = abs(left.y - corner[0].y);
                    if (shortest_distance > distance) {
                        shortest_distance = distance;
                        tier = left_list_common.indexOf(left);
                    }
                }
                common_tier.put(corner, result);
                common_points_list.get(tier).add(corner);
            }
        }

        if (unknown_tile && tier_number_list.size() > 0) {
            int turn = 0;

            for (ArrayList<Double> number : tier_number_list) {
                String result = "";
                if (number.size() == 3) {
                    ArrayList<Integer> in = new ArrayList<>();
                    in.add(0);
                    in.add(1);
                    in.add(2);

                    if (((String) activity.getRobotRack().get(turn * 3).get("tile")).equals("E")) {
                        result = tier_content_list.get(turn).get(number.indexOf(Collections.min(number)));
                    }
                    else {
                        if (((String) activity.getRobotRack().get(turn * 3).get("tile")).substring(0, 1).equals("X")) {
                            result = result + tier_content_list.get(turn).get(number.indexOf(Collections.min(number))).substring(0, 1);
                        } else if (tier_content_list.get(turn).get(number.indexOf(Collections.min(number))).substring(0, 1).equals("X")) {
                            result = result + ((String) activity.getRobotRack().get(turn * 3).get("tile")).substring(0, 1);
                        } else {
                            if (Objects.equals(((String) activity.getRobotRack().get(turn * 3).get("tile")).substring(0, 1), tier_content_list.get(turn).get(number.indexOf(Collections.min(number))).substring(0, 1))) {
                                result = result + ((String) activity.getRobotRack().get(turn * 3).get("tile")).substring(0, 1);
                            } else {
                                result = result + "X";
                            }
                        }
                        if (((String) activity.getRobotRack().get(turn * 3).get("tile")).substring(1, 2).equals("X")) {
                            result = result + tier_content_list.get(turn).get(number.indexOf(Collections.min(number))).substring(1, 2);
                        } else if (tier_content_list.get(turn).get(number.indexOf(Collections.min(number))).substring(1, 2).equals("X")) {
                            result = result + ((String) activity.getRobotRack().get(turn * 3).get("tile")).substring(1, 2);
                        } else {
                            if (Objects.equals(((String) activity.getRobotRack().get(turn * 3).get("tile")).substring(1, 2), tier_content_list.get(turn).get(number.indexOf(Collections.min(number))).substring(1, 2))) {
                                result = result + ((String) activity.getRobotRack().get(turn * 3).get("tile")).substring(1, 2);
                            } else {
                                result = result + "X";
                            }
                        }
                    }

                    activity.setRobotTile(turn * 3, "tile", result);
                    in.remove(in.indexOf(number.indexOf(Collections.min(number))));
                    result = "";

                    if (((String) activity.getRobotRack().get(turn * 3 + 2).get("tile")).equals("E")) {
                        result = tier_content_list.get(turn).get(number.indexOf(Collections.max(number)));
                    }
                    else {
                        if (((String) activity.getRobotRack().get(turn * 3 + 2).get("tile")).substring(0, 1).equals("X")) {
                            result = result + tier_content_list.get(turn).get(number.indexOf(Collections.max(number))).substring(0, 1);
                        } else if (tier_content_list.get(turn).get(number.indexOf(Collections.max(number))).substring(0, 1).equals("X")) {
                            result = result + ((String) activity.getRobotRack().get(turn * 3 + 2).get("tile")).substring(0, 1);
                        } else {
                            if (Objects.equals(((String) activity.getRobotRack().get(turn * 3 + 2).get("tile")).substring(0, 1), tier_content_list.get(turn).get(number.indexOf(Collections.max(number))).substring(0, 1))) {
                                result = result + ((String) activity.getRobotRack().get(turn * 3 + 2).get("tile")).substring(0, 1);
                            } else {
                                result = result + "X";
                            }
                        }
                        if (((String) activity.getRobotRack().get(turn * 3 + 2).get("tile")).substring(1, 2).equals("X")) {
                            result = result + tier_content_list.get(turn).get(number.indexOf(Collections.max(number))).substring(1, 2);
                        } else if (tier_content_list.get(turn).get(number.indexOf(Collections.max(number))).substring(1, 2).equals("X")) {
                            result = result + ((String) activity.getRobotRack().get(turn * 3 + 2).get("tile")).substring(1, 2);
                        } else {
                            if (Objects.equals(((String) activity.getRobotRack().get(turn * 3 + 2).get("tile")).substring(1, 2), tier_content_list.get(turn).get(number.indexOf(Collections.max(number))).substring(1, 2))) {
                                result = result + ((String) activity.getRobotRack().get(turn * 3 + 2).get("tile")).substring(1, 2);
                            } else {
                                result = result + "X";
                            }
                        }
                    }

                    activity.setRobotTile(turn * 3 + 2, "tile", result);
                    in.remove(in.indexOf(number.indexOf(Collections.max(number))));

                    result = "";
                    if (((String) activity.getRobotRack().get(turn * 3 + 1).get("tile")).equals("E")) {
                        result = tier_content_list.get(turn).get(in.get(0));
                    }
                    else {
                        if (((String) activity.getRobotRack().get(turn * 3 + 1).get("tile")).substring(0, 1).equals("X")) {
                            result = result + tier_content_list.get(turn).get(in.get(0)).substring(0, 1);
                        }
                        else if (tier_content_list.get(turn).get(in.get(0)).substring(0, 1).equals("X")) {
                            result = result + ((String) activity.getRobotRack().get(turn * 3 + 1).get("tile")).substring(0, 1);
                        }
                        else {
                            if (Objects.equals(((String) activity.getRobotRack().get(turn * 3 + 1).get("tile")).substring(0, 1), tier_content_list.get(turn).get(in.get(0)).substring(0, 1))){
                                result = result + ((String) activity.getRobotRack().get(turn * 3 + 1).get("tile")).substring(0, 1);
                            }
                            else{
                                result = result + "X";
                            }
                        }
                        if (((String) activity.getRobotRack().get(turn * 3 + 1).get("tile")).substring(1, 2).equals("X")) {
                            result = result + tier_content_list.get(turn).get(in.get(0)).substring(1, 2);
                        }
                        else if (tier_content_list.get(turn).get(in.get(0)).substring(1, 2).equals("X")) {
                            result = result + ((String) activity.getRobotRack().get(turn * 3 + 1).get("tile")).substring(1, 2);
                        }
                        else {
                            if (Objects.equals(((String) activity.getRobotRack().get(turn * 3 + 1).get("tile")).substring(1, 2), tier_content_list.get(turn).get(in.get(0)).substring(1, 2))) {
                                result = result + ((String) activity.getRobotRack().get(turn * 3 + 1).get("tile")).substring(1, 2);
                            } else {
                                result = result + "X";
                            }
                        }
                    }
                    activity.setRobotTile(turn * 3 + 1, "tile", result);
                } else {
                    for (Double distance : number) {
                        result = "";
                        int no = (int) Math.floor(distance);
                        if (no >= 3) {
                            no = 2;
                        }
                        if (((String) activity.getRobotRack().get(turn * 3 + no).get("tile")).equals("E")) {
                            result = tier_content_list.get(turn).get(number.indexOf(distance));
                        }
                        else {
                            if (((String) activity.getRobotRack().get(turn * 3 + no).get("tile")).substring(0, 1).equals("X")) {
                                result = result + tier_content_list.get(turn).get(number.indexOf(distance)).substring(0, 1);
                            } else if (tier_content_list.get(turn).get(number.indexOf(distance)).substring(0, 1).equals("X")) {
                                result = result + ((String) activity.getRobotRack().get(turn * 3 + no).get("tile")).substring(0, 1);
                            } else {
                                if (Objects.equals(((String) activity.getRobotRack().get(turn * 3 + no).get("tile")).substring(0, 1), tier_content_list.get(turn).get(number.indexOf(distance)).substring(0, 1))) {
                                    result = result + ((String) activity.getRobotRack().get(turn * 3 + no).get("tile")).substring(0, 1);
                                } else {
                                    result = result + "X";
                                }
                            }
                            if (((String) activity.getRobotRack().get(turn * 3 + no).get("tile")).substring(1, 2).equals("X")) {
                                result = result + tier_content_list.get(turn).get(number.indexOf(distance)).substring(1, 2);
                            } else if (tier_content_list.get(turn).get(number.indexOf(distance)).substring(1, 2).equals("X")) {
                                result = result + ((String) activity.getRobotRack().get(turn * 3 + no).get("tile")).substring(1, 2);
                            } else {
                                if (Objects.equals(((String) activity.getRobotRack().get(turn * 3 + no).get("tile")).substring(1, 2), tier_content_list.get(turn).get(number.indexOf(distance)).substring(1, 2))) {
                                    result = result + ((String) activity.getRobotRack().get(turn * 3 + no).get("tile")).substring(1, 2);
                                } else {
                                    result = result + "X";
                                }
                            }
                        }
                        activity.setRobotTile(turn * 3 + no, "tile", result);
                    }
                }
                turn++;
            }
        }

        for (ArrayList<Point[]> p : common_points_list) {
            Collections.sort(p, (card1, card2) -> {
                int result = Double.compare(card1[0].x, card2[0].x);
                return result;
            });
            int number = 0;

            for (Point[] po: p) {
                double length = tier_length_common.get(common_points_list.indexOf(p)) / 8;
                double left_distance = Math.sqrt(Math.pow(left_list_common.get(common_points_list.indexOf(p)).x - po[0].x, 2) + Math.pow(left_list_common.get(common_points_list.indexOf(p)).y - po[0].y, 2));
                number = (int) Math.round(left_distance / length);


                if (number >= 0 && number <= 7) {
                    rack.get(common_points_list.indexOf(p)).set(number, common_tier.get(po));
                }
            }
        }
        activity.printArray();
    }
}