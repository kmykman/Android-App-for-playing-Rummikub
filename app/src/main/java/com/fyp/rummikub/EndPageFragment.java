package com.fyp.rummikub;

import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Objects;

public class EndPageFragment extends Fragment {
    View view;
    Button home_page;
    TextView result;

    String winner = "";

    public EndPageFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_end_page, container, false);

        home_page = view.findViewById(R.id.home_page);
        result = view.findViewById(R.id.result);

        home_page.setOnClickListener(this::goHome);

        winner = ((GameActivity)getActivity()).getWinner();

        if (Objects.equals(winner, "robot")){
            result.setText("You lose!");
            result.setTextColor(Color.parseColor("#FA0000"));

        }
        else if (Objects.equals(winner, "human")){
            result.setText("You win!");
            result.setTextColor(Color.parseColor("#009C38"));
        }
        else if (Objects.equals(winner, "tie")){
            result.setText("Tie!");
            result.setTextColor(Color.parseColor("#3633FF"));
        }

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);
        return view;
    }

    private void goHome(View view) {
        getActivity().finish();
    }
}