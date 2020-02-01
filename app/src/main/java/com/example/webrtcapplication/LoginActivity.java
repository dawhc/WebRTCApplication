/**
 * LoginActivity.class
 * Author: Cui Donghang
 * Version: 1.0
 * Date: 2019.12.20
 */
package com.example.webrtcapplication;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Login Activity
 */
public class LoginActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private Button loginButton;
    private ProgressBar loadingProgressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameEditText = findViewById(R.id.username);
        loginButton = findViewById(R.id.login);
        loadingProgressBar = findViewById(R.id.login_loading);

        // Set Username edit text watcher
        usernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals("")) {
                    loginButton.setEnabled(false);
                }
                else {
                    loginButton.setEnabled(true);
                }
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                try {
                    // start MainActivity.class
                    String username = usernameEditText.getText().toString();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class)
                            .putExtra("username", username));
                }
                catch (java.lang.Exception e){
                    Toast.makeText(getApplicationContext(), R.string.login_failed, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadingProgressBar.setVisibility(View.GONE);
    }
}
