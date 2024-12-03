package com.movesense.showcaseapp.section_01_movesense.tests;


import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.movesense.showcaseapp.R;
import com.movesense.showcaseapp.section_01_movesense.MovesenseActivity;

public class info extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.info);

        Button buttonNext = findViewById(R.id.btn_next);
        EditText emailInput = findViewById(R.id.et_input2);
        EditText phoneInput = findViewById(R.id.et_input3);

        buttonNext.setOnClickListener(v -> {
            String email = emailInput.getText().toString();
            String phone = phoneInput.getText().toString();

            if (isValidEmail(email) && isValidPhone(phone)) {
                // Go to the next activity
                Intent intent = new Intent(info.this, MovesenseActivity.class);
                startActivity(intent);
            } else {
                // Show error message
                Toast toast = Toast.makeText(info.this, "Tarkista syöttämäsi tiedot!", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 100);
                toast.show();
            }
        });
    }

    private boolean isValidEmail(String email) {
        // Checks if the email contains "@" and "."
        return email.contains("@") && email.contains(".");
    }

    private boolean isValidPhone(String phone) {
        // Checks if the phone number starts with "+" and contains only digits afterwards
        return phone.startsWith("+") && phone.substring(1).matches("\\d+");
    }
}
