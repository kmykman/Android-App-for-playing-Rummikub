package com.fyp.rummikub;

import android.app.Dialog;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class StartPageFragment extends Fragment {

    static private String LOG_TAG = StartPageFragment.class.getSimpleName();
    ImageView robot_image, human_image;
    RelativeLayout robot, human;
    Button random;
    View robot_view, human_view;
    TextView yes, no, ok;
    GameActivity activity;

    public StartPageFragment() {
    }

    public static StartPageFragment newInstance() {
        StartPageFragment fragment = new StartPageFragment();
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
        View view = inflater.inflate(R.layout.fragment_start_page, container, false);

        activity = (GameActivity) getActivity();

        Log.i(LOG_TAG, String.valueOf(activity.getRobotRack()));

        robot = view.findViewById(R.id.robot_layout);
        human = view.findViewById(R.id.human_layout);
        random = view.findViewById(R.id.random);
        random.setText("Random");
        robot_view = view.findViewById(R.id.robot_view);
        human_view = view.findViewById(R.id.human_view);
        robot_image = view.findViewById(R.id.robot);
        human_image = view.findViewById(R.id.human);

        robot.setOnClickListener(this::robotFirst);
        robot_image.setOnClickListener(this::robotFirst);
        human.setOnClickListener(this::humanFirst);
        human_image.setOnClickListener(this::humanFirst);
        random.setOnClickListener(this::randomFirst);

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

    private void randomFirst(View view) {
        if (random.getText().toString().equals("Random")){
            Animation anim_robot = new AlphaAnimation(0.0f, 1.0f);
            anim_robot.setDuration(50);
            anim_robot.setStartOffset(20);
            anim_robot.setRepeatMode(Animation.REVERSE);
            anim_robot.setRepeatCount(10);
            robot_view.startAnimation(anim_robot);

            Animation anim_human = new AlphaAnimation(1.0f, 0.0f);
            anim_human.setDuration(50);
            anim_human.setStartOffset(20);
            anim_human.setRepeatMode(Animation.REVERSE);
            anim_human.setRepeatCount(10);
            human_view.startAnimation(anim_human);

            List<String> list = new ArrayList<>(Arrays.asList("robot", "human"));
            String first = list.get(new Random().nextInt(list.size()));

            human.setOnClickListener(null);
            robot.setOnClickListener(null);
            robot_image.setOnClickListener(null);
            human_image.setOnClickListener(null);
            random.setText("Start");
            if (Objects.equals(first, "robot")){
                robot_view.setVisibility(View.INVISIBLE);
                human_view.setVisibility(View.VISIBLE);
            }
            else if (Objects.equals(first, "human")){
                human_view.setVisibility(View.INVISIBLE);
                robot_view.setVisibility(View.VISIBLE);
            }
        }
        else if (random.getText().toString() == "Start"){
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.slide_in, R.anim.slide_out, R.anim.enter_top, R.anim.exit_bottom);

            if (robot_view.getVisibility() == View.VISIBLE && human_view.getVisibility() == View.INVISIBLE) {
                HumanTurnFragment HumanTurnFragment = new HumanTurnFragment();
                transaction.replace(R.id.frag_con, HumanTurnFragment).commit();
            }
            else if (robot_view.getVisibility() == View.INVISIBLE && human_view.getVisibility() == View.VISIBLE) {
                RobotTurnFragment RobotTurnFragment = new RobotTurnFragment();
                transaction.replace(R.id.frag_con, RobotTurnFragment).commit();
            }
        }
    }

    private void humanFirst(View view) {
        if (robot_view.getVisibility() == View.INVISIBLE && human_view.getVisibility() == View.INVISIBLE) {
            robot_view.setVisibility(View.VISIBLE);
            human_view.setVisibility(View.INVISIBLE);
            random.setText("Start");
        }
        else {
            robot_view.setVisibility(View.INVISIBLE);
            human_view.setVisibility(View.INVISIBLE);
            random.setText("Random");
        }
    }

    private void robotFirst(View view) {
        if (robot_view.getVisibility() == View.INVISIBLE && human_view.getVisibility() == View.INVISIBLE) {
            robot_view.setVisibility(View.INVISIBLE);
            human_view.setVisibility(View.VISIBLE);
            random.setText("Start");
        }
        else {
            robot_view.setVisibility(View.INVISIBLE);
            human_view.setVisibility(View.INVISIBLE);
            random.setText("Random");
        }
    }
}