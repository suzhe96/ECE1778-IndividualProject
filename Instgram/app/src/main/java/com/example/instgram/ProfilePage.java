package com.example.instgram;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfilePage extends AppCompatActivity {
    // LOG
    private static final String LOG_TAG = ProfilePage.class.getSimpleName();

    // Utils
    private Utils utils = null;

    // Intent
    private Intent profileIntentSignOut =  null;

    // TextView
    private TextView profileTextViewShortBio = null;
    private TextView profileTextViewDisplayName = null;

    // ImageView
    private ImageView profileImageViewProfilePic = null;

    // ProgressBar
    private ProgressBar profileProgressBarLoading = null;

    // Firebase Auth
    private FirebaseAuth mAuth;

    // Firebase Cloud Storage
    FirebaseStorage mStorage = null;

    // Bitmap
    private Bitmap profileBitmapProfilePic = null;

    // 1M
    private static final long ONE_MEGABYTE = 1024 * 1024;


    private void setVisibilityForDone(Boolean isDone) {
        if (isDone) {
            profileTextViewShortBio.setVisibility(View.VISIBLE);
            profileTextViewDisplayName.setVisibility(View.VISIBLE);
            profileImageViewProfilePic.setVisibility(View.VISIBLE);
            profileProgressBarLoading.setVisibility(View.GONE);
        } else {
            profileTextViewShortBio.setVisibility(View.INVISIBLE);
            profileTextViewDisplayName.setVisibility(View.INVISIBLE);
            profileImageViewProfilePic.setVisibility(View.INVISIBLE);
            profileProgressBarLoading.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_page);
        setTitle("Profile");

        // Initialize utils
        utils = new Utils();

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Firebase Cloud Storage
        mStorage = FirebaseStorage.getInstance();

        // Initialize TextView
        profileTextViewShortBio = findViewById(R.id.profileShortBio);
        profileTextViewDisplayName = findViewById(R.id.profileDisplayName);

        // Initialize ImageView
        profileImageViewProfilePic = findViewById(R.id.profileProfilePic);

        // Initialize ProgressBar
        profileProgressBarLoading = findViewById(R.id.profileLoading);

        // Initialize Intent
        profileIntentSignOut = new Intent(this, MainActivity.class);

        // Visibility setting for loading
        setVisibilityForDone(false);


        if(savedInstanceState != null) {
            if (savedInstanceState.getBoolean(BitmapDataFragment.EXISTED)) {
                BitmapDataFragment bitmapFragment = (BitmapDataFragment)getSupportFragmentManager()
                        .findFragmentByTag(BitmapDataFragment.TAG);
                profileBitmapProfilePic = bitmapFragment.getData();
                profileImageViewProfilePic.setImageBitmap(
                        utils.toRoundBitMap(utils.cropProfileBitmap(profileBitmapProfilePic)));
                getSupportFragmentManager().beginTransaction().remove(bitmapFragment).commit();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.address_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_signout:
                Log.d(LOG_TAG, "Sign out button being clicked");
                mAuth.signOut();
                startActivity(profileIntentSignOut);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(LOG_TAG, "No current user available.");
        }

        // User Email
        String profileUserEmail = currentUser.getEmail();
        if (profileUserEmail == null) {
            Log.w(LOG_TAG, "Failed to get email from current user.");
        }
        String profileDocName = getString(R.string.firestore_category_users) +
                utils.processEmailString(profileUserEmail);
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
                    Log.w(LOG_TAG, "get profile user data from firestore failed.", error);
                }
            }
        });

        String key = getString(R.string.cloud_storage_profile_pic) +
                utils.processEmailString(profileUserEmail) + getString(R.string.pic_format_jpg);
        StorageReference profileStorageRef = mStorage.getReference();
        StorageReference profilePicRef = profileStorageRef.child(key);
        profilePicRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                profileImageViewProfilePic.setImageBitmap(utils.toRoundBitMap(
                        utils.cropProfileBitmap(bitmap)));

                // Set Visibility
                setVisibilityForDone(true);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.w(LOG_TAG, "get profile picture from cloud storage failed.", exception);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (profileBitmapProfilePic != null) {
            getSupportFragmentManager().beginTransaction()
                    .add(BitmapDataFragment.newInstance(profileBitmapProfilePic),
                            BitmapDataFragment.TAG)
                    .commit();
            outState.putBoolean(BitmapDataFragment.EXISTED, true);
        } else {
            outState.putBoolean(BitmapDataFragment.EXISTED, false);
        }
        super.onSaveInstanceState(outState);
    }

    public void profileSignOut(View view) {
        Log.d(LOG_TAG, "Sign out button being clicked");
        mAuth.signOut();
        startActivity(profileIntentSignOut);
    }

    public void profileImage(View view) {
        Toast.makeText(this, "Upload profile pic", Toast.LENGTH_SHORT).show();
    }
}