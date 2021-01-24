package com.example.instgram;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class ContentImageFullScreen extends AppCompatActivity {

    private static final String LOG_TAG = ContentImageFullScreen.class.getSimpleName();
    // ImageView
    private ImageView contentImgFullImageView = null;
    // Utils
    private Utils utils = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content_image_full_screen);
        contentImgFullImageView = findViewById(R.id.contentImageFullScreenView);
        utils = new Utils();
        byte [] contentImgByte = getIntent().getByteArrayExtra(
                "extraContentImageBitmapByte");
        Bitmap contentImgBitmap = BitmapFactory.decodeByteArray(
                contentImgByte, 0, contentImgByte.length);
        contentImgFullImageView.setImageBitmap(
                utils.cropProfileBitmap(contentImgBitmap, false));
    }

    public void contentImageFullScreenExit(View view) {
        ActivityCompat.finishAfterTransition(ContentImageFullScreen.this);
    }
}