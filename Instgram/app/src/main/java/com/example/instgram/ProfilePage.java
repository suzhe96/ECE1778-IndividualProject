package com.example.instgram;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class ProfilePage extends AppCompatActivity {
    // LOG
    private static final String LOG_TAG = ProfilePage.class.getSimpleName();

    // Intent
    private Intent profileIntentSignOut =  null;
    private Intent profileIntentReceived = null;

    // TextView
    private TextView profileTextViewShortBio = null;
    private TextView profileTextViewDisplayName = null;

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_page);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize TextView
        profileTextViewShortBio = findViewById(R.id.profileShortBio);
        profileTextViewDisplayName = findViewById(R.id.profileDisplayName);

        // Initialize Intent
        profileIntentSignOut = new Intent(this, MainActivity.class);
        profileIntentReceived = getIntent();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (profileIntentReceived == null) {
            Log.w(LOG_TAG, "profileIntentReceived is null.");
            return;
        }
        String email = profileIntentReceived.getStringExtra(getString(R.string.extra_email));
        if (email == null) {
            Log.w(LOG_TAG, "no userName being passed from extra.");
            return;
        }

        String profileDocName = getString(R.string.firestore_category_users) + email;
        DocumentReference profileDocRef = FirebaseFirestore.getInstance().document(profileDocName);
        profileDocRef.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot docSnapShot, FirebaseFirestoreException error) {
                if (docSnapShot.exists()) {
                    profileTextViewDisplayName.setText(docSnapShot.getString(
                            getString(R.string.firestore_category_users_doc_username)
                    ));
                    profileTextViewShortBio.setText(docSnapShot.getString(
                            getString(R.string.firestore_category_users_doc_shortbio)
                    ));
                } else if (error != null) {
                    Log.w(LOG_TAG, "profile document get user data failed", error);
                }
            }
        });
    }


    public void profileSignOut(View view) {
        Log.d(LOG_TAG, "Sign out button being clicked");
        mAuth.signOut();
        startActivity(profileIntentSignOut);
    }
}