package com.example.instgram;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;

public class Comment extends AppCompatActivity {

    // Log
    private static final String LOG_TAG = Comment.class.getSimpleName();
    // Firebase Auth
    private FirebaseAuth mAuth;
    // TextView
    private TextView commentTextViewCaption = null;
    // Utils
    private Utils utils = null;
    // Ref String
    private String commentRefString;
    // CommentItem List
    private LinkedList<CommentItems> commentRecyclerList = null;
    // RecyclerView
    private RecyclerView commentRecyclerView;
    private CommentAdapter commentAdapter;
    // Current User Bitmap and UserName
    private byte[] commentCurrentUserPhoto;
    private String commentCurrentUserName;
    private String commentCurrentUserEmail;
    // 1M
    private static final long ONE_MEGABYTE = 1024 * 1024;
    // Comment Recyclerview Data Structure for Chronological Order
    private HashMap<Long, CommentFireStoreFields> commentHashMapFields;
    private HashMap<Long, byte[]> commentHashMapBitmap;
    private ArrayList<Long> commentArrayListTimestamp;
    // AsyncHandler for comment RecyclerView
    private AsyncCallHandler commentAsyncHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Comment");
        setContentView(R.layout.activity_comment);
        mAuth = FirebaseAuth.getInstance();
        commentCurrentUserEmail = Objects.requireNonNull(mAuth.getCurrentUser()).getEmail();
        commentTextViewCaption = findViewById(R.id.commentCaption);
        commentRecyclerList = new LinkedList<CommentItems>();
        utils = new Utils();
        commentRecyclerView = findViewById(R.id.commentRecycler);
        commentAdapter = new CommentAdapter(this, commentRecyclerList);
        commentRecyclerView.setAdapter(commentAdapter);
        commentRecyclerView.setLayoutManager(new LinearLayoutManager(Comment.this));
        commentHashMapFields = new HashMap<Long, CommentFireStoreFields>();
        commentHashMapBitmap = new HashMap<Long, byte[]>();
        commentArrayListTimestamp = new ArrayList<Long>();

        // Intent from profile page: Image bytes AND ref string
        byte [] contentImgByte = getIntent().getByteArrayExtra(
                "extraContentImageBitmapByte");
        Bitmap commentBitmap = BitmapFactory.decodeByteArray(
                contentImgByte, 0, contentImgByte.length);
        ImageView commentImageView = findViewById(R.id.commentImageView);
        commentImageView.setImageBitmap(
                utils.cropProfileBitmap(commentBitmap, false));
        commentRefString = getIntent().getStringExtra("extraStorageRef");
        loadCurrentUserInfo();
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadCaption();
        loadAllComments();
    }

    private void loadCurrentUserInfo() {
        // Get current user name
        String email = utils.processEmailString(commentCurrentUserEmail);
        String doc = getString(R.string.firestore_category_users) + email;
        DocumentReference docRef = FirebaseFirestore.getInstance().document(doc);
        docRef.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot docSnapShot, FirebaseFirestoreException error) {
                if (docSnapShot.exists()) {
                    commentCurrentUserName = docSnapShot.getString(
                            getString(R.string.firestore_category_users_doc_username)
                    );
                } else if (error != null) {
                    Log.w(LOG_TAG, "get current user data from firestore failed.", error);
                }
            }
        });
        // Get current user profile photo
        String storage = getString(R.string.cloud_storage_profile_pic) + email;
        StorageReference profileStorageRef = FirebaseStorage.getInstance().getReference();
        StorageReference profilePicRef = profileStorageRef.child(storage);
        profilePicRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                commentCurrentUserPhoto = bytes;
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.w(LOG_TAG, "get current user pic from cloud storage failed.", exception);
            }
        });
    }

    private void loadAllComments() {
        FirebaseFirestore.getInstance()
                .collection(getString(R.string.firestore_category_comments))
                .whereEqualTo("photoRef", commentRefString)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int size  = task.getResult().size();
                            commentAsyncHandler = new AsyncCallHandler(size);
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(LOG_TAG,
                                        document.getId() + " => " + document.getData());
                                loadSingleCommentItem(
                                        new CommentFireStoreFields(document.getData()));
                            }
                        } else {
                            Log.d(LOG_TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });

    }

    private void loadSingleCommentItem(CommentFireStoreFields field) {
        String commentOwner = field.getCommentOwner();
        Long commentTimestamp = Long.parseLong(field.getCommentTimestamp());
        String storage = getString(R.string.cloud_storage_profile_pic) + commentOwner;
        StorageReference profileStorageRef = FirebaseStorage.getInstance().getReference();
        StorageReference profilePicRef = profileStorageRef.child(storage);
        profilePicRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                commentArrayListTimestamp.add(commentTimestamp);
                commentHashMapFields.put(commentTimestamp, field);
                commentHashMapBitmap.put(commentTimestamp, bytes);
                commentAsyncHandler.addSuccessfulTask();
                if (commentAsyncHandler.waitForAllComplete()) {
                    insertCommentToList();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.w(LOG_TAG, "get current user pic from cloud storage failed.", exception);
            }
        });
    }

    private void insertCommentToList() {
        Collections.sort(commentArrayListTimestamp, Collections.reverseOrder());
        for (Long timestamp : commentArrayListTimestamp) {
            byte [] bytes = commentHashMapBitmap.get(timestamp);
            CommentFireStoreFields field = commentHashMapFields.get(timestamp);
            String ownerName = field.getCommentOwnerName();
            String words = field.getCommentWords();
            commentRecyclerList.addLast(new CommentItems(ownerName, words, bytes));
            commentAdapter.notifyItemInserted(0);
        }
        commentRecyclerView.smoothScrollToPosition(0);
    }

    private void loadCaption() {
        //TODO
        commentTextViewCaption.setText("CAPTION TODO");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.comment_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_transition_end:
                Log.d(LOG_TAG, "comment page transition end button clicked");
                ActivityCompat.finishAfterTransition(Comment.this);
                break;
            case R.id.action_delete_post:
                Log.d(LOG_TAG, "comment page delete post button clicked");
                startActivity(new Intent(Comment.this, ProfilePage.class));
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void uploadCommentText(View view) {
        EditText commentEditText = findViewById(R.id.commentText);
        String words = commentEditText.getText().toString();
        String owner = utils.processEmailString(commentCurrentUserEmail);
        String timestamp = utils.getCurrentTimestampString();
        CollectionReference commentCollection = FirebaseFirestore.getInstance()
                .collection(getString(R.string.firestore_category_comments));
        commentCollection.document()
                .set(
                        new CommentFireStoreFields(
                                commentRefString,
                                owner,
                                commentCurrentUserName,
                                words,
                                timestamp))
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(LOG_TAG, "add comment to firestore successfully");
                            commentRecyclerList.addFirst(
                                    new CommentItems(commentCurrentUserName,
                                            words, commentCurrentUserPhoto));
                            commentAdapter.notifyItemInserted(0);
                            commentRecyclerView.smoothScrollToPosition(0);
                        } else {
                            Log.w(LOG_TAG, "add comment to firestore failed",
                                    task.getException());
                        }
                    }
                });
    }






//    public void contentImageFullScreenExit(View view) {
//        ActivityCompat.finishAfterTransition(Comment.this);
//    }
}