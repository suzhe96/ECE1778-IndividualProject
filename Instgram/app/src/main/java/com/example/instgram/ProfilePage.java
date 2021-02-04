package com.example.instgram;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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

    // AsyncHandler On Start
    private AsyncCallHandler profileAsyncHdlOnLoadContentImg;
    // AsyncHandler for Content Uploading
    private AsyncCallHandler profileAsyncHdlContentUpload;

    // 1M
    private static final long ONE_MEGABYTE = 1024 * 1024;

    // Intent Request Code
    private static final int profileRequestCodeContentImgCam = 100;

    // RecycleView for ContentImg
    private LinkedList<Bitmap> profileBitmapListContentImg = null;
    private LinkedList<Bitmap> profileBitmapListContentImgGlobal = null;
    private RecyclerView mRecyclerView;
    private ContentImgAdapter mAdapter;
    private ContentImgAdapter mAdapterGlobal;

    // Global/Grid view switch guard
    private Boolean profileViewSwitchIsGrid = null;

    // ContentImg Data Structure for Chronological Order
    private HashMap<Long, byte[]> profileHashMapContentImg = null;
    private ArrayList<Long> profileArrayListContentImgTimestamp = null;

    // ContentImgGlobal position index mapping of /userEmail/timeStamp
    private ArrayList<String> profileArrayListContentImgGlobalPrefix = null;
    private Boolean profileGlobalViewInitialized = null;



    private void setVisibilityForDone(Boolean isDone) {
        if (isDone) {
            profileTextViewShortBio.setVisibility(View.VISIBLE);
            profileTextViewDisplayName.setVisibility(View.VISIBLE);
            profileImageViewProfilePic.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.VISIBLE);
            profileProgressBarLoading.setVisibility(View.GONE);
        } else {
            profileTextViewShortBio.setVisibility(View.INVISIBLE);
            profileTextViewDisplayName.setVisibility(View.INVISIBLE);
            profileImageViewProfilePic.setVisibility(View.INVISIBLE);
            mRecyclerView.setVisibility(View.INVISIBLE);
            profileProgressBarLoading.setVisibility(View.VISIBLE);
        }
    }

    private void insertContentImgToList() {
        Collections.sort(profileArrayListContentImgTimestamp, Collections.reverseOrder());
        for (Long timestamp : profileArrayListContentImgTimestamp) {
            byte [] bytes = profileHashMapContentImg.get(timestamp);
            profileContentImgFrag.setData(bytes);
            profileContentImgFrag.setTimestampData(timestamp);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            profileBitmapListContentImg.addLast(
                        utils.cropProfileBitmap(bitmap, true));
            mRecyclerView.getAdapter().notifyItemInserted(0);
        }
        mRecyclerView.smoothScrollToPosition(0);
        setVisibilityForDone(true);
    }

    private void loadContentImgGlobalToList(StorageReference refGlobal) {
        refGlobal.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                profileBitmapListContentImgGlobal.addLast(
                        utils.cropProfileBitmap(bitmap, true));
                int globalListSize = profileArrayListContentImgGlobalPrefix.size();
                mRecyclerView.getAdapter().notifyItemInserted(globalListSize);
                profileArrayListContentImgGlobalPrefix.add(refGlobal.getPath());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.w(LOG_TAG, "get content image global from cloud storage failed.",
                        exception);
            }
        });
    }

    private void switchToGlobalView() {
        // switch to global view
        mRecyclerView.setAdapter(mAdapterGlobal);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(ProfilePage.this));
        mAdapterGlobal.notifyDataSetChanged();

        // to see if need fetch resource from the storage
        if (profileGlobalViewInitialized) {
            return;
        }

        // get all contentImg ref if the first time
        String contentImgKey = getString(R.string.cloud_storage_content_img);
        StorageReference listGlobalRef = mStorage.getReference().child(contentImgKey);
        listGlobalRef.listAll()
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {
                        profileGlobalViewInitialized = true;
                        for (StorageReference prefix : listResult.getPrefixes()) {
                            prefix.listAll()
                                    .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                                        @Override
                                        public void onSuccess(ListResult listResult) {
                                            for (StorageReference item : listResult.getItems()) {
                                                Log.w(LOG_TAG, item.getPath());
                                                loadContentImgGlobalToList(item);
                                            }
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.w(LOG_TAG,
                                                    "get listRef contentImageGlobal error",
                                                    e);
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(LOG_TAG, "get listRef contentImageGlobal prefix error", e);
                    }
                });
    }

    private void loadContentImgFromStorage(StorageReference ref, Long timeStamp) {
        ref.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                profileArrayListContentImgTimestamp.add(timeStamp);
                profileHashMapContentImg.put(timeStamp, bytes);
                profileAsyncHdlOnLoadContentImg.addSuccessfulTask();
                if (profileAsyncHdlOnLoadContentImg.waitForAllComplete()) {
                    insertContentImgToList();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.w(LOG_TAG, "get content image from cloud storage failed.", exception);
            }
        });
    }

    private void contentImgOnClickEvent(View view, int position) {
            // current context
            Context context = view.getContext();
            // click for full screen
            Intent contentImageFullScreenIntent =
                new Intent(context, Comment.class);
            if (profileViewSwitchIsGrid) {
                contentImageFullScreenIntent.putExtra("extraContentImageBitmapByte",
                        utils.compressBitmapToByteArray(profileBitmapListContentImg.get(position)));
                String ref = "/" + getString(R.string.cloud_storage_content_img) +
                        utils.processEmailString(profileUserEmail) + "/" +
                        Long.toString(profileArrayListContentImgTimestamp.get(position));
                contentImageFullScreenIntent.putExtra("extraStorageRef", ref);
            } else {
                contentImageFullScreenIntent.putExtra("extraContentImageBitmapByte",
                        utils.compressBitmapToByteArray(
                                profileBitmapListContentImgGlobal.get(position)));
                contentImageFullScreenIntent.putExtra("extraStorageRef",
                        profileArrayListContentImgGlobalPrefix.get(position));
            }
            context.startActivity(contentImageFullScreenIntent);
//            context.startActivity(contentImageFullScreenIntent,
//                    ActivityOptions.makeSceneTransitionAnimation(
//                            (Activity) view.getContext(),
//                            view, "sharedContentImageEnlarged").toBundle());
            return;
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
        profileBitmapListContentImgGlobal = new LinkedList<>();

        // Initialize ContentImg Data Structure
        profileHashMapContentImg = new HashMap<Long, byte[]>();
        profileArrayListContentImgTimestamp = new ArrayList<Long>();

        // Initialize ContentImgGlobal Data Structure
        profileArrayListContentImgGlobalPrefix = new ArrayList<String>();
        profileGlobalViewInitialized = false;

        // Create recycler view.
        mRecyclerView = findViewById(R.id.recyclerview);
        // Create an adapter and supply the data to be displayed.
        mAdapter = new ContentImgAdapter(this, profileBitmapListContentImg);
        mAdapterGlobal = new ContentImgAdapter(this, profileBitmapListContentImgGlobal);
        mAdapter.setOnItemClickListener(new ContentImgAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                contentImgOnClickEvent(view, position);
            }
        });
        mAdapterGlobal.setOnItemClickListener(new ContentImgAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                contentImgOnClickEvent(view, position);
            }
        });
        // Connect the adapter with the recycler view.
        mRecyclerView.setAdapter(mAdapter);
        // Give the recycler view a default layout manager.
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        profileViewSwitchIsGrid = true;

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
        ArrayList<Long> timestampArrList = this.profileContentImgFrag.getTimestampData();
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
        Collections.sort(timestampArrList, Collections.reverseOrder());
        for (int i = 0; i < timestampArrList.size(); ++i) {
            profileArrayListContentImgTimestamp.add(timestampArrList.get(i));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.profile_menu, menu);
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
            case R.id.action_view_switch:
                Log.d(LOG_TAG, "View switch button being clicked");
                profileViewSwitchIsGrid = utils.toggleBoolean(profileViewSwitchIsGrid);
                if (!profileViewSwitchIsGrid) {
                    switchToGlobalView();
                } else {
                    mRecyclerView.setAdapter(mAdapter);
                    mRecyclerView.setLayoutManager(
                            new GridLayoutManager(ProfilePage.this, 3));
                    mAdapter.notifyDataSetChanged();
                }
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
                        Log.d(LOG_TAG, "getting contentImg list successfully");
                        profileAsyncHdlOnLoadContentImg =
                                new AsyncCallHandler(listResult.getItems().size());
                        for (StorageReference item : listResult.getItems()) {
                            Log.w(LOG_TAG, item.getPath());
                            loadContentImgFromStorage(item,
                                    utils.getTimeStampFromStorageRef(item.getPath()));
                        }
                        if (profileAsyncHdlOnLoadContentImg.waitForAllComplete()) {
                            setVisibilityForDone(true);
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
            findViewById(R.id.profileDisplayMainPage).setVisibility (View.GONE);
            findViewById(R.id.profileContentUploadPage).setVisibility(View.VISIBLE);
            findViewById(R.id.profileContentUploadButtonConfirmed).setVisibility(View.VISIBLE);
            findViewById(R.id.profileContentUploadButtonDiscard).setVisibility(View.VISIBLE);
            findViewById(R.id.profileContentUploadCaption).setVisibility(View.VISIBLE);
            findViewById(R.id.profileContentUploadHashTagSwitch).setVisibility(View.VISIBLE);
            findViewById(R.id.profileContentUploadLoading).setVisibility(View.GONE);
        }
    }

    private void profileContentUpload(String caption) {
        // Tasks: 1. caption to FireStore; 2. contentImg to Storage
        profileAsyncHdlContentUpload = new AsyncCallHandler(2);

        // Get current timestamp
        String timestampString = utils.getCurrentTimestampString();


        // upload caption + hashTag to FireStore
        String doc = getString(R.string.firestore_category_caption) +
                utils.processEmailString(profileUserEmail) +
                getString(R.string.firestore_caption_delimiter) + timestampString;
        DocumentReference docRef = FirebaseFirestore.getInstance().document(doc);

        Map<String, Object> userData = new HashMap<String, Object>();
        userData.put(getString(R.string.firestore_category_caption_doc_text), caption);
        docRef.set(userData).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(LOG_TAG, "succeeded to sync caption to firestore.");
                    profileAsyncHdlContentUpload.addSuccessfulTask();
                    if (profileAsyncHdlContentUpload.waitForAllComplete()) {
                        switchProfileUI(true);
                    }
                } else {
                    Log.w(LOG_TAG, "failed to sync caption to firestore",
                            task.getException());
                    findViewById(R.id.profileContentUploadButtonConfirmed)
                            .setVisibility(View.VISIBLE);
                    findViewById(R.id.profileContentUploadButtonDiscard)
                            .setVisibility(View.VISIBLE);
                    findViewById(R.id.profileContentUploadCaption)
                            .setVisibility(View.VISIBLE);
                    findViewById(R.id.profileContentUploadHashTagSwitch)
                            .setVisibility(View.VISIBLE);
                    findViewById(R.id.profileContentUploadLoading)
                            .setVisibility(View.GONE);
                    Toast.makeText(ProfilePage.this,
                            "FireStore Caption upload failed",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        // upload contentImg to storage
        StorageReference regStorageRef = mStorage.getReference();
        String key = getString(R.string.cloud_storage_content_img) +
                utils.processEmailString(profileUserEmail) +"/" + timestampString;
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
                findViewById(R.id.profileContentUploadCaption).setVisibility(View.VISIBLE);
                findViewById(R.id.profileContentUploadHashTagSwitch).setVisibility(View.VISIBLE);
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
                profileContentImgFrag.setTimestampData(Long.parseLong(timestampString));
                Bitmap bitmap = utils.cropProfileBitmap(profileBitmapCameraBuffer, true);
                profileBitmapListContentImg.addFirst(bitmap);
                profileArrayListContentImgTimestamp.add(0, Long.parseLong(timestampString));
                if (profileGlobalViewInitialized) {
                    profileBitmapListContentImgGlobal.addLast(bitmap);
                    profileArrayListContentImgGlobalPrefix.add("/" + key);
                }



                if (profileViewSwitchIsGrid) {
                    mRecyclerView.getAdapter().notifyItemInserted(0);
                    mRecyclerView.getAdapter().notifyDataSetChanged();
                    mRecyclerView.smoothScrollToPosition(0);
                } else {
                    int size = profileBitmapListContentImgGlobal.size();
                    mRecyclerView.getAdapter().notifyItemInserted(size);
                    mRecyclerView.getAdapter().notifyDataSetChanged();
                    mRecyclerView.smoothScrollToPosition(size);
                }
                profileAsyncHdlContentUpload.addSuccessfulTask();
                if (profileAsyncHdlContentUpload.waitForAllComplete()) {
                    switchProfileUI(true);
                }

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
        findViewById(R.id.profileContentUploadCaption).setVisibility(View.GONE);
        findViewById(R.id.profileContentUploadHashTagSwitch).setVisibility(View.GONE);
        findViewById(R.id.profileContentUploadLoading).setVisibility(View.VISIBLE);
        // prepare caption and hashTag
        profileContentUploadGetCaption();
    }

    private void profileContentUploadGetCaption() {
        EditText captionEditText = findViewById(R.id.profileContentUploadCaption);
        String captionText = captionEditText.getText().toString();
        Switch autoHashTagSwitch = findViewById(R.id.profileContentUploadHashTagSwitch);
        boolean enable = autoHashTagSwitch.isChecked();
        if (!enable) {
            profileContentUpload(captionText);
        } else {
            InputImage image = InputImage.fromBitmap(profileBitmapCameraBuffer, 0);
             ImageLabelerOptions options =
                new ImageLabelerOptions.Builder()
                    .setConfidenceThreshold(0.7f)
                        .build();
            ImageLabeler labeler = ImageLabeling.getClient(options);
            labeler.process(image)
                    .addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onSuccess(List<ImageLabel> labels) {
                            Log.d(LOG_TAG, "Firebase ML kit getting successfully");
                            ArrayList<String> labelArr = new ArrayList<String>();
                            for (ImageLabel label : labels) {
                                labelArr.add(label.getText());
                            }
                            String [] simpleArr = new String[ labelArr.size() ];
                            labelArr.toArray(simpleArr);
                            String hashTags = String.join(" #", simpleArr);
                            profileContentUpload(captionText + "#" + hashTags);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(LOG_TAG, "Google ML kit exception.", e);
                        }
                    });
        }

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