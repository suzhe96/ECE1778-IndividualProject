package com.example.instgram;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    // Log
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    // EditText
    private EditText mainEditTextEmail = null;
    private EditText getMainEditTextPassword = null;

    // ProgressBar
    private ProgressBar mainProgressBarLogIn = null;

    // Button
    private Button mainButtonLogIn = null;

    // Intent
    private Intent mainSignUpIntent = null;
    private Intent mainProfileIntent = null;

    // Util
    private Utils utils = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Log In");
        setContentView(R.layout.activity_main);
        // Initialize EditText
        mainEditTextEmail = findViewById(R.id.mainEmail);
        getMainEditTextPassword = findViewById(R.id.mainPassword);
        // Initialize ProgressBar
        mainProgressBarLogIn = findViewById(R.id.mainLoadingIconLogIn);
        // Initialize Button
        mainButtonLogIn = findViewById(R.id.mainLoginButton);
        // Initialize Intent
        mainSignUpIntent = new Intent(MainActivity.this, Register.class);
        mainProfileIntent = new Intent(MainActivity.this, ProfilePage.class);
        // Initialize Utils
        utils = new Utils();
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(LOG_TAG, "User has signed in already");
            startActivity(mainProfileIntent);
        }
    }

    private void setVisibilityForLoading(Boolean loadingVisble) {
        if (loadingVisble) {
            mainProgressBarLogIn.setVisibility(View.VISIBLE);
            mainButtonLogIn.setVisibility(View.GONE);
        } else {
            mainProgressBarLogIn.setVisibility(View.GONE);
            mainButtonLogIn.setVisibility(View.VISIBLE);
        }
    }

    public void mainLogIn(View view) {
        String loginEmail = mainEditTextEmail.getText().toString();
        String loginPassword = getMainEditTextPassword.getText().toString();
        if (loginEmail.isEmpty() || loginPassword.isEmpty()) {
            Log.w(LOG_TAG, "Empty user input");
            Toast.makeText(MainActivity.this,
                    "ERROR_EMPTY_INPUT", Toast.LENGTH_SHORT).show();
            return;
        }

        // set loading icon visable
        setVisibilityForLoading(true);
        mAuth.signInWithEmailAndPassword(loginEmail.toLowerCase(), loginPassword)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(LOG_TAG, "Log in successfully in mainLogIn");
                            mainProfileIntent.putExtra(getString(R.string.extra_email), loginEmail);
                            startActivity(mainProfileIntent);
                        } else {
                            Log.w(LOG_TAG, "Log in failed", task.getException());
                            setVisibilityForLoading(false);
                            Toast.makeText(MainActivity.this,
                                    utils.fireAuthExceptionCode(task.getException()),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void mainSignUp(View view) {
        Log.d(LOG_TAG, "Sign up button being clicked");
        startActivity(mainSignUpIntent);
    }
}