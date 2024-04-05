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
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class HumanTurnFragment extends Fragment {
    static private String LOG_TAG = HumanTurnFragment.class.getSimpleName();
    View view;
    RelativeLayout turnEnd_layout;
    TextView yes, no, ok, done, turn_end;
    ImageView champion, ok_sign;
    GameActivity activity;

    public HumanTurnFragment() {}

    public static HumanTurnFragment newInstance() {
        HumanTurnFragment fragment = new HumanTurnFragment();
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

        activity = (GameActivity) getActivity();

        view = inflater.inflate(R.layout.fragment_human_turn, container, false);
        turnEnd_layout = view.findViewById(R.id.turnEnd_layout);
        turn_end = view.findViewById(R.id.turn_end);
        ok_sign = view.findViewById(R.id.ok_sign);
        champion = view.findViewById(R.id.champion);
        done = view.findViewById(R.id.done);


        turnEnd_layout.setOnClickListener(this::turnEnd);
        turn_end.setOnClickListener(this::turnEnd);
        ok_sign.setOnClickListener(this::turnEnd);
        champion.setOnClickListener(this::goEndPage);
        done.setOnClickListener(this::goEndPage);


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


    private void goEndPage(View view) {
        activity.setWinner("human");
        EndPageFragment EndPageFragment = new EndPageFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in, R.anim.slide_out, R.anim.enter_top, R.anim.exit_bottom);
        transaction.addToBackStack(null);
        transaction.replace(R.id.frag_con, EndPageFragment).commit();
    }

    private void turnEnd(View view) {
        Log.e(LOG_TAG, "press turnEnd");
        goRobotTurnPage();
    }

    void goRobotTurnPage(){
        RobotTurnFragment RobotTurnFragment = new RobotTurnFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in, R.anim.slide_out, R.anim.enter_top, R.anim.exit_bottom);
        transaction.addToBackStack(null);
        transaction.replace(R.id.frag_con, RobotTurnFragment).commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.e(LOG_TAG, "onDestroy()");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.e(LOG_TAG, "onStop()");
    }
}