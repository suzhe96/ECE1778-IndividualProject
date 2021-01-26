package com.example.instgram;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
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
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.LinkedList;
import java.util.Map;

public class ProfilePage extends AppCompatActivity {
    // LOG
    private static final String LOG_TAG = ProfilePage.class.getSimpleName();

    // Utils
    private Utils utils = null;

    // User Email
    private String profileUserEmail = null;

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
    private Bitmap profileBitmapCameraBuffer = null;

    // Fragment
    private BitmapDataFragment profileBitmapProfileFrag = null;
    private TextDataFragment profileTextProfileFrag = null;
    private ContentImgListFragment profileContentImgFrag = null;

    // AsyncCallHandler On Start
    private AsyncCallHandler profileAsyncHdlOnLoadContentImg = null;

    // 1M
    private static final long ONE_MEGABYTE = 1024 * 1024;

    // Intent Request Code
    private static final int profileRequestCodeContentImgCam = 100;

    // RecycleView for ContentImg
    private LinkedList<Bitmap> profileBitmapListContentImg = null;
    private RecyclerView mRecyclerView;
    private ContentImgAdapter mAdapter;


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

    private void loadContentImgFromStorage(StorageReference ref) {
        ref.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                profileContentImgFrag.setData(bytes);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                profileBitmapListContentImg.addFirst(
                        utils.cropProfileBitmap(bitmap, true));
                mRecyclerView.getAdapter().notifyItemInserted(0);
                mRecyclerView.smoothScrollToPosition(0);
                profileAsyncHdlOnLoadContentImg.addSuccessfulTask();
                if (profileAsyncHdlOnLoadContentImg.waitForAllComplete()) {
                    setVisibilityForDone(true);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.w(LOG_TAG, "get content image from cloud storage failed.", exception);
            }
        });
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

        // Initialize UserEmail
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(LOG_TAG, "No current user available on create.");
        }
        profileUserEmail = currentUser.getEmail();

        // Initialize RecycleView Bitmap LinkedList
        profileBitmapListContentImg = new LinkedList<>();

        // Create recycler view.
        mRecyclerView = findViewById(R.id.recyclerview);
        // Create an adapter and supply the data to be displayed.
        mAdapter = new ContentImgAdapter(this, profileBitmapListContentImg);
        // Connect the adapter with the recycler view.
        mRecyclerView.setAdapter(mAdapter);
        // Give the recycler view a default layout manager.
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        // Retain fragment instance
        profileRetainingFragment();
        profileSetRetainingFrag();
    }

    private void profileRetainingFragment() {
        // find the retained fragment on activity restarts
        FragmentManager fragmentManager = getSupportFragmentManager();
        this.profileBitmapProfileFrag = (BitmapDataFragment) fragmentManager
                .findFragmentByTag(BitmapDataFragment.TAG);
        this.profileTextProfileFrag = (TextDataFragment) fragmentManager
                .findFragmentByTag(TextDataFragment.TAG);
        this.profileContentImgFrag = (ContentImgListFragment) fragmentManager
                .findFragmentByTag(ContentImgListFragment.TAG);
        // create the fragment
        if (this.profileBitmapProfileFrag == null) {
            this.profileBitmapProfileFrag = new BitmapDataFragment();
            fragmentManager.beginTransaction()
                    .add(this.profileBitmapProfileFrag, BitmapDataFragment.TAG)
                    .commit();
        }
        if (this.profileTextProfileFrag == null) {
            this.profileTextProfileFrag = new TextDataFragment();
            fragmentManager.beginTransaction()
                    .add(this.profileTextProfileFrag, TextDataFragment.TAG)
                    .commit();
        }
        if (this.profileContentImgFrag == null) {
            this.profileContentImgFrag = new ContentImgListFragment();
            fragmentManager.beginTransaction()
                    .add(this.profileContentImgFrag, ContentImgListFragment.TAG)
                    .commit();
        }
    }

    private void profileSetRetainingFrag() {
        if (this.profileBitmapProfileFrag == null ||
                this.profileTextProfileFrag == null || this.profileContentImgFrag == null) {
            setVisibilityForDone(false);
            return;
        }
        Bitmap bitmap = this.profileBitmapProfileFrag.getData();
        Map<String, String> text = this.profileTextProfileFrag.getData();
        LinkedList<byte[]> byteArrList = this.profileContentImgFrag.getData();
        if (bitmap == null || text == null || byteArrList == null) {
            setVisibilityForDone(false);
            return;
        }
        profileImageViewProfilePic.setImageBitmap(utils.toRoundBitMap(
                utils.cropProfileBitmap(bitmap, false)));
        profileTextViewDisplayName.setText(
                text.get(getString(R.string.firestore_category_users_doc_username)));
        profileTextViewShortBio.setText(
                text.get(getString(R.string.firestore_category_users_doc_shortbio)));
        for (int i = 0; i < byteArrList.size(); ++i) {
            byte[] bytes = byteArrList.get(i);
            Bitmap bitmapContentImg = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            profileBitmapListContentImg.addLast(
                    utils.cropProfileBitmap(bitmapContentImg, true));
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
            case R.id.action_takepics:
                Log.d(LOG_TAG, "Take pics button being clicked");

                Intent cameraIntent = new Intent();
                cameraIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, profileRequestCodeContentImgCam);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (this.profileBitmapProfileFrag.isDataExisted() ||
                this.profileTextProfileFrag.isDataExisted() ||
                this.profileContentImgFrag.isDataExisted()) {
            return;
        }
        // User Email
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
                    String displayName = docSnapShot.getString(
                            getString(R.string.firestore_category_users_doc_username)
                    );
                    String shortBio = docSnapShot.getString(
                            getString(R.string.firestore_category_users_doc_shortbio)
                    );
                    profileTextProfileFrag.setData(
                            getString(R.string.firestore_category_users_doc_username), displayName);
                    profileTextProfileFrag.setData(
                            getString(R.string.firestore_category_users_doc_shortbio), shortBio);
                    profileTextViewDisplayName.setText(displayName);
                    profileTextViewShortBio.setText(shortBio);
                } else if (error != null) {
                    Log.w(LOG_TAG, "get profile user data from firestore failed.", error);
                }
            }
        });

        String key = getString(R.string.cloud_storage_profile_pic) +
                utils.processEmailString(profileUserEmail);
        StorageReference profileStorageRef = mStorage.getReference();
        StorageReference profilePicRef = profileStorageRef.child(key);
        profilePicRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                profileBitmapProfileFrag.setData(bitmap);

                profileImageViewProfilePic.setImageBitmap(utils.toRoundBitMap(
                        utils.cropProfileBitmap(bitmap, false)));
                setVisibilityForDone(true);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.w(LOG_TAG, "get profile picture from cloud storage failed.", exception);
            }
        });

        // query section for content image
        String contentImgKey = getString(R.string.cloud_storage_content_img) +
                utils.processEmailString(profileUserEmail) + "/";
        StorageReference listRef = mStorage.getReference().child(contentImgKey);
        listRef.listAll()
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {
                        profileAsyncHdlOnLoadContentImg =
                                new AsyncCallHandler(listResult.getItems().size());
                        for (StorageReference item : listResult.getItems()) {
                            loadContentImgFromStorage(item);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(LOG_TAG, "get listRef contentImage error", e);
                    }
                });
    }



    public void profileImage(View view) {
        Toast.makeText(this, "Upload profile pic", Toast.LENGTH_SHORT).show();
    }


    private void switchProfileUI(boolean mainPageVisible) {
        if (mainPageVisible) {
            findViewById(R.id.profileDisplayMainPage).setVisibility(View.VISIBLE);
            findViewById(R.id.profileContentUploadPage).setVisibility(View.GONE);
        } else {
            // update imageView profileContentUploadPage
            ImageView imgView = findViewById(R.id.profileContentUploadView);
//            imgView.setImageBitmap(utils.cropProfileBitmap(
//                    profileBitmapCameraBuffer, false));
            imgView.setImageBitmap(profileBitmapCameraBuffer);
            findViewById(R.id.profileDisplayMainPage).setVisibility(View.GONE);
            findViewById(R.id.profileContentUploadPage).setVisibility(View.VISIBLE);
            findViewById(R.id.profileContentUploadButtonConfirmed).setVisibility(View.VISIBLE);
            findViewById(R.id.profileContentUploadButtonDiscard).setVisibility(View.VISIBLE);
            findViewById(R.id.profileContentUploadLoading).setVisibility(View.GONE);
        }
    }

    private void profileContentUploadToStorage() {
        StorageReference regStorageRef = mStorage.getReference();
        String key = getString(R.string.cloud_storage_content_img) +
                utils.processEmailString(profileUserEmail) +"/" +
                utils.getCurrentTimestampString();
        StorageReference regProfileContentImgRef = regStorageRef.child(key);
        UploadTask uploadTask = regProfileContentImgRef.putBytes(
                utils.compressBitmapToByteArray(profileBitmapCameraBuffer));
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.w(LOG_TAG, "Failed to upload content image to cloud storage",
                        exception);
                findViewById(R.id.profileContentUploadButtonConfirmed).setVisibility(View.VISIBLE);
                findViewById(R.id.profileContentUploadButtonDiscard).setVisibility(View.VISIBLE);
                findViewById(R.id.profileContentUploadLoading).setVisibility(View.GONE);
                Toast.makeText(ProfilePage.this,
                        "Storage: " + utils.fireStoreExceptionCode(exception),
                        Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(LOG_TAG, "Cloud storage upload content image succeed.");
                profileContentImgFrag.setData(
                        utils.compressBitmapToByteArray(profileBitmapCameraBuffer));
                profileBitmapListContentImg.addFirst(
                        utils.cropProfileBitmap(profileBitmapCameraBuffer, true));
                mRecyclerView.getAdapter().notifyItemInserted(0);
                mRecyclerView.smoothScrollToPosition(0);
                switchProfileUI(true);
            }
        });
    }

    public void profileContentUploadDiscard(View view) {
        // update UI element
        switchProfileUI(true);
    }

    public void profileContentUploadConfirmed(View view) {
        // set loading icon to be visible
        findViewById(R.id.profileContentUploadButtonConfirmed).setVisibility(View.GONE);
        findViewById(R.id.profileContentUploadButtonDiscard).setVisibility(View.GONE);
        findViewById(R.id.profileContentUploadLoading).setVisibility(View.VISIBLE);

        // upload data to storage
        profileContentUploadToStorage();
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == profileRequestCodeContentImgCam && data != null) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            if (bitmap != null) {
                profileBitmapCameraBuffer = bitmap;
                switchProfileUI(false);
            }
        } else {
            Log.w(LOG_TAG, "take content img on activity failed.");
        }

    }


}