package com.example.instgram;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {
    // LOG
    private static final String LOG_TAG = Register.class.getSimpleName();

    // EXTRA
    public static final String regExtraShortBio =
            "com.example.instgram.extra.REG_PROFILE_SHORT_BIO";

    // Firebase Auth
    private FirebaseAuth mAuth = null;

    // Firebase Cloud Storage
    FirebaseStorage mStorage = null;

    // Intent
    private Intent regProfileIntent = null;

    // Intent Request Code
    private static int regRequestCodeProfileCam = 100;

    // EditView
    private EditText regEditTextShortBio = null;
    private EditText regEditTextEmail = null;
    private EditText regEditTextPassword = null;
    private EditText regEditTextUserName = null;
    private EditText regEditTextConfirmPassword = null;

    // ImageView
    private ImageView regImageViewProfilePic = null;

    // Bitmap
    private Bitmap regBitmapProfilePic = null;

    // Utils
    private Utils utils = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        // Initialize utils
        utils = new Utils();
        // Initialize EditText
        regEditTextShortBio = findViewById(R.id.regBio);
        regEditTextEmail = findViewById(R.id.regEmail);
        regEditTextPassword = findViewById(R.id.regPassword);
        regEditTextUserName = findViewById(R.id.regUserName);
        regEditTextConfirmPassword = findViewById(R.id.regPasswordConfirm);
        // Initialize ImageView
        regImageViewProfilePic = findViewById(R.id.regProfilePic);
        // Initialize Intent
        regProfileIntent = new Intent(Register.this, ProfilePage.class);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // Initialize Firebase Cloud Storage
        mStorage = FirebaseStorage.getInstance();

        if(savedInstanceState != null) {
            if (savedInstanceState.getBoolean(BitmapDataFragment.EXISTED)) {
                BitmapDataFragment bitmapFragment = (BitmapDataFragment)getSupportFragmentManager()
                        .findFragmentByTag(BitmapDataFragment.TAG);
                regBitmapProfilePic = bitmapFragment.getData();
                regImageViewProfilePic.setImageBitmap(utils.toRoundBitMap(regBitmapProfilePic));
                getSupportFragmentManager().beginTransaction().remove(bitmapFragment).commit();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (regBitmapProfilePic != null) {
            getSupportFragmentManager().beginTransaction()
                    .add(BitmapDataFragment.newInstance(regBitmapProfilePic),
                            BitmapDataFragment.TAG)
                    .commit();
            outState.putBoolean(BitmapDataFragment.EXISTED, true);
        } else {
            outState.putBoolean(BitmapDataFragment.EXISTED, false);
        }
        super.onSaveInstanceState(outState);
    }

    private void regRouteToProfile() {
        regProfileIntent.putExtra(getString(R.string.extra_email),
                regEditTextEmail.getText().toString());
        startActivity(regProfileIntent);
    }

    private void syncDataToCloudStorage() {
        if (regBitmapProfilePic == null) {
            Log.w(LOG_TAG, "empty profile bitmap while uploaded.");
            regRouteToProfile();
        } else {
            String email = regEditTextEmail.getText().toString();
            String key = getString(R.string.cloud_storage_profile_pic) +
                    utils.processEmailString(email) + getString(R.string.pic_format_jpg);
            StorageReference regStorageRef = mStorage.getReference();
            StorageReference regProfilePicRef = regStorageRef.child(key);
            ByteArrayOutputStream blob = new ByteArrayOutputStream();
            regBitmapProfilePic.compress(Bitmap.CompressFormat.JPEG, 100, blob);
            byte[] data = blob.toByteArray();
            UploadTask uploadTask = regProfilePicRef.putBytes(data);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.w(LOG_TAG, "Failed to upload profile pic to cloud storage",
                            exception);
                    Toast.makeText(Register.this,
                            "Cloud storage upload failed", Toast.LENGTH_SHORT).show();
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Log.d(LOG_TAG, "Cloud storage upload succeed.");
                    regRouteToProfile();
                }
            });
        }
    }


    private void syncDataToFirestore() {
        String userName = regEditTextUserName.getText().toString();
        String shortBio = regEditTextShortBio.getText().toString();
        String email = regEditTextEmail.getText().toString();
        String regDocName = getString(R.string.firestore_category_users) +
                utils.processEmailString(email);
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
                    syncDataToCloudStorage();
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

        if(signInPassword.isEmpty() || signInUserName.isEmpty() || signInEmail.isEmpty()) {
            Log.w(LOG_TAG, "createUserWithEmail:failure input validation: null string");
            Toast.makeText(Register.this,
                    "Empty user input", Toast.LENGTH_SHORT).show();
            return;
        }

        if (signInPassword.equals(signInConfirmPassword)) {
            mAuth.createUserWithEmailAndPassword(signInEmail.toLowerCase(), signInPassword)
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

    public void regUploadProfilePic(View view) {
        Intent cameraIntent = new Intent();
        cameraIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, regRequestCodeProfileCam);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == regRequestCodeProfileCam && data != null) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            if (bitmap != null) {
                regBitmapProfilePic = bitmap;
                regImageViewProfilePic.setImageBitmap(utils.toRoundBitMap(bitmap));
            }
        } else {
            Log.w(LOG_TAG, "set profile picture on activity failed.");
        }

    }
}