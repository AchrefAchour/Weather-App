package essths.li3.weatherapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Afficher pendant 3 secondes puis passer à MainActivity
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, 3000); // 3 secondes
    }
}