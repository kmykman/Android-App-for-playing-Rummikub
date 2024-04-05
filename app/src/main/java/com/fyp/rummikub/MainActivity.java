package com.fyp.rummikub;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;


public class MainActivity extends AppCompatActivity {
    Button control, game;
    static private String LOG_TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        control = findViewById(R.id.control);
        game = findViewById(R.id.game);
    }

    public void goControlActivity(View view) {
        Log.e(LOG_TAG, "Press goControlActivity...");
        startActivity(new Intent(this, ControlActivity.class));
    }

    public void goGameActivity(View view) {
        Log.e(LOG_TAG, "Press goGameActivity...");
        startActivity(new Intent(this, GameActivity.class));
    }
}