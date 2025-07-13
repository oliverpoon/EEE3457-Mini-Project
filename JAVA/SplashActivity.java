package com.example.project.Activitis;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.project.R;
import pl.droidsonroids.gif.GifImageView;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 3000; // 3 seconds
    private ProgressBar progressBar;
    private TextView loadingText;
    private GifImageView gifImageView;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initViews();
        startProgressAnimation();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        loadingText = findViewById(R.id.loadingText);
        gifImageView = findViewById(R.id.splashGif);
        handler = new Handler();

    }

    private void startProgressAnimation() {
        final int[] progress = {0};
        final String[] loadingTexts = {
                "Loading weather data...",
                "Connecting to HK Observatory...",
                "Getting GPS position...",
                "Almost Almost Almost..."
        };

        Runnable progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (progress[0] <= 100) {
                    progressBar.setProgress(progress[0]);

                    // Update loading text
                    if (progress[0] < 25) {
                        loadingText.setText(loadingTexts[0]);
                    } else if (progress[0] < 50) {
                        loadingText.setText(loadingTexts[1]);
                    } else if (progress[0] < 75) {
                        loadingText.setText(loadingTexts[2]);
                    } else {
                        loadingText.setText(loadingTexts[3]);
                    }

                    progress[0] += 2;
                    handler.postDelayed(this, 60); // Update every 60ms
                } else {
                    // Progress completed, jump to the main activity
                    navigateToMainActivity();
                }
            }
        };

        handler.post(progressRunnable);
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Finish SplashActivity

        // Add fade-in and fade-out animation effects
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        // GifImageView doesn't need manual cleanup, system handles it automatically
    }

    @Override
    public void onBackPressed() {
        // Prevent back key press on splash screen
        // Not called super.onBackPressed()
    }
}