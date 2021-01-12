package com.example.instgram;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.errorprone.annotations.ForOverride;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    // Log
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    // EditText
    private EditText mainEditTextEmail = null;
    private EditText getMainEditTextPassword = null;

    // Intent
    private Intent mainSignUpIntent = null;
    private Intent mainProfileIntent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Initialize EditText
        mainEditTextEmail = findViewById(R.id.mainEmail);
        getMainEditTextPassword = findViewById(R.id.mainPassword);
        // Initialize Intent
        mainSignUpIntent = new Intent(MainActivity.this, Register.class);
        mainProfileIntent = new Intent(MainActivity.this, ProfilePage.class);
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

    public void mainLogIn(View view) {
        String loginEmail = mainEditTextEmail.getText().toString();
        String loginPassword = getMainEditTextPassword.getText().toString();
        mAuth.signInWithEmailAndPassword(loginEmail.toLowerCase(), loginPassword)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(LOG_TAG, "Log in successfully in mainLogIn");
                            mainProfileIntent.putExtra(getString(R.string.extra_email), loginEmail);
                            startActivity(mainProfileIntent);
                        } else {
                            Log.d(LOG_TAG, "Log in failed", task.getException());
                            Toast.makeText(MainActivity.this,
                                    "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void mainSignUp(View view) {
        Log.d(LOG_TAG, "Sign up button being clicked");
        startActivity(mainSignUpIntent);
    }
}