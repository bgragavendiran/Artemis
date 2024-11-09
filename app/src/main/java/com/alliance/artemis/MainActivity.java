package com.alliance.artemis;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.alliance.artemis.home.HomeActivity;
import com.alliance.artemis.utils.OtpEditText;

public class MainActivity extends AppCompatActivity {
    private Spinner spinnerOutposts, spinnerBreederCode;
    private OtpEditText otpEditText;
    private Button buttonLogin;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Initialize UI components
        spinnerOutposts = findViewById(R.id.spinner_outposts);
        spinnerBreederCode = findViewById(R.id.spinner_breeder_code);
        otpEditText = findViewById(R.id.et_otp);
        buttonLogin = findViewById(R.id.button_login);

        // Set up dropdowns
        setupOutpostsDropdown();
        setupBreederCodeDropdown();

        // Set up login button click listener
        buttonLogin.setOnClickListener(view -> handleLogin());
    }


    private void setupOutpostsDropdown() {
        String[] outposts = {"Outpost A", "Outpost B", "Outpost C"}; // Example data
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, outposts);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOutposts.setAdapter(adapter);
    }

    private void setupBreederCodeDropdown() {
        String[] breederCodes = {"Code 123", "Code 456", "Code 789"}; // Example data
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, breederCodes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBreederCode.setAdapter(adapter);
    }

    private void handleLogin() {
        String selectedOutpost = spinnerOutposts.getSelectedItem().toString();
        String selectedBreederCode = spinnerBreederCode.getSelectedItem().toString();
        String otp = otpEditText.getText().toString();

        // Example validation logic
        if (otp.length() == 4) {
            // TODO: Implement actual login logic here (e.g., API call)
            Intent intent =new Intent(MainActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Please enter a valid 4-digit passcode", Toast.LENGTH_SHORT).show();
        }
    }
}