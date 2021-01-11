package com.example.instgram;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {
    // LOG
    private static final String LOG_TAG = Register.class.getSimpleName();

    // EXTRA
    public static final String regExtraShortBio =
            "com.example.instgram.extra.REG_PROFILE_SHORT_BIO";

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    // Intent
    private Intent regProfileIntent = null;

    // EditView
    private EditText regEditTextShortBio = null;
    private EditText regEditTextEmail = null;
    private EditText regEditTextPassword = null;
    private EditText regEditTextUserName = null;
    private EditText regEditTextConfirmPassword = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        // Initialize EditText
        regEditTextShortBio = findViewById(R.id.regBio);
        regEditTextEmail = findViewById(R.id.regEmail);
        regEditTextPassword = findViewById(R.id.regPassword);
        regEditTextUserName = findViewById(R.id.regUserName);
        regEditTextConfirmPassword = findViewById(R.id.regPasswordConfirm);
        // Initialize Intent
        regProfileIntent = new Intent(Register.this, ProfilePage.class);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
    }

    private boolean validateUserInput(String password, String username, String email) {
        if (password.isEmpty() || username.isEmpty() || email.isEmpty()) {
            return false;
        }
        return true;
    }

    private void syncDataToFirestore() {
        String userName = regEditTextUserName.getText().toString();
        String shortBio = regEditTextShortBio.getText().toString();
        String email = regEditTextEmail.getText().toString();
        String regDocName = getString(R.string.firestore_category_users) + email;
        DocumentReference regDocRef = FirebaseFirestore.getInstance().document(regDocName);

        Map<String, Object> userData = new HashMap<String, Object>();
        userData.put(getString(R.string.firestore_category_users_doc_username),
                userName);
        userData.put(getString(R.string.firestore_category_users_doc_shortbio),
                shortBio);
        regDocRef.set(userData).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(LOG_TAG, "succeeded to sync user data to firestore.");
                    regProfileIntent.putExtra(getString(R.string.extra_email), email);
                    startActivity(regProfileIntent);
                } else {
                    Log.w(LOG_TAG, "failed to sync user data to firestore",
                            task.getException());
                    Toast.makeText(Register.this,
                            "Sync data failed.", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    public void regSignUp(View view) {
        String signInEmail = regEditTextEmail.getText().toString();
        String signInPassword = regEditTextPassword.getText().toString();
        String signInConfirmPassword = regEditTextConfirmPassword.getText().toString();
        String signInUserName = regEditTextUserName.getText().toString();

        if(!validateUserInput(signInPassword, signInUserName, signInEmail)) {
            Log.w(LOG_TAG, "createUserWithEmail:failure input validation");
            Toast.makeText(Register.this,
                    "User input validation failed.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (signInPassword.equals(signInConfirmPassword)) {
            mAuth.createUserWithEmailAndPassword(signInEmail, signInPassword)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Log.d(LOG_TAG, "Log in successfully in mainLogIn");
                                syncDataToFirestore();
                            } else {
                                Log.w(LOG_TAG, "createUserWithEmail:failure",
                                        task.getException());
                                Toast.makeText(Register.this,
                                        "Authentication failed.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            Log.w(LOG_TAG, "Inconsistent password in regSignUp.");
            Toast.makeText(Register.this, "Inconsistent password.",
                    Toast.LENGTH_SHORT).show();
        }

    }
}